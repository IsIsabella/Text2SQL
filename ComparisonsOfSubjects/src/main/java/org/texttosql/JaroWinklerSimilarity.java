package org.texttosql;

import javax.annotation.Nullable;

/**
 * Реализация Jaro-Winkler similarity, учитывающая совпадения символов с
 * учётом окна и дающая бонус за общий префикс (до 4 символов)
 */
public class JaroWinklerSimilarity {

    private static final double SCALING_FACTOR = 0.1;  // стандартное значение из оригинала
    private static final int MAX_PREFIX_BONUS_LENGTH = 4;

    /**
     * Вычисляет сходство Jaro-Winkler между двумя строками
     *
     * @param left  первая строка
     * @param right вторая строка
     * @return значение от 0.0 до 1.0
     */
    public double apply(@Nullable CharSequence left, @Nullable CharSequence right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Строки не могут быть null");
        }

        if (left.length() == 0 || right.length() == 0) {
            return 0.0;
        }

        if (left.toString().equals(right.toString())) {
            return 1.0;
        }

        // Определяем, какая строка длиннее
        CharSequence max, min;
        if (left.length() > right.length()) {
            max = left;
            min = right;
        } else {
            max = right;
            min = left;
        }

        int range = Math.max(max.length() / 2 - 1, 0);

        int[] matchIndices = new int[min.length()];
        boolean[] matchedInMax = new boolean[max.length()];
        int matches = 0;

        // Поиск совпадающих символов в пределах окна
        for (int i = 0; i < min.length(); i++) {
            char c1 = min.charAt(i);
            int start = Math.max(0, i - range);
            int end = Math.min(i + range + 1, max.length());

            for (int j = start; j < end; j++) {
                if (!matchedInMax[j] && c1 == max.charAt(j)) {
                    matchIndices[i] = j;
                    matchedInMax[j] = true;
                    matches++;
                    break;
                }
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        // Считаем транспозиции (полуперестановки)
        int transpositions = 0;
        int k = 0;
        for (int i = 0; i < min.length(); i++) {
            if (matchIndices[i] != -1) {
                if (min.charAt(i) != max.charAt(matchIndices[i])) {
                    transpositions++;
                }
                k++;
            }
        }
        transpositions /= 2;

        // Базовая формула Jaro
        double jaro = (
                (double) matches / min.length() +
                        (double) matches / max.length() +
                        (double) (matches - transpositions) / matches) / 3.0;

        // Winkler: бонус за общий префикс (максимум 4 символа)
        int prefixLength = 0;
        int maxCheck = Math.min(Math.min(left.length(), right.length()), MAX_PREFIX_BONUS_LENGTH);
        for (int i = 0; i < maxCheck; i++) {
            if (left.charAt(i) == right.charAt(i)) {
                prefixLength++;
            } else {
                break;
            }
        }

        double jaroWinkler = jaro + (prefixLength * SCALING_FACTOR * (1.0 - jaro));

        return Math.min(jaroWinkler, 1.0); // не больше 1.0
    }
}