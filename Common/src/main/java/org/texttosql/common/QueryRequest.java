package org.texttosql.common;

import net.jcip.annotations.NotThreadSafe;

import java.util.Map;

/**
 * Класс для передачи данных запроса на естественном языке и выбранных таблиц
 */
@NotThreadSafe
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
     * @param naturalLanguage запрос на естественном языке
     * @param tableSelected   маппинг выбранных таблиц
     */
    public QueryRequest(String naturalLanguage, Map<String, Boolean> tableSelected) {
        this.naturalLanguage = naturalLanguage;
        this.tableSelected = tableSelected;
    }

    /**
     * Получает запрос на естественном языке
     *
     * @return запрос на естественном языке
     */
    public String getNaturalLanguage() {
        return this.naturalLanguage;
    }

    /**
     * Устанавливает запрос на естественном языке
     *
     * @param naturalLanguage запрос на естественном языке
     */
    public void setNaturalLanguage(String naturalLanguage) {
        this.naturalLanguage = naturalLanguage;
    }

    /**
     * Получает маппинг выбранных таблиц
     *
     * @return маппинг выбранных таблиц
     */
    public Map<String, Boolean> getTableSelected() {
        return this.tableSelected;
    }

    /**
     * Устанавливает маппинг выбранных таблиц
     *
     * @param tableSelected маппинг выбранных таблиц
     */
    public void setTableSelected(Map<String, Boolean> tableSelected) {
        this.tableSelected = tableSelected;
    }
}