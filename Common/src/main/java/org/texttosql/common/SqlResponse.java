package org.texttosql.common;

import net.jcip.annotations.NotThreadSafe;

/**
 * Класс для передачи сгенерированного SQL-запроса или ошибки от сервера к клиенту
 */
@NotThreadSafe
public class SqlResponse {
    /**
     * Сгенерированный SQL-запрос
     */
    private String sql;
    /**
     * Сообщение об ошибке, если произошла ошибка
     */
    private String error;

    /**
     * Конструктор по умолчанию
     */
    public SqlResponse() {
    }

    /**
     * Конструктор с успешным SQL-запросом
     *
     * @param sql сгенерированный SQL-запрос
     */
    public SqlResponse(String sql) {
        this.sql = sql;
    }

    /**
     * Конструктор с SQL-запросом и ошибкой
     *
     * @param sql   сгенерированный SQL-запрос (может быть null)
     * @param error сообщение об ошибке
     */
    public SqlResponse(String sql, String error) {
        this.sql = sql;
        this.error = error;
    }

    /**
     * Получает SQL-запрос
     *
     * @return SQL-запрос
     */
    public String getSql() {
        return this.sql;
    }

    /**
     * Устанавливает SQL-запрос
     *
     * @param sql SQL-запрос
     */
    public void setSql(String sql) {
        this.sql = sql;
    }

    /**
     * Получает сообщение об ошибке
     *
     * @return сообщение об ошибке
     */
    public String getError() {
        return this.error;
    }

    /**
     * Устанавливает сообщение об ошибке
     *
     * @param error сообщение об ошибке
     */
    public void setError(String error) {
        this.error = error;
    }
}