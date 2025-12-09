package org.texttosql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Сравнение результатов CUSTOM и APACHE_COMMONS реализаций")
class EntityResolverTest {

    private static final double PROBABILITY_DELTA = 0.0001; // допустимая погрешность

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvFileSource(
            resources = "/entity_resolver_test_data.csv",
            numLinesToSkip = 1,
            delimiter = ',',
            encoding = "UTF-8")
    void customAndApacheShouldGiveSameResult(
            String comment,
            String lastName1, String firstName1, String middleName1,
            String email1, String phone1, String position1,
            String lastName2, String firstName2, String middleName2,
            String email2, String phone2, String position2) {

        AccessSubject s1 = new AccessSubject(lastName1, firstName1, middleName1, email1, phone1, position1);
        AccessSubject s2 = new AccessSubject(lastName2, firstName2, middleName2, email2, phone2, position2);

        double probCustom = EntityResolver.calculateMatchProbabilityCustom(s1, s2);
        double probApache = EntityResolver.calculateMatchProbabilityApache(s1, s2);

        MatchStatus statusCustom = EntityResolver.getMatchStatus(probCustom);
        MatchStatus statusApache = EntityResolver.getMatchStatus(probApache);

        // Красивый вывод для анализа
        System.out.printf("%-10s | Custom: %.4f → %-22s | Apache: %.4f → %-10s %n%n",
                comment.length() > 67 ? comment.substring(0, 64) + "..." : comment,
                probCustom, statusCustom,
                probApache, statusApache);

        // Объективные проверки
        assertEquals(probApache, probCustom, EntityResolverTest.PROBABILITY_DELTA,
                "Вероятности должны совпадать (или быть очень близкими) для: " + comment);

        assertEquals(statusApache, statusCustom,
                "MatchStatus должен быть одинаковым для: " + comment);
    }
}