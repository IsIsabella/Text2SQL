package org.texttosql.common;

import net.jcip.annotations.NotThreadSafe;

import java.util.List;

/**
 * Класс для десериализации конфигурации ролей из JSON-файла
 */
@NotThreadSafe
public class Configuration {
    /**
     * Список конфигураций ролей
     */
    public List<RoleConfiguration> roles;
}