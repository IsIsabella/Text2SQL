package org.texttosql.common;

import net.jcip.annotations.NotThreadSafe;

import java.util.Map;

/**
 * Класс для хранения конфигурации роли, включая зашифрованные колонки и маппинг таблиц
 */
@NotThreadSafe
public class RoleConfiguration {
    /**
     * Имя роли
     */
    public String roleName;
    /**
     * Массив имен зашифрованных колонок
     */
    public String[] encryptedColumns;
    /**
     * Маппинг имен колонок на таблицы
     */
    public Map<String, String> tableMappings;
}