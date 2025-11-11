package org.texttosql.server;

import net.jcip.annotations.NotThreadSafe;

import java.sql.*;

/**
 * Работа с PostgreSQL: авторизация, выполнение запросов
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
     * @param host         Хост базы данных
     * @param username     Имя пользователя
     * @param password     Пароль пользователя
     * @param databaseName Название базы данных
     */
    public ConnectWithDb(String host, String username, String password, String databaseName) {
        if (host.isEmpty() || username.isEmpty() || password.isEmpty() || databaseName.isEmpty()) {
            throw new RuntimeException("Значение параметра пусто");
        }

        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    private String getUrl() {
        return "jdbc:postgresql://" + this.host + "/" + this.databaseName + "?charSet=UTF8";
    }

    /**
     * Проверяет логин и пароль через таблицу users
     *
     * @return информация о пользователе или null
     * @throws RuntimeException если произошла ошибка при выполнении запроса
     */
    public DbUserInfo authenticateUser() {
        try {
            // Читаем роль пользователя
            String roleSql = "SELECT role FROM public.users WHERE username = ?";
            String role;

            try (Connection conn = DriverManager.getConnection(this.getUrl(), "buyer", "buyer_pass");
                 PreparedStatement stmt = conn.prepareStatement(roleSql)) {

                stmt.setString(1, this.username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        role = rs.getString("role");
                    } else {
                        return null;
                    }
                }
            }
            // После получения role из БД
            DbUserInfo userInfo = new DbUserInfo(this.username, role);

            // Проверяем пароль
            String passSql = """
                    SELECT gost_kuz_decrypt(password_hash, get_data_key('users', 'password_hash', ?)) = ?
                    FROM public.users WHERE username = ?
                    """;

            try (Connection conn = DriverManager.getConnection(this.getUrl(),
                    userInfo.dbUsername(), userInfo.dbPassword());
                 PreparedStatement stmt = conn.prepareStatement(passSql)) {

                stmt.setString(1, role + "_role");
                stmt.setString(2, this.password);
                stmt.setString(3, this.username);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getBoolean(1)) {
                        return userInfo; // Возвращаем готовый объект
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка авторизации: " + e.getMessage());
        }
    }

    /**
     * Выполняет SQL-запрос и возвращает результаты в текстовом формате
     *
     * @param sql SQL-запрос для выполнения
     * @return результаты выполнения запроса в виде строки
     * @throws RuntimeException если произошла ошибка при выполнении запроса
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
     * Проверяет строку на корректность UTF-8 и заменяет некорректные символы на "ENCODING"
     *
     * @param text строка
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
    public record DbUserInfo(String clientUsername, String role) {
        public String dbUsername() {
            return switch (this.role) {
                case "admin" -> "admin";
                case "seller" -> "seller";
                case "buyer" -> "buyer";
                default -> throw new RuntimeException("Неизвестная роль: " + this.role);
            };
        }

        public String dbPassword() {
            return switch (this.role) {
                case "admin" -> "admin_pass";
                case "seller" -> "seller_pass";
                case "buyer" -> "buyer_pass";
                default -> throw new RuntimeException("Неизвестная роль: " + this.role);
            };
        }
    }
}