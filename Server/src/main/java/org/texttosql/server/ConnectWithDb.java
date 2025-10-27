package org.texttosql.server;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * Класс для взаимодействия с базой данных PostgreSQL.
 */
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
        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    /**
     * Проверяет существование пользователя в базе данных
     *
     * @return "1" если пользователь существует, иначе null
     * @throws Exception если произошла ошибка при подключении к базе данных
     */
    public String checkUser() throws Exception {
        String connectionString = "jdbc:postgresql://" + this.host + "/postgres?charSet=UTF8";
        try (Connection conn = DriverManager.getConnection(connectionString, this.username, this.password)) {
            String sql = "SELECT 1 FROM pg_roles WHERE rolname = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, this.username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException ex) {
            throw new Exception("Ошибка при соединении с базой данных: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Выполняет SQL-запрос и возвращает результаты в текстовом формате
     *
     * @param sql SQL-запрос для выполнения
     * @return результаты выполнения запроса в виде строки
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    public String results(String sql) throws Exception {
        String connectionString = "jdbc:postgresql://" + this.host + "/" + this.databaseName + "?charSet=UTF8";
        StringBuilder result = new StringBuilder();

        try (Connection conn = DriverManager.getConnection(connectionString, this.username, this.password);
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
                            value = this.sanitizeString(obj.toString());
                        }
                    } catch (Exception e) {
                        value = "ENCODING";
                    }

                    result.append(String.format("%-30s", value));
                }
                result.append("\n");
            }

        } catch (SQLException ex) {
            throw new Exception("Ошибка при соединении с базой данных: " + ex.getMessage());
        }

        return result.toString();
    }

    /**
     * Проверяет, имеет ли текущий пользователь указанную роль
     *
     * @param role имя роли для проверки
     * @return true, если пользователь имеет роль, иначе false
     * @throws Exception если произошла ошибка при подключении к базе данных
     */
    public boolean currentRole(String role) throws Exception {
        String connectionString = "jdbc:postgresql://" + this.host + "/postgres?charSet=UTF8";
        try (Connection conn = DriverManager.getConnection(connectionString, this.username, this.password)) {
            String sql = "SELECT pg_has_role(current_user, ?, 'MEMBER')";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, role);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        } catch (SQLException ex) {
            throw new Exception("Ошибка при соединении с базой данных: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Проверяет строку на корректность UTF-8 и заменяет некорректные символы на "ENCODING"
     *
     * @param text входная строка для проверки
     * @return проверенная строка или "ENCODING" при некорректных символах
     */
    private String sanitizeString(String text) {
        if (text == null || text.isBlank()) return text;

        // Проверка на символы �
        if (text.contains("�")) return "ENCODING";

        // Проверка на непечатаемые символы
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                return "ENCODING";
            }
        }

        // Проверка UTF-8
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.decode(StandardCharsets.UTF_8.encode(text));
        } catch (CharacterCodingException e) {
            return "ENCODING";
        }

        return text;
    }
}