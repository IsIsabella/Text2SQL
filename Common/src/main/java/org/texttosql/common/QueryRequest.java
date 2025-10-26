package org.texttosql.common;

import java.util.Map;

/**
 * Класс для передачи данных запроса на естественном языке и выбранных таблиц.
 */
public class QueryRequest {
    /**
     * Запрос на естественном языке
     */
    private String naturalLanguage;
    /**
     * Маппинг выбранных таблиц (имя таблицы -> выбрана ли)
     */
    private Map<String, Boolean> tableSelected;

    /**
     * Конструктор по умолчанию
     */
    public QueryRequest() {
    }

    /**
     * Конструктор с параметрами
     *
     * @param naturalLanguage Запрос на естественном языке
     * @param tableSelected   Маппинг выбранных таблиц
     */
    public QueryRequest(String naturalLanguage, Map<String, Boolean> tableSelected) {
        this.naturalLanguage = naturalLanguage;
        this.tableSelected = tableSelected;
    }

    /**
     * Получает запрос на естественном языке
     *
     * @return Запрос на естественном языке
     */
    public String getNaturalLanguage() {
        return naturalLanguage;
    }

    /**
     * Устанавливает запрос на естественном языке
     *
     * @param naturalLanguage Запрос на естественном языке
     */
    public void setNaturalLanguage(String naturalLanguage) {
        this.naturalLanguage = naturalLanguage;
    }

    /**
     * Получает маппинг выбранных таблиц
     *
     * @return Маппинг выбранных таблиц
     */
    public Map<String, Boolean> getTableSelected() {
        return tableSelected;
    }

    /**
     * Устанавливает маппинг выбранных таблиц
     *
     * @param tableSelected Маппинг выбранных таблиц
     */
    public void setTableSelected(Map<String, Boolean> tableSelected) {
        this.tableSelected = tableSelected;
    }
}