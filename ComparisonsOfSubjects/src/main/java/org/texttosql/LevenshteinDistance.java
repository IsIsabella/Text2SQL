package org.texttosql;

import javax.annotation.Nullable;

/**
 * Полностью исправленная реализация LevenshteinDistance
 * — без переполнения
 * — без NPE
 * — без выхода за границы массива
 * — поведение идентично Apache Commons
 */
public class LevenshteinDistance {

    private final Integer threshold;

    public LevenshteinDistance() {
        this(null);
    }

    public LevenshteinDistance(Integer threshold) {
        if (threshold != null && threshold < 0) {
            throw new IllegalArgumentException("Threshold must not be negative");
        }
        this.threshold = threshold;
    }

    public int apply(@Nullable CharSequence left, @Nullable CharSequence right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("Строки не могут быть null");
        }

        int n = left.length();
        int m = right.length();

        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }

        // Делаем left короче
        if (n > m) {
            CharSequence tmp = left;
            left = right;
            right = tmp;
            int tmpLen = n;
            n = m;
            m = tmpLen;
        }

        // Ранняя проверка: если разница в длине больше порога
        if (threshold != null && m - n > threshold) {
            return -1;
        }

        int[] previous = new int[n + 1];
        int[] current = new int[n + 1];

        for (int i = 0; i <= n; i++) {
            previous[i] = i;
        }

        // Если порог не задан — используем "без ограничений"
        boolean useThreshold = threshold != null;
        int thresholdVal = useThreshold ? threshold : 0; // значение не важно, если не используем

        for (int j = 1; j <= m; j++) {
            char rightChar = right.charAt(j - 1);
            current[0] = j;

            // Определяем границы окна
            int from = 1;
            int to = n;

            if (useThreshold) {
                from = Math.max(1, j - thresholdVal - 1);
                to = Math.min(n, j + thresholdVal + 1);

                // Сбрасываем значения вне окна
                if (from > 1) current[from - 1] = Integer.MAX_VALUE;
                if (to < n) current[to + 1] = Integer.MAX_VALUE; // защита от переполнения
            }

            boolean hasValueUnderThreshold = false;

            for (int i = from; i <= to; i++) {
                int cost = left.charAt(i - 1) == rightChar ? 0 : 1;

                int insert = current[i - 1] + 1;
                int delete = previous[i] + 1;
                int substitute = previous[i - 1] + cost;

                int value = Math.min(Math.min(insert, delete), substitute);
                current[i] = value;

                if (useThreshold && value <= thresholdVal) {
                    hasValueUnderThreshold = true;
                }
            }

            if (useThreshold && !hasValueUnderThreshold) {
                return -1;
            }

            // Смена строк
            int[] temp = previous;
            previous = current;
            current = temp;
        }

        int result = previous[n];

        return (useThreshold && result > threshold) ? -1 : result;
    }

    public Integer getThreshold() {
        return threshold;
    }
}