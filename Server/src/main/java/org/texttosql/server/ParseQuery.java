package org.texttosql.server;

import com.google.gson.Gson;
import org.texttosql.common.Configuration;
import org.texttosql.common.RoleConfiguration;

import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для парсинга SQL-запросов с учетом шифрования/расшифровки колонок на основе ролей
 */
public class ParseQuery {
    /**
     * SQL-запрос для обработки, приведённый к нижнему регистру.
     */
    private String sql;

    /**
     * Хост базы данных (например, "localhost").
     */
    private String host;

    /**
     * Имя пользователя для подключения к базе данных.
     */
    private String username;

    /**
     * Пароль пользователя для подключения к базе данных.
     */
    private String password;

    /**
     * Имя базы данных (например, "bookstore").
     */
    private String databaseName;

    /**
     * Карта конфигураций ролей, где ключ — имя роли, значение — конфигурация роли.
     */
    private Map<String, RoleConfiguration> roleConfigurations;

    /**
     * Создаёт экземпляр {@code ParseQuery} с указанным SQL-запросом и параметрами подключения к базе данных.
     * <p>
     * Инициализирует SQL-запрос (приводит к нижнему регистру) и загружает конфигурацию ролей из JSON-файла.
     * </p>
     *
     * @param sql           SQL-запрос для обработки
     * @param host          хост базы данных
     * @param username      имя пользователя для подключения к базе
     * @param password      пароль пользователя
     * @param databaseName  имя базы данных
     * @param configFilePath путь к JSON-файлу конфигурации ролей
     * @throws RuntimeException если файл конфигурации не удалось загрузить или разобрать
     */
    public ParseQuery(String sql, String host, String username, String password,
                      String databaseName, String configFilePath) {
        this.sql = sql.toLowerCase();
        this.host = host;
        this.username = username;
        this.password = password;
        this.databaseName = databaseName;
        this.roleConfigurations = this.loadRoleConfigurations(configFilePath);
    }

    /**
     * Загружает конфигурацию ролей из JSON-файла
     *
     * @param configFilePath путь к JSON-файлу конфигурации
     * @return карта конфигураций ролей, где ключ — имя роли, значение — объект RoleConfiguration
     * @throws RuntimeException если файл не удалось прочитать или JSON некорректен
     */
    private Map<String, RoleConfiguration> loadRoleConfigurations(String configFilePath) {
        try (FileReader reader = new FileReader(configFilePath)) {
            Gson gson = new Gson();
            Configuration config = gson.fromJson(reader, Configuration.class);
            Map<String, RoleConfiguration> map = new HashMap<>();
            for (RoleConfiguration role : config.roles) {
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
                                        (m, e) ->
                                                m.put(e.getKey().toLowerCase(), e.getValue().toLowerCase()),
                                        HashMap::putAll) : null;
                map.put(normalizedRoleName, normalizedRole);
            }
            return map;
        } catch (Exception ex) {
            throw new RuntimeException("Ошибка загрузки конфигурации: " + ex.getMessage(), ex);
        }
    }

    /**
     * Обрабатывает SQL-запрос, заменяя зашифрованные колонки в зависимости от роли пользователя
     *
     * @return обработанный SQL-запрос с заменёнными зашифрованными колонками
     * @throws Exception если произошла ошибка при проверке роли или обработке SQL
     */
    public String parseSql() throws Exception {
        ConnectWithDb connect = new ConnectWithDb(this.host, this.username, this.password, this.databaseName);
        for (Map.Entry<String, RoleConfiguration> entry : roleConfigurations.entrySet()) {
            if (connect.currentRole(entry.getKey())) {
                return replacingColumn(entry.getValue());
            }
        }
        return this.sql;
    }

    /**
     * Заменяет зашифрованные колонки на вызовы функции gost_kuz_decrypt, исключая алиасы после AS
     *
     * @param roleConfig конфигурация роли пользователя
     * @return обработанный SQL-запрос
     */
    private String replacingColumn(RoleConfiguration roleConfig) {
        if (roleConfig.encryptedColumns == null || roleConfig.encryptedColumns.length == 0) {
            return this.sql;
        }

        Set<String> processedColumns = new HashSet<>();

        // Поиск выражений вида: alias.column [AS aliasName]
        // или column [AS aliasName]
        String columnPattern = "\\b(\\w+\\.)?(" + String.join("|", roleConfig.encryptedColumns) + ")\\b(\\s+AS\\s+(\\w+))?";
        Pattern pattern = Pattern.compile(columnPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(this.sql);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String alias = matcher.group(1);
            String columnName = matcher.group(2);
            String asClause = matcher.group(3);
            String aliasName = matcher.group(4);

            String fullColumn = alias != null ? alias + columnName : columnName;

            // Если уже обработали — пропускаем
            if (processedColumns.contains(fullColumn)) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }

            // Проверяем соответствие колонки таблице
            String tableName = getTableNameForColumn(columnName, roleConfig.tableMappings);

            // Формируем выражение
            String replacement = String.format(
                    "gost_kuz_decrypt(%s, (select get_data_key('%s', '%s', '%s')))",
                    fullColumn, tableName, columnName, roleConfig.roleName
            );

            // Если после AS идёт то же имя, что и колонка — сохраняем AS <columnName>
            if (aliasName != null && aliasName.equalsIgnoreCase(columnName)) {
                replacement = String.format("%s AS %s", replacement, aliasName);
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            processedColumns.add(fullColumn);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }
    /**
     * Получает имя таблицы, связанное с колонкой, из карты соответствий таблицы
     *
     * @param columnName   имя колонки
     * @param tableMappings карта соответствий колонок и таблиц
     * @return имя таблицы, связанное с колонкой
     * @throws IllegalArgumentException если соответствие для колонки не найдено
     */
    private String getTableNameForColumn(String columnName, Map<String, String> tableMappings) {
        if (tableMappings == null || !tableMappings.containsKey(columnName)) {
            throw new IllegalArgumentException("Соответствие не найдено для столбца: " + columnName);
        }
        return tableMappings.get(columnName);
    }
}