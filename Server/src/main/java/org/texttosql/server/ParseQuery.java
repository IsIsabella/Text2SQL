package org.texttosql.server;

import com.google.gson.Gson;
import net.jcip.annotations.NotThreadSafe;
import org.texttosql.common.Configuration;
import org.texttosql.common.RoleConfiguration;

import java.io.FileReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для парсинга SQL-запросов с учетом шифрования/расшифровки колонок на основе ролей
 */
@NotThreadSafe
public class ParseQuery {
    /**
     * SQL-запрос
     */
    private final String sql;
    /**
     * Имя пользователя базы данных
     */
    private final String username;
    /**
     * Конфигурация ролей
     */
    private final Map<String, RoleConfiguration> roleConfigurations;

    public ParseQuery(String sql, String username, String configPath) {
        this.sql = sql;
        this.username = username;
        this.roleConfigurations = this.loadRoleConfigurations(configPath);
    }

    /**
     * Загрузка конфигурации
     *
     * @param configPath конфигурационный файл
     * @return конфигурация ролей
     * @throws RuntimeException при ошибке загрузки конфигурации
     */
    private Map<String, RoleConfiguration> loadRoleConfigurations(String configPath) {
        try (FileReader reader = new FileReader(configPath)) {
            Gson gson = new Gson();
            Configuration config = gson.fromJson(reader, Configuration.class);
            Map<String, RoleConfiguration> map = new HashMap<>();

            for (RoleConfiguration role : config.roles) {
                String name = role.roleName.toLowerCase();
                RoleConfiguration norm = new RoleConfiguration();
                norm.roleName = name;
                norm.encryptedColumns = role.encryptedColumns != null
                        ? Arrays.stream(role.encryptedColumns).map(String::toLowerCase).toArray(String[]::new)
                        : new String[0];
                norm.tableMappings = role.tableMappings != null
                        ? new HashMap<>(role.tableMappings) : new HashMap<>();
                norm.tableMappings.replaceAll((k, v) -> v.toLowerCase());
                map.put(name, norm);
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить конфигурацию ролей: " + e.getMessage());
        }
    }

    /**
     * Преобразование SQL-запросов
     *
     * @return SQL-запрос с преобразованием колонок, содержащих шифрование
     * @throws RuntimeException при ошибке загрузки конфигурации
     */
    public String parseSql() {
        String roleName = switch (this.username) {
            case "admin" -> "admin_role";
            case "seller" -> "seller_role";
            case "buyer" -> "buyer_role";
            default -> throw new RuntimeException("Неизвестный пользователь: " + this.username);
        };

        RoleConfiguration config = this.roleConfigurations.get(roleName);
        if (config == null || config.encryptedColumns.length == 0) {
            return this.sql;
        }

        return this.replacingColumn(config);
    }

    /**
     * Применяет к зашифрованным колонкам gost_kuz_decrypt()
     *
     * @param config конфигурация роли (содержит зашифрованные колонки)
     * @return SQL-запрос с преобразованием колонок, содержащих шифрование
     */
    private String replacingColumn(RoleConfiguration config) {
        Set<String> processed = new HashSet<>();
        String columnList = String.join("|", config.encryptedColumns);

        // Паттерн не заходит внутрь '...' и "..."
        String patternStr = "(?i)\\b(\\w+\\.)?(" + columnList + ")\\b(?=\\s|[,;)$]|\\s+AS\\b)";
        Pattern pattern = Pattern.compile(patternStr);

        // Разбиваем SQL на токены, исключая кавычки
        Matcher matcher = pattern.matcher(this.sql);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            // Проверяем не внутри ли мы кавычек
            if (this.isInsideQuotes(this.sql, start)) {
                // Пропускаем — это строка, а не колонка
                result.append(this.sql, lastEnd, end);
                lastEnd = end;
                continue;
            }

            String alias = matcher.group(1); // b.
            String column = matcher.group(2).toLowerCase();
            String fullColumn = (alias != null ? alias : "") + column;

            if (processed.contains(fullColumn)) {
                result.append(this.sql, lastEnd, end);
                lastEnd = end;
                continue;
            }

            String table = this.getTableNameForColumn(column, config.tableMappings);
            String tableRef = alias != null ? alias.substring(0, alias.length() - 1) : table;

            // Формируем вызов
            String decryptCall = String.format(
                    "gost_kuz_decrypt(\"%s\".\"%s\", (SELECT get_data_key('%s', '%s', '%s')))",
                    tableRef, column, table, column, config.roleName
            );

            // Сохраняем AS, если есть
            String after = this.sql.substring(end);
            if (after.matches("(?i)^\\s+AS\\s+\\w+.*")) {
                int asEnd = after.indexOf(' ', after.toLowerCase().indexOf(" as ") + 4);
                if (asEnd == -1) asEnd = after.length();
                String asClause = after.substring(0, asEnd);
                decryptCall += asClause;
                end += asEnd;
            }

            result.append(this.sql, lastEnd, start);
            result.append(decryptCall);
            lastEnd = end;
            processed.add(fullColumn);
        }

        result.append(this.sql.substring(lastEnd));
        return result.toString();
    }

    /**
     * Проверяет, находится ли позиция внутри одинарных или двойных кавычек
     *
     * @param sql SQL-запрос
     * @param pos проверяемая позиция
     * @return находится ли позиция внутри одинарных или двойных кавычек
     */
    private boolean isInsideQuotes(String sql, int pos) {
        boolean inSingle = false;
        boolean inDouble = false;

        for (int i = 0; i < pos; i++) {
            char c = sql.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            }
        }
        return inSingle || inDouble;
    }

    /**
     * Получаем название таблицы, в которой находится колонка
     *
     * @param column колонка
     * @param mappings сопоставление колонок и таблиц
     * @return название таблицы
     * @throws RuntimeException если таблица не найдена
     */
    private String getTableNameForColumn(String column, Map<String, String> mappings) {
        String table = mappings.get(column);
        if (table == null) {
            throw new RuntimeException("Нет таблицы для колонки: " + column);
        }
        return table;
    }
}