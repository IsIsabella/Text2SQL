package org.texttosql;

import java.sql.*;
import java.nio.charset.*;

public class ConnectWithDb {
    private final String host;
    private final String username;
    private final String password;
    private final String databaseName;

    public ConnectWithDb(String host, String username, String password, String databaseName) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
    }

    /**
     * Проверка, существует ли пользователь
     */
    public String checkUser() throws Exception {
        String connectionString = "jdbc:postgresql://" + host + "/postgres?charSet=UTF8";
        try (Connection conn = DriverManager.getConnection(connectionString, username, password)) {
            String sql = "SELECT 1 FROM pg_roles WHERE rolname = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
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
     * Выполняет SQL-запрос и возвращает результаты в виде текста.
     * Если встречаются битые или нечитаемые символы — подставляет ENCODING.
     */
    public String results(String sql) throws Exception {
        String connectionString = "jdbc:postgresql://" + host + "/" + databaseName + "?charSet=UTF8";
        StringBuilder result = new StringBuilder();

        try (Connection conn = DriverManager.getConnection(connectionString, username, password);
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
                            value = sanitizeString(obj.toString());
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
     * Проверка роли пользователя
     */
    public boolean currentRole(String role) throws Exception {
        String connectionString = "jdbc:postgresql://" + host + "/postgres?charSet=UTF8";
        try (Connection conn = DriverManager.getConnection(connectionString, username, password)) {
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
     * Проверяет строку и заменяет битые символы на "ENCODING".
     * Срабатывает, если:
     * - содержит символы '�'
     * - содержит непечатаемые ASCII
     * - не проходит проверку UTF-8
     */
    private String sanitizeString(String text) {
        if (text == null || text.isBlank()) return text;

        // Проверка на символы � (replacement char)
        if (text.contains("�")) return "ENCODING";

        // Проверка на непечатаемые символы
        for (char c : text.toCharArray()) {
            if (Character.isISOControl(c) && !Character.isWhitespace(c)) {
                return "ENCODING";
            }
        }

        // Проверка на корректную UTF-8 кодировку
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.decode(StandardCharsets.UTF_8.encode(text));
        } catch (CharacterCodingException e) {
            return "ENCODING";
        }

        // Если всё в порядке
        return text;
    }
}
