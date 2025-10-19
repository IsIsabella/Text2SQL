package org.texttosql;

import com.google.gson.Gson;

import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для парсинга SQL-запросов с учетом шифрования/расшифровки колонок
 * на основе ролей пользователя. Поддерживает нормализацию регистров для
 * таблиц и колонок, используя данные из конфигурационного файла JSON.
 */
public class ParseQuery {
    private String sql;                 // SQL-запрос
    private String host;                // Хост базы данных
    private String username;            // Имя пользователя
    private String password;            // Пароль
    private String databaseName;        // Название базы данных
    private Map<String, RoleConfiguration> roleConfigurations; // Конфигурации ролей

    /**
     * Конструктор класса ParseQuery.
     *
     * @param sql            SQL-запрос для парсинга
     * @param host           Хост базы данных
     * @param username       Имя пользователя базы данных
     * @param password       Пароль пользователя
     * @param databaseName   Название базы данных
     * @param configFilePath Путь к файлу конфигурации ролей (JSON)
     */
    public ParseQuery(String sql, String host, String username, String password, String databaseName, String configFilePath) {
        this.sql = sql.toLowerCase(); // Нормализация SQL-запроса к нижнему регистру
        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
        this.roleConfigurations = loadRoleConfigurations(configFilePath);
    }

    /**
     * Загружает конфигурации ролей из JSON-файла.
     *
     * @param configFilePath Путь к файлу конфигурации
     * @return Map с конфигурациями ролей, где ключ - имя роли в нижнем регистре
     * @throws RuntimeException Если произошла ошибка при загрузке конфигурации
     */
    private Map<String, RoleConfiguration> loadRoleConfigurations(String configFilePath) {
        try (FileReader reader = new FileReader(configFilePath)) {
            Gson gson = new Gson();
            Configuration config = gson.fromJson(reader, Configuration.class);
            Map<String, RoleConfiguration> map = new HashMap<>();
            for (RoleConfiguration role : config.roles) {
                // Нормализация имени роли и создания новой конфигурации с нормализованными данными
                String normalizedRoleName = role.roleName.toLowerCase();
                RoleConfiguration normalizedRole = new RoleConfiguration();
                normalizedRole.roleName = normalizedRoleName;
                normalizedRole.encryptedColumns = role.encryptedColumns != null ?
                        Arrays.stream(role.encryptedColumns)
                                .map(String::toLowerCase)
                                .toArray(String[]::new) : null;
                normalizedRole.tableMappings = role.tableMappings != null ?
                        role.tableMappings.entrySet().stream()
                                .collect(HashMap::new,
                                        (m, e) -> m.put(e.getKey().toLowerCase(), e.getValue().toLowerCase()),
                                        HashMap::putAll) : null;
                map.put(normalizedRoleName, normalizedRole);
            }
            return map;
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка загрузки конфигурации: " + ex.getMessage(), ex);
        }
    }

    /**
     * Парсит SQL-запрос, заменяя зашифрованные колонки на вызовы функции расшифровки
     * в зависимости от роли пользователя.
     *
     * @return Обработанный SQL-запрос
     * @throws Exception Если произошла ошибка при подключении к базе данных
     */
    public String parseSql() throws Exception {
        ConnectWithDb connect = new ConnectWithDb(host, username, password, databaseName);
        for (Map.Entry<String, RoleConfiguration> entry : roleConfigurations.entrySet()) {
            if (connect.currentRole(entry.getKey())) {
                return replacingColumn(entry.getValue());
            }
        }
        return sql; // Возвращаем исходный SQL, если роль не найдена
    }

    /**
     * Заменяет зашифрованные колонки в SQL-запросе на вызовы функции gost_kuz_decrypt
     * с использованием функции get_data_key для получения ключа.
     *
     * @param roleConfig Конфигурация роли для текущего пользователя
     * @return Обработанный SQL-запрос с замененными колонками
     */
    private String replacingColumn(RoleConfiguration roleConfig) {
        if (roleConfig.encryptedColumns == null || roleConfig.encryptedColumns.length == 0) {
            return sql;
        }
        // Паттерн для поиска имен колонок как независимых идентификаторов, исключая аргументы функций
        String patternStr = "\\b(" + String.join("|", roleConfig.encryptedColumns) + ")\\b(?![^\\(]*\\))";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(sql);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String columnName = matcher.group();
            String tableName = getTableNameForColumn(columnName, roleConfig.tableMappings);
            String replacement = String.format("gost_kuz_decrypt(%s, (select get_data_key('%s', '%s', '%s')))",
                    columnName, tableName, columnName, roleConfig.roleName);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Получает имя таблицы, соответствующее заданной колонке, из маппинга.
     *
     * @param columnName    Имя колонки
     * @param tableMappings Маппинг колонок на таблицы
     * @return Имя таблицы
     * @throws IllegalArgumentException Если соответствие для колонки не найдено
     */
    private String getTableNameForColumn(String columnName, Map<String, String> tableMappings) {
        if (tableMappings == null || !tableMappings.containsKey(columnName)) {
            throw new IllegalArgumentException("Соответствие не найдено для столбца: " + columnName);
        }
        return tableMappings.get(columnName);
    }

    /**
     * Вспомогательный класс для десериализации конфигурации из JSON
     */
    static class Configuration {
        java.util.List<RoleConfiguration> roles;
    }

    /**
     * Вспомогательный класс для хранения конфигурации роли
     */
    static class RoleConfiguration {
        String roleName;                // Имя роли
        String[] encryptedColumns;      // Массив зашифрованных колонок
        Map<String, String> tableMappings; // Маппинг колонок на таблицы

        /**
         * Конструктор по умолчанию, требуемый для Gson
         */
        public RoleConfiguration() {
        }
    }
}