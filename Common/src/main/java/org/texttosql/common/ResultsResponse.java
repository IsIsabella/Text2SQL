package org.texttosql.common;

/**
 * DTO для передачи результатов выполнения SQL-запроса или ошибки от сервера к клиенту
 */
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
     * @param results Результаты выполнения SQL-запроса
     */
    public ResultsResponse(String results) {
        this.results = results;
    }

    /**
     * Конструктор с результатами и ошибкой
     *
     * @param results Результаты выполнения SQL-запроса (может быть null)
     * @param error   Сообщение об ошибке
     */
    public ResultsResponse(String results, String error) {
        this.results = results;
        this.error = error;
    }

    /**
     * Получает результаты выполнения SQL-запроса
     *
     * @return Результаты выполнения SQL-запроса
     */
    public String getResults() {
        return results;
    }

    /**
     * Устанавливает результаты выполнения SQL-запроса
     *
     * @param results Результаты выполнения SQL-запроса
     */
    public void setResults(String results) {
        this.results = results;
    }

    /**
     * Получает сообщение об ошибке
     *
     * @return Сообщение об ошибке
     */
    public String getError() {
        return error;
    }

    /**
     * Устанавливает сообщение об ошибке
     *
     * @param error Сообщение об ошибке
     */
    public void setError(String error) {
        this.error = error;
    }
}