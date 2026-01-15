package org.texttosql;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Словарь синонимов для должностей
 */
public class PositionSynonyms {
    /**
     * Словарь с сокращенными/транслитерированными и полными названиями должностей
     */
    private static final Map<String, String> SYNONYMS = new HashMap<>();

    static {
        PositionSynonyms.SYNONYMS.put("директор", "руководитель");
        PositionSynonyms.SYNONYMS.put("учитель", "педагог");
        PositionSynonyms.SYNONYMS.put("врач", "медик");
        PositionSynonyms.SYNONYMS.put("программист", "разработчик");
        PositionSynonyms.SYNONYMS.put("programmer", "программист");
        PositionSynonyms.SYNONYMS.put("director", "директор");
        PositionSynonyms.SYNONYMS.put("teacher", "учитель");
        PositionSynonyms.SYNONYMS.put("doctor", "врач");
        PositionSynonyms.SYNONYMS.put("manager", "менеджер");
        PositionSynonyms.SYNONYMS.put("админ", "администратор");
        PositionSynonyms.SYNONYMS.put("admin", "администратор");
        PositionSynonyms.SYNONYMS.put("системный администратор", "администратор");
        PositionSynonyms.SYNONYMS.put("sysadmin", "администратор");
        PositionSynonyms.SYNONYMS.put("продавец", "менеджер");
        PositionSynonyms.SYNONYMS.put("консультант", "менеджер");
        PositionSynonyms.SYNONYMS.put("sales", "менеджер");
        PositionSynonyms.SYNONYMS.put("sales manager", "менеджер");
        PositionSynonyms.SYNONYMS.put("руководитель отдела", "руководитель");
        PositionSynonyms.SYNONYMS.put("начальник", "руководитель");
        PositionSynonyms.SYNONYMS.put("boss", "руководитель");
        PositionSynonyms.SYNONYMS.put("head", "руководитель");
        PositionSynonyms.SYNONYMS.put("бухгалтер", "бухгалтер");
        PositionSynonyms.SYNONYMS.put("accountant", "бухгалтер");
        PositionSynonyms.SYNONYMS.put("главбух", "главный бухгалтер");
        PositionSynonyms.SYNONYMS.put("chief accountant", "главный бухгалтер");
        PositionSynonyms.SYNONYMS.put("секретарь", "секретарь");
        PositionSynonyms.SYNONYMS.put("assistant", "секретарь");
        PositionSynonyms.SYNONYMS.put("офис-менеджер", "секретарь");
        PositionSynonyms.SYNONYMS.put("инженер", "инженер");
        PositionSynonyms.SYNONYMS.put("engineer", "инженер");
        PositionSynonyms.SYNONYMS.put("техник", "инженер");
        PositionSynonyms.SYNONYMS.put("разработчик", "разработчик");
        PositionSynonyms.SYNONYMS.put("developer", "разработчик");
        PositionSynonyms.SYNONYMS.put("dev", "разработчик");
        PositionSynonyms.SYNONYMS.put("coder", "разработчик");
        PositionSynonyms.SYNONYMS.put("аналитик", "аналитик");
        PositionSynonyms.SYNONYMS.put("analyst", "аналитик");
        PositionSynonyms.SYNONYMS.put("data analyst", "аналитик");
        PositionSynonyms.SYNONYMS.put("дизайнер", "дизайнер");
        PositionSynonyms.SYNONYMS.put("designer", "дизайнер");
        PositionSynonyms.SYNONYMS.put("ui/ux", "дизайнер");
        PositionSynonyms.SYNONYMS.put("маркетолог", "маркетолог");
        PositionSynonyms.SYNONYMS.put("marketer", "маркетолог");
        PositionSynonyms.SYNONYMS.put("smm", "маркетолог");
        PositionSynonyms.SYNONYMS.put("hr", "специалист по кадрам");
        PositionSynonyms.SYNONYMS.put("рекрутер", "специалист по кадрам");
        PositionSynonyms.SYNONYMS.put("recruiter", "специалист по кадрам");
        PositionSynonyms.SYNONYMS.put("юрист", "юрист");
        PositionSynonyms.SYNONYMS.put("lawyer", "юрист");
        PositionSynonyms.SYNONYMS.put("юрисконсульт", "юрист");
        PositionSynonyms.SYNONYMS.put("логист", "логист");
        PositionSynonyms.SYNONYMS.put("логист-менеджер", "логист");
        PositionSynonyms.SYNONYMS.put("кассир", "кассир");
        PositionSynonyms.SYNONYMS.put("cashier", "кассир");
    }

    /**
     * Нормализует должность с учетом синонимов
     *
     * @param position должность субъекта доступа
     */
    public static String getCanonicalPosition(@Nullable String position) {
        if (position == null) {
            return null;
        }
        return PositionSynonyms.SYNONYMS.getOrDefault(position.toLowerCase(), position.toLowerCase());
    }
}
