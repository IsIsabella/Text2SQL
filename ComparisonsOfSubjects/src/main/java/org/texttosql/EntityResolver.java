package org.texttosql;

import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Основной класс для принятия решения о схожести субъектов
 */
@Immutable
public class EntityResolver {
    private static final LevenshteinDistance LEVENSHTEIN = new LevenshteinDistance();
    private static final JaroWinklerSimilarity JARO = new JaroWinklerSimilarity();

    // Назначаем веса признаков
    private static final double WEIGHT_LAST = 0.2;
    private static final double WEIGHT_FIRST = 0.15;
    private static final double WEIGHT_MIDDLE = 0.05;
    private static final double WEIGHT_EMAIL = 0.3;
    private static final double WEIGHT_PHONE = 0.2;
    private static final double WEIGHT_POSITION = 0.1;

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
        int dist = EntityResolver.LEVENSHTEIN.apply(s1, s2);
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
    private static double JaroWinklerSimilarity(@Nullable String s1, @Nullable String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }
        if (s1.equals(s2)) {
            return 1.0;
        }
        return EntityResolver.JARO.apply(s1, s2);
    }

    /**
     * Вычисляет вероятность совпадения двух субъектов
     *
     * @param subject1 первый субъект
     * @param subject2 второй субъект
     * @return вероятность совпадения двух субъектов
     */
    public static double calculateMatchProbability(AccessSubject subject1, AccessSubject subject2) {
        // Нормализуем данные
        AccessSubject norm1 = DataNormalizer.normalize(subject1);
        AccessSubject norm2 = DataNormalizer.normalize(subject2);

        // Вычисляем сходства по всем параметрам
        double lastNameSim = EntityResolver.JaroWinklerSimilarity(norm1.getLastName(), norm2.getLastName());
        double firstNameSim = EntityResolver.JaroWinklerSimilarity(norm1.getFirstName(), norm2.getFirstName());
        double middleNameSim = 0.0;
        if (norm1.getMiddleName() != null && norm2.getMiddleName() != null) {
            middleNameSim = EntityResolver.JaroWinklerSimilarity(norm1.getMiddleName(), norm2.getMiddleName());
        }
        double emailSim = EntityResolver.LevenshtainSimilarity(norm1.getEmail(), norm2.getEmail());
        double phoneSim = EntityResolver.LevenshtainSimilarity(norm1.getPhone(), norm2.getPhone());
        double posSim = EntityResolver.JaroWinklerSimilarity(norm1.getPosition(), norm2.getPosition());

        // Множитель редкости только для фамилии
        double rarity = SurnameRarity.getRarityMultiplier(norm1.getLastName());

        // Взвешенная сумма
        double fioProb = (EntityResolver.WEIGHT_LAST * lastNameSim * rarity) +
                EntityResolver.WEIGHT_FIRST * firstNameSim + EntityResolver.WEIGHT_MIDDLE * middleNameSim;
        double probability = fioProb + EntityResolver.WEIGHT_EMAIL * emailSim +
                EntityResolver.WEIGHT_PHONE * phoneSim + EntityResolver.WEIGHT_POSITION * posSim;

        // Нормализуем, чтобы не выходило за 1
        return Math.min(probability, 1.0);
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
        } else if (probability >= 0.7) {
            return MatchStatus.MANUAL_VERIFICATION;
        } else {
            return MatchStatus.DIFFERENT;
        }
    }
}
