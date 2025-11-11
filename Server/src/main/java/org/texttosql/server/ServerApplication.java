package org.texttosql.server;

import com.google.errorprone.annotations.Immutable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Главный класс для запуска серверной части приложения на Spring Boot
 */
@SpringBootApplication
@Immutable
public class ServerApplication {
    /**
     * Точка входа для запуска сервера
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
}