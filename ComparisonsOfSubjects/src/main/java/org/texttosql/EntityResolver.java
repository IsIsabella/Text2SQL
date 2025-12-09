package org.texttosql;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Основной класс для принятия решения о схожести субъектов
 * Поддерживает два режима:
 * 1. CUSTOM
 * 2. APACHE_COMMONS
 */
@Immutable
public class EntityResolver {

    // Кастомные метрики
    private static final LevenshteinDistance CUSTOM_LEVENSHTEIN = new LevenshteinDistance();
    private static final JaroWinklerSimilarity CUSTOM_JARO = new JaroWinklerSimilarity();

    // Apache Commons Text метрики
    private static final org.apache.commons.text.similarity.LevenshteinDistance APACHE_LEVENSHTEIN
            = org.apache.commons.text.similarity.LevenshteinDistance.getDefaultInstance();
    private static final org.apache.commons.text.similarity.JaroWinklerSimilarity APACHE_JARO
            = new org.apache.commons.text.similarity.JaroWinklerSimilarity();

    // Назначаем веса признаков
    private static final double WEIGHT_LAST = 0.2;
    private static final double WEIGHT_FIRST = 0.15;
    private static final double WEIGHT_MIDDLE = 0.05;
    private static final double WEIGHT_EMAIL = 0.3;
    private static final double WEIGHT_PHONE = 0.2;
    private static final double WEIGHT_POSITION = 0.1;

    /**
     * Вычисляет вероятность совпадения двух субъектов, используя кастомные алгоритмы
     *
     * @param subject1 первый субъект
     * @param subject2 второй субъект
     * @return вероятность совпадения двух субъектов
     */
    public static double calculateMatchProbabilityCustom(AccessSubject subject1, AccessSubject subject2) {
        AccessSubject norm1 = DataNormalizer.normalize(subject1);
        AccessSubject norm2 = DataNormalizer.normalize(subject2);

        double lastNameSim = EntityResolver.JaroWinklerSimilarity(norm1.getLastName(), norm2.getLastName());
        double firstNameSim = EntityResolver.JaroWinklerSimilarity(norm1.getFirstName(), norm2.getFirstName());
        double middleNameSim = (norm1.getMiddleName() != null && norm2.getMiddleName() != null)
                ? EntityResolver.JaroWinklerSimilarity(norm1.getMiddleName(), norm2.getMiddleName()) : 0.0;
        double emailSim = EntityResolver.LevenshtainSimilarity(norm1.getEmail(), norm2.getEmail());
        double phoneSim = EntityResolver.LevenshtainSimilarity(norm1.getPhone(), norm2.getPhone());
        double posSim = EntityResolver.JaroWinklerSimilarity(norm1.getPosition(), norm2.getPosition());

        double rarity = SurnameRarity.getRarityMultiplier(norm1.getLastName());

        double fioProb = (EntityResolver.WEIGHT_LAST * lastNameSim * rarity) +
                (EntityResolver.WEIGHT_FIRST * firstNameSim) +
                (EntityResolver.WEIGHT_MIDDLE * middleNameSim);

        double probability = fioProb +
                EntityResolver.WEIGHT_EMAIL * emailSim +
                EntityResolver.WEIGHT_PHONE * phoneSim +
                EntityResolver.WEIGHT_POSITION * posSim;

        return Math.min(probability, 1.0);
    }

    /**
     * Вычисляет вероятность совпадения двух субъектов, используя алгоритмы из org.apache.commons
     *
     * @param subject1 первый субъект
     * @param subject2 второй субъект
     * @return вероятность совпадения двух субъектов
     */
    public static double calculateMatchProbabilityApache(AccessSubject subject1, AccessSubject subject2) {
        AccessSubject norm1 = DataNormalizer.normalize(subject1);
        AccessSubject norm2 = DataNormalizer.normalize(subject2);

        double lastNameSim = EntityResolver.apacheJaroWinklerSimilarity(norm1.getLastName(), norm2.getLastName());
        double firstNameSim = EntityResolver.apacheJaroWinklerSimilarity(norm1.getFirstName(), norm2.getFirstName());
        double middleNameSim = (norm1.getMiddleName() != null && norm2.getMiddleName() != null)
                ? EntityResolver.apacheJaroWinklerSimilarity(norm1.getMiddleName(), norm2.getMiddleName()) : 0.0;
        double emailSim = EntityResolver.apacheLevenshteinSimilarity(norm1.getEmail(), norm2.getEmail());
        double phoneSim = EntityResolver.apacheLevenshteinSimilarity(norm1.getPhone(), norm2.getPhone());
        double posSim = EntityResolver.apacheJaroWinklerSimilarity(norm1.getPosition(), norm2.getPosition());

        double rarity = SurnameRarity.getRarityMultiplier(norm1.getLastName());

        double fioProb = (EntityResolver.WEIGHT_LAST * lastNameSim * rarity) +
                (EntityResolver.WEIGHT_FIRST * firstNameSim) +
                (EntityResolver.WEIGHT_MIDDLE * middleNameSim);

        double probability = fioProb +
                EntityResolver.WEIGHT_EMAIL * emailSim +
                EntityResolver.WEIGHT_PHONE * phoneSim +
                EntityResolver.WEIGHT_POSITION * posSim;

        return Math.min(probability, 1.0);
    }

    /**
     * Вычисляет сходство на основе Jaro-Winkler
     *
     * @param s1 первая строка
     * @param s2 вторая строка
     * @return нормализованное сходство на основе Jaro-Winkler
     */
    private static double JaroWinklerSimilarity(@Nullable String s1, @Nullable String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        } else {
            return EntityResolver.CUSTOM_JARO.apply(s1, s2);
        }
    }

    /**
     * Вычисляет нормализованное сходство на основе расстояния Левенштейна
     *
     * @param s1 первая строка
     * @param s2 вторая строка
     * @return нормализованное сходство на основе расстояния Левенштейна
     */
    private static double LevenshtainSimilarity(@Nullable String s1, @Nullable String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }
        int dist = EntityResolver.CUSTOM_LEVENSHTEIN.apply(s1, s2);

        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) dist / maxLen);
    }

    /**
     * Вычисляет сходство на основе Jaro-Winkler
     *
     * @param s1 первая строка
     * @param s2 вторая строка
     * @return нормализованное сходство на основе Jaro-Winkler
     */
    private static double apacheJaroWinklerSimilarity(@Nullable String s1, @Nullable String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        } else {
            return EntityResolver.APACHE_JARO.apply(s1, s2);
        }
    }

    /**
     * Вычисляет нормализованное сходство на основе расстояния Левенштейна
     *
     * @param s1 первая строка
     * @param s2 вторая строка
     * @return нормализованное сходство на основе расстояния Левенштейна
     */
    private static double apacheLevenshteinSimilarity(@Nullable String s1, @Nullable String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }
        Integer dist = EntityResolver.APACHE_LEVENSHTEIN.apply(s1, s2);
        if (dist == null) {
            return 0.0;
        }
        int maxLen = Math.max(s1.length(), s2.length());
        return 1.0 - ((double) dist / maxLen);
    }

    /**
     * Определяет статус на основе вероятности
     *
     * @param probability вероятность совпадения двух субъектов
     * @return статус
     */
    public static MatchStatus getMatchStatus(double probability) {
        if (probability >= 0.9) {
            return MatchStatus.MATCH;
        }
        if (probability >= 0.7) {
            return MatchStatus.MANUAL_VERIFICATION;
        } else {
            return MatchStatus.DIFFERENT;
        }
    }
}