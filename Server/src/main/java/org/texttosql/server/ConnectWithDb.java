package org.texttosql.server;

import net.jcip.annotations.NotThreadSafe;
import org.texttosql.AccessSubject;
import org.texttosql.EntityResolver;
import org.texttosql.MatchStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Основной класс для взаимодействия с PostgreSQL.
 * Реализует:
 * авторизацию по текущему пользователю Windows (SSO);
 * сопоставление с зашифрованными данными из таблицы users;
 * выполнение SELECT-запросов от имени роли (admin/seller/buyer)
 */
@NotThreadSafe
public class ConnectWithDb {
    /**
     * Хост базы данных
     */
    private static final String HOST = "localhost";
    /**
     * Название базы данных
     */
    private static final String DATABASE = "bookstore_secure";

    /**
     * Формирует JDBC URL подключения к PostgreSQL с указанием кодировки UTF-8
     *
     * @return строка URL подключения
     */
    private String getUrl() {
        return "jdbc:postgresql://" + ConnectWithDb.HOST + "/" + ConnectWithDb.DATABASE + "?characterEncoding=UTF-8";
    }

    /**
     * Авторизация по текущему пользователю Windows.
     * Сравнивает данные текущего пользователя Windows (ФИО, email, телефон, должность)
     * с расшифрованными данными из таблицы users
     *
     * @return информация о пользователе и его роли
     * @throws Exception если произошла ошибка при аутентификации
     */
    public DbUserInfo authenticateByWindowsSSO() {
        WindowsUserProvider.CurrentUser winUser = WindowsUserProvider.getCurrentUser();
        if (winUser == null || !winUser.enabled()) {
            return new DbUserInfo("Неизвестный пользователь", "buyer", false,
                    "Не удалось получить данные текущего пользователя Windows");
        }

        AccessSubject localSubject = this.toAccessSubject(winUser);
        try {
            List<ConnectWithDb.UserCandidate> candidates = this.loadAllUsersFromDb();
            ConnectWithDb.UserCandidate bestMatch = this.findBestMatch(localSubject, candidates);

            String displayName = winUser.fullName() != null && !winUser.fullName().isBlank()
                    ? winUser.fullName().trim()
                    : winUser.login();

            // Найдено совпадение
            if (bestMatch != null) {
                return new DbUserInfo(
                        displayName,
                        bestMatch.role(),
                        false,
                        "Авторизация успешна: роль " + bestMatch.role());
            }
            // Нет совпадения — даём минимальные права
            return new DbUserInfo(
                    displayName,
                    "buyer",
                    false,
                    "Совпадение с записью в БД не найдено. Доступ предоставлен как покупателю"
            );

        } catch (Exception e) {
            throw new RuntimeException("Ошибка Windows SSO аутентификации: " + e.getMessage(), e);
        }
    }

    /**
     * Преобразует данные Windows-пользователя в объект AccessSubject
     *
     * @param winUser Windows-пользователь
     * @return объект AccessSubject
     * @throws Exception если Windows-пользователь null
     */
    private AccessSubject toAccessSubject(WindowsUserProvider.CurrentUser winUser) {
        if (winUser == null || !winUser.enabled()) {
            throw new RuntimeException("Пользователь не указан");
        }

        String[] fio = winUser.fullName() != null
                ? winUser.fullName().trim().split("\\s+")
                : new String[0];

        String lastName = fio.length > 0 ? fio[0] : null;
        String firstName = fio.length > 1 ? fio[1] : null;
        String middleName = fio.length > 2 ? fio[fio.length - 1] : null;

        String email = null, phone = null, position = null;
        if (winUser.description() != null && !winUser.description().isBlank()) {
            String[] parts = winUser.description().split(";", -1);
            email = parts.length > 0 ? parts[0].trim() : null;
            phone = parts.length > 1 ? parts[1].trim() : null;
            position = parts.length > 2 ? parts[2].trim() : null;
        }

        return new AccessSubject(lastName, firstName, middleName, email, phone, position);
    }

    /**
     * Кандидат из БД
     *
     * @param subject объект AccessSubject
     * @param role    роль
     */
    private record UserCandidate(AccessSubject subject, String role) {
    }

