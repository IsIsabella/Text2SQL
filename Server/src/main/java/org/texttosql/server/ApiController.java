package org.texttosql.server;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.texttosql.common.LoginRequest;
import org.texttosql.common.QueryRequest;
import org.texttosql.common.ResultsResponse;
import org.texttosql.common.SqlResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * REST-контроллер для обработки запросов к API,
 * обрабатывает: вход, генерацию SQL, выполнение SQL
 */
@RestController
@RequestMapping("/api")
@ThreadSafe
public class ApiController {

    /**
     * Путь к конфигу ролей (JSON)
     */
    @Value("${roles.config.path}")
    private String rolesConfigPath;

    /**
     * Для работы с JSON
     */
    private final Gson gson = new Gson();

    /**
     * Обрабатывает вход пользователя
     *
     * @param request логин и пароль
     * @return успех или ошибка
     * @throws Exception если произошла ошибка при аутентификации
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Подключаемся к БД для проверки пользователя
            ConnectWithDb authDb = new ConnectWithDb("localhost", request.getUsername(),
                    request.getPassword(), "bookstore_secure");

            ConnectWithDb.DbUserInfo userInfo = authDb.authenticateUser();

            if (userInfo == null) {
                throw new RuntimeException("Неверный логин или пароль");
            }

            // Успешный вход
            response.put("success", true);
            response.put("username", userInfo.clientUsername());
            response.put("role", userInfo.role());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // Ошибка входа
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Генерирует SQL из текста на естественном языке
     *
     * @param request запрос с текстом
     * @return сгенерированный SQL или ошибка
     * @throws Exception если произошла ошибка при генерации
     */
    @PostMapping("/generate-sql")
    public ResponseEntity<SqlResponse> generateSql(@RequestBody QueryRequest request) {
        try {
            String question = request.getNaturalLanguage();
            if (question == null || question.trim().isEmpty()) {
                throw new RuntimeException("Запрос не может быть пустым");
            }

            ConnectToDeepSeek connect = new ConnectToDeepSeek();
            String sql = connect.generateSql(question);

            return ResponseEntity.ok(new SqlResponse(sql));

        } catch (Exception e) {
            return ResponseEntity.ok(new SqlResponse(null, e.getMessage()));
        }
    }

    /**
     * Выполняет SQL от имени авторизованного пользователя
     *
     * @param body sql, username, password
     * @return результаты или ошибка
     * @throws Exception если произошла ошибка при выполнении запроса
     */
    @PostMapping("/execute-sql")
    public ResponseEntity<ResultsResponse> executeSql(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        String clientUser = body.get("username");
        String clientPass = body.get("password");

        try {
            if (sql == null || sql.trim().isEmpty()) {
                throw new RuntimeException("SQL-запрос не может быть пустым");
            }

            // Авторизация
            ConnectWithDb auth = new ConnectWithDb("localhost",
                    clientUser, clientPass, "bookstore_secure");
            ConnectWithDb.DbUserInfo userInfo = auth.authenticateUser();
            if (userInfo == null) {
                return ResponseEntity.ok(ResultsResponse.error("Неверный логин или пароль"));
            }

            // Проверка SQL
            new SqlValidator().validate(sql);

            // Парсинг (расшифровка колонок)
            ParseQuery parser = new ParseQuery(sql, userInfo.dbUsername(), this.rolesConfigPath);
            String parsedSql = parser.parseSql();

            // Выполнение
            String result = auth.results(parsedSql, userInfo.dbUsername(), userInfo.dbPassword());

            return ResponseEntity.ok(ResultsResponse.success(result));

        } catch (Exception e) {
            return ResponseEntity.ok(ResultsResponse.error(e.getMessage()));
        }
    }
}