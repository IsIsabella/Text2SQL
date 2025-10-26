package org.texttosql.server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс для валидации SQL-запросов перед их выполнением в базе данных,
 * разрешает только SELECT-запросы (включая WITH для CTE),
 * запрещает любые операции, создающие или изменяющие объекты (DDL, DML кроме SELECT),
 * доступ к системным схемам/таблицам и множественные statements
 */
public class SqlValidator {
    /**
     * Список запрещенных ключевых слов (операций)
     */
    private static final String[] FORBIDDEN_KEYWORDS = {
            "drop", "delete", "insert", "update", "alter", "create", "truncate",
            "grant", "revoke", "exec", "execute", "set", "begin", "commit", "rollback"
    };

    /**
     * Список запрещенных схем/префиксов (системные таблицы PostgreSQL)
     */
    private static final String[] FORBIDDEN_SCHEMAS = {
            "pg_", "information_schema", "pg_catalog", "pg_authid", "pg_user", "pg_roles"
    };

    /**
     * Список разрешенных таблиц в базе данных bookstore
     */
    private static final Set<String> ALLOWED_TABLES = new HashSet<>(Arrays.asList(
            "authors", "books", "publishinghouse", "circulation", "cheque", "wrote", "contains"
    ));

    /**
     * Валидирует SQL-запрос
     *
     * @param sql SQL-запрос для проверки
     * @throws Exception Если запрос содержит запрещенные элементы
     */
    public void validate(String sql) throws Exception {
        if (sql == null || sql.trim().isEmpty()) {
            throw new Exception("SQL-запрос пуст");
        }

        String normalized = normalizeSql(sql);
        String trimmed = normalized.trim();

        // Проверка на множественные statements
        if (normalized.split(";").length > 2) {
            throw new Exception("Множественные SQL-statements запрещены");
        }

        // Проверка типа запроса
        Pattern selectPattern = Pattern.compile("^\\s*(with|select)");
        boolean isSelect = selectPattern.matcher(trimmed).find();
        if (!isSelect) {
            throw new Exception("Разрешены только SELECT-запросы (с WITH для CTE)");
        }

        // Проверка запрещенных ключевых слов
        for (String kw : SqlValidator.FORBIDDEN_KEYWORDS) {
            if (normalized.contains(kw)) {
                throw new Exception("Запрещенная операция: " + kw.toUpperCase());
            }
        }

        // Проверка на доступ к запрещенным схемам
        for (String schema : SqlValidator.FORBIDDEN_SCHEMAS) {
            if (normalized.contains(schema)) {
                throw new Exception("Доступ к запрещенной схеме/таблице: " + schema.toUpperCase());
            }
        }

        // Извлечение CTE-имен (алиасы)
        Set<String> cteNames = this.extractCteNames(normalized);

        // Извлечение таблиц
        Set<String> usedTables = this.extractTables(normalized);

        // Проверка таблиц (игнорируя CTE-алиасы)
        for (String table : usedTables) {
            if (cteNames.contains(table)) {
                continue;
            }
            if (!SqlValidator.ALLOWED_TABLES.contains(table.toLowerCase())) {
                throw new Exception("Доступ к неразрешенной таблице: " + table);
            }
        }
    }

    /**
     * Нормализует SQL: приводит к нижнему регистру, удаляет комментарии
     *
     * @param sql Исходный SQL
     * @return Нормализованный SQL
     */
    private String normalizeSql(String sql) {
        // Удаление однострочных комментариев
        sql = sql.replaceAll("--[^\n]*", "");
        // Удаление многострочных комментариев
        sql = sql.replaceAll("/\\*.*?\\*/", "");
        // Удаление лишних пробелов и приведение к нижнему регистру
        return sql.toLowerCase().replaceAll("\\s+", " ");
    }

    /**
     * Извлекает имена CTE (алиасы) из WITH-части
     *
     * @param normalized Нормализованный SQL
     * @return Set с именами CTE
     */
    private Set<String> extractCteNames(String normalized) {
        Set<String> cteNames = new HashSet<>();
        if (normalized.startsWith("with ")) {
            Pattern pattern = Pattern.compile("(\\w+)\\s+as");
            Matcher matcher = pattern.matcher(normalized);
            while (matcher.find()) {
                cteNames.add(matcher.group(1));
            }
        }
        return cteNames;
    }

    /**
     * Извлекает имена таблиц из SQL-запроса, игнорируя алиасы (например, 'Books B')
     * Поддерживает FROM, JOIN и подзапросы (рекурсивно)
     *
     * @param normalized Нормализованный SQL
     * @return Set с именами таблиц
     */
    private Set<String> extractTables(String normalized) {
        Set<String> tables = new HashSet<>();
        // Regex для поиска таблиц после FROM, JOIN, игнорируя алиасы
        Pattern pattern = Pattern.compile("(from|join)\\s+(\\w+)(?:\\s+\\w+)?");
        Matcher matcher = pattern.matcher(normalized);

        while (matcher.find()) {
            tables.add(matcher.group(2)); // Извлекаем имя таблицы (группа 2)
        }

        // Проверка подзапросов (рекурсия)
        int subqueryStart = normalized.indexOf("(");
        while (subqueryStart != -1) {
            int subqueryEnd = this.findMatchingClosingParen(normalized, subqueryStart);
            if (subqueryEnd != -1) {
                String subquery = normalized.substring(subqueryStart + 1, subqueryEnd);
                tables.addAll(this.extractTables(subquery));
                subqueryStart = normalized.indexOf("(", subqueryEnd);
            } else {
                subqueryStart = -1;
            }
        }

        return tables;
    }

    /**
     * Находит соответствующую закрывающую скобку для открывающей
     *
     * @param sql SQL-строка
     * @param start Индекс открывающей скобки
     * @return Индекс закрывающей скобки или -1
     */
    private int findMatchingClosingParen(String sql, int start) {
        int count = 0;
        for (int i = start; i < sql.length(); i++) {
            if (sql.charAt(i) == '(') {
                count++;
            } else if (sql.charAt(i) == ')') {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}