package org.texttosql;

/**
 * Пример использования.
 */
public class Main {

    public static void main(String[] args) {
        AccessSubject subj1 = new AccessSubject("Smirnov", "Dima", null,
                "dima@company.ru", "8(123)456-78-90", "manager");
        AccessSubject subj2 = new AccessSubject("Смирнов", "Дмитрий", "Олегович",
                "Dima@company.ru", "+71234567890", "менеджер");
        double prob = EntityResolver.calculateMatchProbabilityCustom(subj1, subj2);
        System.out.println("1. Вероятность: " + prob + ", Статус: " + EntityResolver.getMatchStatus(prob));

        AccessSubject subj3 = new AccessSubject("Ivanov", "Ivan", "Ivanovich",
                "ivan@example.com", "+71234567890", "director");
        AccessSubject subj4 = new AccessSubject("Иванов", "Иван", "Иванович",
                "ivan@exampel.com", "8(123)456-78-90", "директор");
        double prob1 = EntityResolver.calculateMatchProbabilityCustom(subj3, subj4);
        System.out.println("2. Вероятность: " + prob1 + ", Статус: " + EntityResolver.getMatchStatus(prob1));

        AccessSubject subj7 = new AccessSubject("Smirnov", "Dima", null,
                "dima@company.ru", null, "");
        AccessSubject subj8 = new AccessSubject("Petrov", "Alex", null,
                "alex@other.com", null, "");
        double prob3 = EntityResolver.calculateMatchProbabilityCustom(subj7, subj8);
        System.out.println("3. Вероятность: " + prob3 + ", Статус: " + EntityResolver.getMatchStatus(prob3));

        AccessSubject subj9 = new AccessSubject("Smirnov", "Dmitry", "Olegovich",
                "dmitry@company.ru", "+71234567890", "manager");
        AccessSubject subj10 = new AccessSubject("Smirnoff", "Дмитрий", "Олегович",
                "dmitry56544@yandex.ru", "81277986543", "менеджер");
        double prob4 = EntityResolver.calculateMatchProbabilityCustom(subj9, subj10);
        System.out.println("4. Вероятность: " + prob4 + ", Статус: " + EntityResolver.getMatchStatus(prob4));

        AccessSubject subj11 = new AccessSubject("Ivanov", "Ivan", "Ivanovich",
                "ivan@example.com", "+71234567890", "director");
        AccessSubject subj12 = new AccessSubject("Иванов", "Иван", "Petrovich",
                "ivanpetrov@example.com", "+71874567890", "директор");
        double prob5 = EntityResolver.calculateMatchProbabilityCustom(subj11, subj12);
        System.out.println("5. Вероятность: " + prob5 + ", Статус: " + EntityResolver.getMatchStatus(prob5));

        AccessSubject subj13 = new AccessSubject("Kruzo", "Alexey", "Sergeevich",
                null, "+79876543210", null);
        AccessSubject subj14 = new AccessSubject("Крузо", "Алексей", "Сергеевич",
                null, "89876543210", null);
        double prob6 = EntityResolver.calculateMatchProbabilityCustom(subj13, subj14);
        System.out.println("6. Вероятность: " + prob6 + ", Статус: " + EntityResolver.getMatchStatus(prob6));

        AccessSubject subj15 = new AccessSubject("Чацкий", "Vladimir", null,
                "vlad@rare.com", null, "programmer");
        AccessSubject subj16 = new AccessSubject("Чацкий", "Vlademir", "Vladimirovich",
                null, null, "программист");
        double prob7 = EntityResolver.calculateMatchProbabilityCustom(subj15, subj16);
        System.out.println("7. Вероятность: " + prob7 + ", Статус: " + EntityResolver.getMatchStatus(prob7));

        AccessSubject subj19 = new AccessSubject("Vasiliev", "Sasha", null,
                "sasha@vasiliev.ru", "81234567890", "doctor");
        AccessSubject subj20 = new AccessSubject("Васильев", "Александр", null,
                "sasha@vasiliev.ru", "+71234567890", "врач");
        double prob9 = EntityResolver.calculateMatchProbabilityCustom(subj19, subj20);
        System.out.println("8. Вероятность: " + prob9 + ", Статус: " + EntityResolver.getMatchStatus(prob9));

        AccessSubject subj23 = new AccessSubject("Sidorov", "Nikolay", "Nikolaevich",
                "nik@sid.ru", "+71234567890", "manager");
        AccessSubject subj24 = new AccessSubject("Сидоров", "Николай", "Николаевич",
                "other@sid.ru", "+71234567890", "менеджер");
        double prob11 = EntityResolver.calculateMatchProbabilityCustom(subj23, subj24);
        System.out.println("9. Вероятность: " + prob11 + ", Статус: " + EntityResolver.getMatchStatus(prob11));

        AccessSubject subj25 = new AccessSubject("Novikov", "Andrey", null,
                null, null, null);
        AccessSubject subj26 = new AccessSubject("Новиков", "Андрей", null,
                null, null, "программист");
        double prob12 = EntityResolver.calculateMatchProbabilityCustom(subj25, subj26);
        System.out.println("10. Вероятность: " + prob12 + ", Статус: " + EntityResolver.getMatchStatus(prob12));

        AccessSubject s27 = new AccessSubject("Smirnov", "Лёша", null,
                "lesha@company.ru", "+71234567890", "manager");
        AccessSubject s28 = new AccessSubject("Смирнов", "Алексей", "Петрович",
                "lesha@company.ru", "8(123)456-78-90", "менеджер");

        double prob14 = EntityResolver.calculateMatchProbabilityCustom(s27, s28);
        System.out.println("11. Вероятность: " + prob14 + ", Статус: " + EntityResolver.getMatchStatus(prob14));

        AccessSubject s29 = new AccessSubject("Андреева", "Надежда", "Валентиновна",
                "n.andreeva@bookstore.ru", "+71234567890", "администратор");
        AccessSubject s30 = new AccessSubject("Смирнов", "Алексей", "Петрович",
                "lesha@company.ru", "8(123)456-78-90", "менеджер");

        double prob15 = EntityResolver.calculateMatchProbabilityCustom(s29, s30);
        System.out.println("11. Вероятность: " + prob15 + ", Статус: " + EntityResolver.getMatchStatus(prob15));
    }
}
