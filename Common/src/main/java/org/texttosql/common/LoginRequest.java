package org.texttosql.common;

/**
 * Класс для передачи данных аутентификации от клиента к серверу
 */
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
     * @param username Имя пользователя
     * @param password Пароль пользователя
     */
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Получает имя пользователя
     *
     * @return Имя пользователя
     */
    public String getUsername() {
        return username;
    }

    /**
     * Устанавливает имя пользователя
     *
     * @param username Имя пользователя
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Получает пароль пользователя
     *
     * @return Пароль пользователя
     */
    public String getPassword() {
        return password;
    }

    /**
     * Устанавливает пароль пользователя
     *
     * @param password Пароль пользователя
     */
    public void setPassword(String password) {
        this.password = password;
    }
}