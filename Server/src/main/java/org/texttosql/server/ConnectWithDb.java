package org.texttosql.server;

import net.jcip.annotations.NotThreadSafe;
import org.texttosql.AccessSubject;
import org.texttosql.EntityResolver;
import org.texttosql.MatchStatus;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Работа с PostgreSQL: авторизация, выполнение запросов.
 * Класс выполняет подключение к базе данных, аутентификацию пользователя с учётом
 * расшифровки зашифрованных данных и дополнительной проверки на совпадение с локальной
 * системой пользователей для ролей admin и seller. При несоответствии с локальным пользователем
 * роль понижается до buyer, а клиенту возвращается флаг и сообщение для отображения предупреждения
 */
@NotThreadSafe
public class ConnectWithDb {
    /**
     * Хост базы данных
     */
    private final String host;
    /**
     * Имя пользователя базы данных
     */
    private final String username;
    /**
     * Пароль пользователя
     */
    private final String password;
    /**
     * Название базы данных
     */
    private final String databaseName;

    /**
     * Конструктор для инициализации параметров подключения
     *
     * @param host         хост базы данных
     * @param username     имя пользователя (логин)
     * @param password     пароль пользователя
     * @param databaseName название базы данных
     * @throws RuntimeException если любой параметр пустой
     */
    public ConnectWithDb(String host, String username, String password, String databaseName) {
        if (host == null || host.isEmpty() ||
                username == null || username.isEmpty() ||
                password == null ||
                databaseName == null || databaseName.isEmpty()) {
            throw new RuntimeException("Все параметры подключения должны быть заполнены");
        }

        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    /**
     * Формирует JDBC URL подключения к PostgreSQL с указанием кодировки UTF-8
     *
     * @return строка URL подключения
     */
    private String getUrl() {
        return "jdbc:postgresql://" + this.host + "/" + this.databaseName + "?characterEncoding=UTF-8";
    }

    /**
     * Выполняет аутентификацию пользователя
     *
     * @return объект DbUserInfo с результатом аутентификации или null
     * @throws RuntimeException при ошибке подключения или выполнения SQL
     */
    public DbUserInfo authenticateUser() {
        try {
            // 1. Получаем роль пользователя
            String role = this.getUserRole();
            if (role == null) {
                return null; // Пользователь не найден
            }

            // 2. Проверяем пароль
            if (!this.isPasswordCorrect(role)) {
                return null; // Пароль неверный
            }

            // 3. Для admin/seller — проверяем локального пользователя
            if ("admin".equals(role) || "seller".equals(role)) {
                String effectiveRole = this.verifyWithLocalUsers(role);
                boolean downgraded = !effectiveRole.equals(role);

                String warning = null;
                if (downgraded) {
                    warning = String.format(
                            "Внимание! Вы вошли как %s, но ваши данные не совпадают с локальной " +
                                    "системой пользователей. Доступ предоставлен с правами покупателя (buyer).",
                            this.username
                    );
                }

                return new DbUserInfo(this.username, effectiveRole, downgraded, warning);
            }

            // 4. Для buyer — без проверки
            return new DbUserInfo(this.username, role, false, null);

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при аутентификации: " + e.getMessage(), e);
        }
    }

    /**
     * Получает роль пользователя из таблицы users
     *
     * @return роль или null
     * @throws SQLException при ошибке выполнения запроса
     */
    private String getUserRole() throws SQLException {
        String sql = "SELECT role FROM public.users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(this.getUrl(), "buyer", "buyer_pass");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, this.username);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("role") : null;
            }
        }
    }

    /**
     * Проверяет корректность пароля через расшифровку
     *
     * @param role роль пользователя (для выбора ключа)
     * @return true если пароль совпадает
     * @throws SQLException при ошибке выполнения запроса
     */
    private boolean isPasswordCorrect(String role) throws SQLException {
        String sql = """
                SELECT gost_kuz_decrypt(password_hash, get_data_key('users', 'password_hash', ?)) = ?
                FROM public.users WHERE username = ?
                """;

        // Создаём временный DbUserInfo только для получения dbUsername/dbPassword
        DbUserInfo tempInfo = new DbUserInfo(this.username, role);

        try (Connection conn = DriverManager.getConnection(
                this.getUrl(),
                tempInfo.dbUsername(),
                tempInfo.dbPassword());
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role + "_role");
            stmt.setString(2, this.password);
            stmt.setString(3, this.username);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }
        }
    }

    /**
     * Проверяет совпадение пользователя из БД с локальными пользователями
     *
     * @param dbRole роль из БД admin или seller
     * @return исходная роль при совпадении, иначе buyer
     */
    private String verifyWithLocalUsers(String dbRole) {
        try {
            AccessSubject dbSubject = this.getDecryptedUserData(dbRole);
            if (dbSubject == null) {
                return "buyer";
            }

            List<AccessSubject> localUsers = this.getLocalWindowsUsers();
            if (localUsers.isEmpty()) {
                return "buyer";
            }

            for (AccessSubject localUser : localUsers) {
                double probability = EntityResolver.calculateMatchProbability(dbSubject, localUser);
                if (EntityResolver.getMatchStatus(probability) == MatchStatus.MATCH) {
                    return dbRole;
                }
            }

            return "buyer";

        } catch (Exception e) {
            return "buyer";
        }
    }

    /**
     * Получает расшифрованные персональные данные пользователя
     *
     * @param role роль
     * @return объект AccessSubject или null
     * @throws RuntimeException при ошибке SQL
     */
    private AccessSubject getDecryptedUserData(String role) {
        String query = String.format("""
                SELECT 
                    gost_kuz_decrypt(last_name, get_data_key('users', 'last_name', '%s_role')) AS last_name,
                    gost_kuz_decrypt(first_name, get_data_key('users', 'first_name', '%s_role')) AS first_name,
                    gost_kuz_decrypt(middle_name, get_data_key('users', 'middle_name', '%s_role')) AS middle_name,
                    email,
                    phone,
                    position
                FROM public.users 
                WHERE username = ?
                """, role, role, role);

        try (Connection conn = DriverManager.getConnection(this.getUrl(), role, role + "_pass");
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, this.username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AccessSubject(
                            rs.getString("last_name"),
                            rs.getString("first_name"),
                            rs.getString("middle_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("position")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка расшифровки данных пользователя: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Получает список активных локальных пользователей Windows через PowerShell
     *
     * @return список AccessSubject
     */
    private List<AccessSubject> getLocalWindowsUsers() {
        List<AccessSubject> users = new ArrayList<>();
        try {
            String command = """
                    powershell.exe -NoProfile -Command "chcp 65001; \
                    Get-LocalUser | \
                    Select-Object Name,FullName,Description,Enabled | \
                    ConvertTo-Csv -NoTypeInformation | Out-String -Width 2000"
                    """;

            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                // Разделяем CSV: "Name","FullName","Description","Enabled"
                String[] parts = line.split("\",\"", -1);
                if (parts.length < 4) continue;

                // Убираем кавычки
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].replaceAll("^\"|\"$", "").trim();
                }

                String fullName = parts[1];
                String description = parts.length > 2 ? parts[2] : "";
                String enabledStr = parts[3];

                // Парсим активных
                if (!"True".equalsIgnoreCase(enabledStr)) {
                    continue; // Пропускаем неактивных
                }

                // Парсим ФИО
                String lastName = null, firstName = null, middleName = null;
                if (!fullName.isBlank()) {
                    String[] fio = fullName.split("\\s+");
                    if (fio.length > 0) lastName = fio[0];
                    if (fio.length > 1) firstName = fio[1];
                    if (fio.length > 2) middleName = fio[2];
                }

                //Парсим Description: email;phone;position
                String email = null, phone = null, position = null;
                if (!description.isBlank()) {
                    String[] desc = description.split(";", -1);
                    if (desc.length > 0) email = desc[0].trim();
                    if (desc.length > 1) phone = desc[1].trim();
                    if (desc.length > 2) position = desc[2].trim();
                }

                users.add(new AccessSubject(lastName, firstName, middleName, email, phone, position));
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Не удалось получить локальных пользователей: " + e.getMessage());
        }
        return users;
    }

    /**
     * Выполняет SQL-запрос и возвращает результаты в текстовом виде
     *
     * @param sql      SQL-запрос
     * @param username имя пользователя БД
     * @param password пароль БД
     * @return отформатированная строка с результатами
     * @throws RuntimeException при ошибке выполнения
     */
    public String results(String sql, String username, String password) {
        StringBuilder result = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(this.getUrl(), username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            // Заголовки
            for (int i = 1; i <= columnCount; i++) {
                result.append(String.format("%-30s", meta.getColumnName(i)));
            }
            result.append("\n");

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String value;
                    try {
                        Object obj = rs.getObject(i);
                        if (obj == null) {
                            value = "NULL";
                        } else if (obj instanceof byte[]) {
                            value = "BINARY_DATA";
                        } else {
                            value = this.sanitize(obj.toString());
                        }
                    } catch (Exception e) {
                        value = "ENCODING";
                    }

                    result.append(String.format("%-30s", value));
                }
                result.append("\n");
            }

        } catch (Exception ex) {
            throw new RuntimeException("Ошибка при соединении с базой данных: " + ex.getMessage());
        }

        return result.toString();
    }

    /**
     * Проверяет строку на корректность UTF-8
     *
     * @param text строка
     * @return исходная строка или "ENCODING"
     */
    private String sanitize(String text) {
        if (text == null || text.isBlank()) return text;
        if (text.contains("�")) return "ENCODING";
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                return "ENCODING";
            }
        }
        return text;
    }

    /**
     * Информация о пользователе
     */
    public record DbUserInfo(
            String clientUsername,
            String role,
            boolean downgraded,
            String warningMessage) {

        public DbUserInfo(String clientUsername, String role) {
            this(clientUsername, role, false, null);
        }

        /**
         * Возвращает имя пользователя базы данных в зависимости от роли
         *
         * @return имя пользователя БД
         */
        public String dbUsername() {
            return switch (this.role) {
                case "admin" -> "admin";
                case "seller" -> "seller";
                case "buyer" -> "buyer";
                default -> throw new IllegalArgumentException("Неизвестная роль: " + this.role);
            };
        }

        /**
         * Возвращает пароль пользователя базы данных в зависимости от роли
         *
         * @return пароль пользователя БД
         */
        public String dbPassword() {
            return switch (this.role) {
                case "admin" -> "admin_pass";
                case "seller" -> "seller_pass";
                case "buyer" -> "buyer_pass";
                default -> throw new IllegalArgumentException("Неизвестная роль: " + this.role);
            };
        }
    }
}