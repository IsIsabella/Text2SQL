package org.texttosql;

import com.ibm.icu.text.Transliterator;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Класс для нормализации данных
 */
@Immutable
public class DataNormalizer {
    /**
     * Паттерн для номера телефона
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("[^0-9+]");
    /**
     * Транслитератор, использует ICU4J для транслитерации по стандарту BGN
     */
    private static final Transliterator CYRILLIC_TO_LATIN = Transliterator.getInstance("Latin-Russian/BGN");

    /**
     * Словарь с сокращенными и полными формами имен
     */
    private static final Map<String, String> NAME_MAPPING = new HashMap<>();
    static {
        NAME_MAPPING.put("саша", "александр");
        NAME_MAPPING.put("дима", "дмитрий");
        NAME_MAPPING.put("леша", "алексей");
        NAME_MAPPING.put("маша", "мария");
        NAME_MAPPING.put("катя", "екатерина");
        NAME_MAPPING.put("женя", "евгений");
        NAME_MAPPING.put("юра", "юрий");
        NAME_MAPPING.put("таня", "татьяна");
        NAME_MAPPING.put("настя", "анастасия");
        NAME_MAPPING.put("ксюша", "ксения");
        NAME_MAPPING.put("оля", "ольга");
        NAME_MAPPING.put("вика", "виктория");
        NAME_MAPPING.put("лиза", "елизавета");
        NAME_MAPPING.put("соня", "софия");
        NAME_MAPPING.put("даша", "дарья");
        NAME_MAPPING.put("миша", "михаил");
        NAME_MAPPING.put("кирилл", "кирилл");
        NAME_MAPPING.put("вова", "владимир");
        NAME_MAPPING.put("света", "светлана");
        NAME_MAPPING.put("наташа", "наталья");
        NAME_MAPPING.put("игорь", "игорь");
        NAME_MAPPING.put("серёжа", "сергей");
        NAME_MAPPING.put("андрей", "андрей");
        NAME_MAPPING.put("рома", "роман");
        NAME_MAPPING.put("паша", "павел");
        NAME_MAPPING.put("витя", "виталий");
        NAME_MAPPING.put("никита", "никита");
        NAME_MAPPING.put("толя", "анатолий");
        NAME_MAPPING.put("валера", "валерий");
        NAME_MAPPING.put("гена", "геннадий");
        NAME_MAPPING.put("костя", "константин");
        NAME_MAPPING.put("федя", "федор");
        NAME_MAPPING.put("стас", "станислав");
        NAME_MAPPING.put("влад", "владислав");
        NAME_MAPPING.put("женя", "евгения"); // женский вариант
        NAME_MAPPING.put("юля", "юлия");
        NAME_MAPPING.put("лена", "елена");
        NAME_MAPPING.put("ира", "ирина");
        NAME_MAPPING.put("люда", "людмила");
        NAME_MAPPING.put("марина", "марина");
        NAME_MAPPING.put("алина", "алина");
        NAME_MAPPING.put("полина", "полина");
        NAME_MAPPING.put("вася", "василий");
        NAME_MAPPING.put("петя", "петр");
        NAME_MAPPING.put("гриша", "григорий");
        NAME_MAPPING.put("артем", "артем");
        NAME_MAPPING.put("даня", "даниил");
        NAME_MAPPING.put("слава", "вячеслав");
        NAME_MAPPING.put("мила", "людмила");
        NAME_MAPPING.put("аня", "анна");
        NAME_MAPPING.put("лиля", "лилия");
        NAME_MAPPING.put("аля", "альбина");
        NAME_MAPPING.put("надя", "надежда");
    }

    /**
     * Полная нормализация данных субъекта
     *
     * @param subject субъект доступа
     * @throws NullPointerException если субъект доступа null
     */
    public static AccessSubject normalize(AccessSubject subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }

        AccessSubject normalizedFIO = DataNormalizer.normalizeFIO(subject);
        String lastName = DataNormalizer.transliterateIfNeeded(normalizedFIO.getLastName());
        String firstName = DataNormalizer.transliterateIfNeeded(normalizedFIO.getFirstName());
        firstName = DataNormalizer.toFullName(firstName);
        String middleName = DataNormalizer.transliterateIfNeeded(normalizedFIO.getMiddleName());
        String email = DataNormalizer.normalizeEmail(subject.getEmail());
        String phone = DataNormalizer.normalizePhone(subject.getPhone());
        String position = DataNormalizer.normalizePosition(subject.getPosition());
        return new AccessSubject(lastName, firstName, middleName, email, phone, position);
    }

    /**
     * Нормализует ФИО: разбивает на компоненты, нормализует каждый,
     * предполагаем формат "Фамилия Имя Отчество" или без отчества
     *
     * @param subject субъект доступа
     * @throws NullPointerException если субъект доступа null
     */
    public static AccessSubject normalizeFIO(AccessSubject subject) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }
        String lastName = DataNormalizer.normalizeString(subject.getLastName());
        String firstName = DataNormalizer.normalizeString(subject.getFirstName());
        String middleName = DataNormalizer.normalizeString(subject.getMiddleName());
        return new AccessSubject(lastName, firstName, middleName,
                subject.getEmail(), subject.getPhone(), subject.getPosition());
    }

    /**
     * Приведение к полной форме
     *
     * @param name имя для приведения
     */
    public static String toFullName(@Nullable String name) {
        if (name == null) {
            return null;
        }
        return NAME_MAPPING.getOrDefault(name, name);
    }

    /**
     * Транслитерирует кириллицу в латиницу, если нужно
     *
     * @param input строка для транслитерации
     */
    public static String transliterateIfNeeded(@Nullable String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return DataNormalizer.CYRILLIC_TO_LATIN.transliterate(input);
    }

    /**
     * Нормализует телефон: удаляет скобки, заменяет 8 на +7, оставляет только цифры и +
     *
     * @param phone номер телефона субъекта доступа
     */
    public static String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        phone = DataNormalizer.PHONE_PATTERN.matcher(phone).replaceAll("");
        if (phone.startsWith("8")) {
            phone = "+7" + phone.substring(1);
        }
        return phone;
    }

    /**
     * Нормализует e-mail: нижний регистр
     *
     * @param email e-mail субъекта доступа
     */
    public static String normalizeEmail(@Nullable String email) {
        if (email == null) {
            return null;
        }
        return email.toLowerCase();
    }

    /**
     * Нормализует должность
     *
     * @param position должность субъекта доступа
     */
    public static String normalizePosition(@Nullable String position) {
        if (position == null) {
            return null;
        }
        String normalized = DataNormalizer.normalizeString(position);
        return PositionSynonyms.getCanonicalPosition(normalized);
    }

    /**
     * Нормализует строку: нижний регистр, замена ё на е, удаление лишних пробелов и пунктуации
     *
     * @param input строка для нормализации
     */
    public static String normalizeString(@Nullable String input) {
        if (input == null) {
            return null;
        }
        return input.toLowerCase()
                .replace('ё', 'е')
                .trim()
                .replaceAll("[^a-zа-я0-9+@.]", "");
    }
}
