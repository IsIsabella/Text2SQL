package org.texttosql;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс для расчета редкости фамилии
 */
@Immutable
public class SurnameRarity {
    /**
     * Частотный словарь фамилий
     * ToDO: решить каким образом будет храниться этот словарь, и откуда будет браться эта информация
     */
    private static final Map<String, Double> SURNAME_FREQUENCY = new HashMap<>();

    static {
        // Примеры частотностей
        SurnameRarity.SURNAME_FREQUENCY.put("иванов", 1.0);
        SurnameRarity.SURNAME_FREQUENCY.put("смирнов", 0.8);
        SurnameRarity.SURNAME_FREQUENCY.put("кузнецов", 0.5);
        SurnameRarity.SURNAME_FREQUENCY.put("попов", 0.4);
        SurnameRarity.SURNAME_FREQUENCY.put("васильев", 0.3);
        SurnameRarity.SURNAME_FREQUENCY.put("чацкий", 0.0001);
        SurnameRarity.SURNAME_FREQUENCY.put("крузо", 0.0001);
    }

    /**
     * Возвращает множитель редкости на основе частоты
     * >=1% -> 0.5
     * 0.1-1% -> 1
     * <=0.1% -> 1.5
     *
     *  @param lastName фамилия субъекта доступа
     */
    public static double getRarityMultiplier(@Nullable String lastName) {
        if (lastName == null) {
            return 1.0;
        }
        Double freq = SurnameRarity.SURNAME_FREQUENCY.getOrDefault(lastName.toLowerCase(), 0.01);
        if (freq >= 1.0){
            return 0.5;
        }
        if (freq <= 0.1){
            return 1.5;
        }
        else{
            return 1.0;
        }
    }
}
