package org.texttosql.server;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.texttosql.common.LoginRequest;
import org.texttosql.common.QueryRequest;
import org.texttosql.common.ResultsResponse;
import org.texttosql.common.SqlResponse;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Value("${roles.config.path}")
    private String rolesConfigPath;

    private final Gson gson = new Gson();

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            ConnectWithDb connect = new ConnectWithDb("localhost",
                    request.getUsername(), request.getPassword(), "bookstore");
            String userExists = connect.checkUser();
            if ("1".equals(userExists)) {
                response.put("success", true);
                response.put("username", request.getUsername());
                return response;
            } else {
                throw new Exception("Пользователь не найден");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }

    @PostMapping("/generate-sql")
    public ResponseEntity<SqlResponse> generateSql(@RequestBody QueryRequest request) {
        try {
            String question = request.getNaturalLanguage();
            ConnectToDeepSeek connect = new ConnectToDeepSeek();
            String sql = connect.generateSql(question, null); // tableSelected не используется
            return ResponseEntity.ok(new SqlResponse(sql));
        } catch (Exception e) {
            return ResponseEntity.ok(new SqlResponse(null, e.getMessage()));
        }
    }

    @PostMapping("/execute-sql")
    public ResponseEntity<ResultsResponse> executeSql(@RequestBody Map<String, String> body) {
        String sql = body.get("sql");
        String username = body.get("username");
        String password = body.get("password");
        try {
            // Добавляем валидацию
            SqlValidator validator = new SqlValidator();
            validator.validate(sql);

            // Продолжаем как раньше
            ParseQuery parseQuery = new ParseQuery(sql, "localhost", username,
                    password, "bookstore", rolesConfigPath);
            String parsedSql = parseQuery.parseSql();
            ConnectWithDb db = new ConnectWithDb("localhost", username,
                    password, "bookstore");
            String results = db.results(parsedSql);
            return ResponseEntity.ok(new ResultsResponse(results));
        } catch (Exception e) {
            return ResponseEntity.ok(new ResultsResponse(null, e.getMessage()));
        }
    }
}