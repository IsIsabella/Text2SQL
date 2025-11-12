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
     * ToDO: решить каким образом будет храниться этот словарь, и откуда будет браться эта информация
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
