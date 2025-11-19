package org.texttosql.server;

import com.google.errorprone.annotations.ThreadSafe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.texttosql.common.QueryRequest;
import org.texttosql.common.ResultsResponse;
import org.texttosql.common.SqlResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * REST-контроллер:
 * • /api/login – SSO-вход;
 * • /api/generate-sql – генерация SQL через локальный сервер Ollama;
 * • /api/execute-sql – выполнение запроса
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
     * Обрабатывает вход пользователя
     *
     * @return успех или ошибка
     * @throws Exception если произошла ошибка при аутентификации
     */
    @GetMapping("/login")
    public ResponseEntity<Map<String, Object>> login() {
        Map<String, Object> resp = new HashMap<>();
        try {
            ConnectWithDb db = new ConnectWithDb();
            ConnectWithDb.DbUserInfo info = db.authenticateByWindowsSSO();

            resp.put("success", true);
            resp.put("username", info.clientUsername());
            resp.put("role", info.role());
            if (info.warningMessage() != null) {
                resp.put("warning", info.warningMessage());
            }
            return ResponseEntity.ok(resp);

        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage());
            return ResponseEntity.status(401).body(resp);//401 - клиент не авторизован для доступа к ресурсу
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
        try {
            if (sql == null || sql.trim().isEmpty()) {
                throw new RuntimeException("SQL-запрос не может быть пустым");
            }

            ConnectWithDb db = new ConnectWithDb();
            ConnectWithDb.DbUserInfo userInfo = db.authenticateByWindowsSSO();

            SqlValidator sqlValidator = new SqlValidator();
            sqlValidator.validate(sql);

            ParseQuery parser = new ParseQuery(sql, userInfo.dbUsername(), this.rolesConfigPath);
            String parsedSql = parser.parseSql();

            String result = db.results(parsedSql, userInfo.dbUsername(), userInfo.dbPassword());

            return ResponseEntity.ok(ResultsResponse.success(result));

        } catch (Exception e) {
            return ResponseEntity.ok(ResultsResponse.error(e.getMessage()));
        }
    }
}