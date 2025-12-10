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

    @Value("${db.roles.admin}")
    private String adminPassword;

    @Value("${db.roles.seller}")
    private String sellerPassword;

    @Value("${db.roles.buyer}")
    private String buyerPassword;

    private ConnectWithDb createConnectWithDb() {
        Map<String, String> dbPasswords = Map.of(
                "admin", adminPassword,
                "seller", sellerPassword,
                "buyer", buyerPassword);
        return new ConnectWithDb(dbPasswords);
    }

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
            ConnectWithDb db = this.createConnectWithDb();
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
    public ResponseEntity<ResultsResponse> executeSql(@RequestBody Map<String, Object> body) {
        String sql = (String) body.get("sql");
        Boolean isGuest = (Boolean) body.get("isGuest");

        try {
            if (sql == null || sql.trim().isEmpty()) {
                throw new RuntimeException("SQL-запрос не может быть пустым");
            }

            ConnectWithDb db = createConnectWithDb();
            ConnectWithDb.DbUserInfo userInfo;

            if (isGuest != null && isGuest) {
                // Если гость, используем фиктивные данные
                userInfo = new ConnectWithDb.DbUserInfo("Гость", "buyer");
            } else {
                // Иначе пытаемся Windows SSO
                userInfo = db.authenticateByWindowsSSO();
            }

            SqlValidator sqlValidator = new SqlValidator();
            sqlValidator.validate(sql);

            ParseQuery parser = new ParseQuery(sql, userInfo.dbUsername(), this.rolesConfigPath);
            String parsedSql = parser.parseSql();

            // Используем пароли из конфигурации, а не из userInfo
            Map<String, String> dbPasswords = Map.of(
                    "admin", adminPassword,
                    "seller", sellerPassword,
                    "buyer", buyerPassword
            );

            // Получаем пароль для роли из конфигурации
            String password = dbPasswords.get(userInfo.role());
            if (password == null) {
                throw new RuntimeException("Пароль для роли '" + userInfo.role() + "' не найден");
            }

            String result = db.results(parsedSql, userInfo.dbUsername(), password);

            return ResponseEntity.ok(ResultsResponse.success(result));

        } catch (Exception e) {
            return ResponseEntity.ok(ResultsResponse.error(e.getMessage()));
        }
    }
}