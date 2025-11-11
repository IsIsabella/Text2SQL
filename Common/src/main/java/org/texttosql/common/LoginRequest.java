package org.texttosql.common;

import net.jcip.annotations.NotThreadSafe;

/**
 * Класс для передачи данных аутентификации от клиента к серверу
 */
@NotThreadSafe
public class LoginRequest {
    /**
     * Имя пользователя
     */
    private String username;
    /**
     * Пароль пользователя
     */
    private String password;

    /**
     * Конструктор по умолчанию
     */
    public LoginRequest() {
    }

    /**
     * Конструктор с параметрами
     *
     * @param username имя пользователя
     * @param password пароль пользователя
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Получает имя пользователя
     *
     * @return имя пользователя
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Устанавливает имя пользователя
     *
     * @param username имя пользователя
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Получает пароль пользователя
     *
     * @return пароль пользователя
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Устанавливает пароль пользователя
     *
     * @param password пароль пользователя
     */
    public void setPassword(String password) {
        this.password = password;
    }
}