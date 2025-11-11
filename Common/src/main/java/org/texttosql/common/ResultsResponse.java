package org.texttosql.common;

import net.jcip.annotations.NotThreadSafe;

/**
 * DTO для передачи результатов выполнения SQL-запроса или ошибки от сервера к клиенту
 */
@NotThreadSafe
public class ResultsResponse {
    /**
     * Результаты выполнения SQL-запроса в текстовом формате
     */
    private String results;
    /**
     * Сообщение об ошибке, если произошла ошибка
     */
    private String error;

    /**
     * Конструктор по умолчанию
     */
    public ResultsResponse() {
    }

    /**
     * Конструктор с успешными результатами
     *
     * @param results результаты выполнения SQL-запроса
     */
    public ResultsResponse(String results) {
        this.results = results;
    }

    /**
     * Конструктор с результатами и ошибкой
     *
     * @param results результаты выполнения SQL-запроса (может быть null)
     * @param error   сообщение об ошибке
     */
    public ResultsResponse(String results, String error) {
        this.results = results;
        this.error = error;
    }

    /**
     * Получает результаты выполнения SQL-запроса
     *
     * @return результаты выполнения SQL-запроса
     */
    public String getResults() {
        return this.results;
    }

    /**
     * Устанавливает результаты выполнения SQL-запроса
     *
     * @param results результаты выполнения SQL-запроса
     */
    public void setResults(String results) {
        this.results = results;
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

    /**
     * Успешный результат
     *
     * @param result результат выполнения SQL-запроса
     */
    public static ResultsResponse success(String result) {
        return new ResultsResponse(result, null);
    }

    /**
     * Ошибка
     *
     * @param error сообщение об ошибке
     */
    public static ResultsResponse error(String error) {
        return new ResultsResponse(null, error);
    }
}