    /**
     * Загружает всех пользователей из таблицы users и расшифровывает их данные
     *
     * @return список пользователей из таблицы users с расшифрованными данными
     */
    private List<ConnectWithDb.UserCandidate> loadAllUsersFromDb() {
        List<ConnectWithDb.UserCandidate> candidates = new ArrayList<>();
        String[] roles = {"admin", "seller"};
        String sql = """
                SELECT
                    gost_kuz_decrypt(last_name,   get_data_key('users', 'last_name',   ?)) AS last_name,
                    gost_kuz_decrypt(first_name,  get_data_key('users', 'first_name',  ?)) AS first_name,
                    gost_kuz_decrypt(middle_name, get_data_key('users', 'middle_name', ?)) AS middle_name,
                    email, phone, position
                FROM public.users
                WHERE role = ?
                """;
        for (String role : roles) {
            String keySuffix = role + "_role";
            try (Connection conn = DriverManager.getConnection(getUrl(), role, role + "_pass");
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, keySuffix);
                ps.setString(2, keySuffix);
                ps.setString(3, keySuffix);
                ps.setString(4, role);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AccessSubject subject = new AccessSubject(
                                rs.getString("last_name"),
                                rs.getString("first_name"),
                                rs.getString("middle_name"),
                                rs.getString("email"),
                                rs.getString("phone"),
                                rs.getString("position")
                        );
                        candidates.add(new ConnectWithDb.UserCandidate(subject, role));
                    }
                }
            } catch (SQLException e) {
                // Не падаем — просто нет таких пользователей
                System.err.println("Не удалось загрузить пользователей с ролью " + role + ": " + e.getMessage());
            }
        }
        return candidates;
    }

    /**
     * Находит лучшее совпадение, используя EntityResolver
     *
     * @param local      пользователь, который совершает авторизацию
     * @param candidates список пользователей из таблицы
     * @return кандидат из таблицы, с которым данные сошлись, либо null
     * @throws Exception если входные параметры null
     */
    private ConnectWithDb.UserCandidate findBestMatch(AccessSubject local,
                                                      List<ConnectWithDb.UserCandidate> candidates) {
        if (local == null) {
            throw new RuntimeException("Текущий пользователь не найден");
        }
        if (candidates == null) {
            throw new RuntimeException("Список пользователей отсутствует");
        }

        ConnectWithDb.UserCandidate best = null;
        double bestProb = 0.0;

        for (ConnectWithDb.UserCandidate candidate : candidates) {
            double probability = EntityResolver.calculateMatchProbability(local, candidate.subject);
            if (EntityResolver.getMatchStatus(probability) == MatchStatus.MATCH && probability > bestProb) {
                bestProb = probability;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Выполняет SQL-запрос и возвращает результаты в текстовом виде
     *
     * @param sql SQL-запрос
     * @return отформатированная строка с результатами
     * @throws Exception при ошибке выполнения
     */
    public String results(String sql, String dbUsername, String dbPassword) {
        StringBuilder output = new StringBuilder();

        try (Connection conn = DriverManager.getConnection(getUrl(), dbUsername, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Заголовки
            for (int i = 1; i <= columnCount; i++) {
                output.append(String.format("%-30s", meta.getColumnName(i)));
            }
            output.append("\n");

            // Данные
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    Object obj = rs.getObject(i);
                    String value = obj == null ? "NULL"
                            : obj instanceof byte[] ? "BINARY_DATA"
                            : sanitize(obj.toString());
                    output.append(String.format("%-30s", value));
                }
                output.append("\n");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка выполнения запроса: " + e.getMessage(), e);
        }

        return output.toString();
    }

    /**
     * Проверяет строку на корректность UTF-8
     *
     * @param text строка
     * @return исходная строка или "ENCODING"
     */
    private String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        if (text.contains("�")) {
            return "ENCODING";
        }
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                return "ENCODING";
            }
        }
        return text;
    }

    /**
     * Информация о пользователе после аутентификации
     *
     * @param clientUsername ФИО
     * @param role           роль
     * @param downgraded     была ли понижена роль (для совместимости)
     * @param warningMessage сообщение для пользователя
     */
    public record DbUserInfo(
            String clientUsername,
            String role,
            boolean downgraded,
            String warningMessage) {

        /**
         * Информация о пользователе после аутентификации
         *
         * @param clientUsername ФИО
         * @param role           роль
         */
        public DbUserInfo(String clientUsername, String role) {
            this(clientUsername, role, false, null);
        }

        /**
         * Возвращает имя пользователя базы данных в зависимости от роли
         *
         * @return имя пользователя БД
         * @throws Exception при ошибке выполнения
         */
        public String dbUsername() {
            return switch (role) {
                case "admin" -> "admin";
                case "seller" -> "seller";
                case "buyer" -> "buyer";
                default -> throw new IllegalArgumentException("Неизвестная роль: " + role);
            };
        }

        /**
         * Возвращает пароль пользователя базы данных в зависимости от роли
         *
         * @return пароль пользователя БД
         * @throws Exception при ошибке выполнения
         */
        public String dbPassword() {
            return switch (role) {
                case "admin" -> "admin_pass";
                case "seller" -> "seller_pass";
                case "buyer" -> "buyer_pass";
                default -> throw new IllegalArgumentException("Неизвестная роль: " + role);
            };
        }
    }
}