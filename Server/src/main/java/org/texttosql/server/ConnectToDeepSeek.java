package org.texttosql.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класс для взаимодействия с Python-скриптом, генерирующим SQL-запросы
 */
public class ConnectToDeepSeek {
    private static final Logger logger = LoggerFactory.getLogger(ConnectToDeepSeek.class);
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/chat";
    private static final Gson gson = new Gson();
    private static final HttpClient client = HttpClient.newHttpClient();

    // Полная схема базы данных
    private static final String SCHEMA = """
        create table Authors(
            AuthorID integer not null primary key check(AuthorID>0),
            AuthorFIO varchar(40) not null
        );
        create table Books(
            Cipher integer not null primary key check(Cipher>0),
            BookName varchar(4000) not null,
            BookTheme varchar(30) not null
            check(BookTheme in('Любовь','Дружба','Смерть','Общественные проблемы','Внутренние противоречия')),
            BookGenre varchar(15) not null
            check(BookGenre in('Роман','Поэма','Рассказ','Пьеса','Эпопея','Драма'))
        );
        create table PublishingHouse(
            PublishingHouseID integer not null primary key
            check(PublishingHouseID>0),
            PublishingHouseName varchar(1000) not null,
            TradeMargin decimal(5,4) default'0' not null check (TradeMargin>0 and TradeMargin<=100)
        );
        create table Circulation(
            CirculationID integer not null primary key check(CirculationID>0),
            DeliveryDate date not null,
            PurchasePrice decimal(18,4) default'1000.0000' not null check(PurchasePrice>0),
            NumReceivedCopies integer not null check(NumReceivedCopies>=0),
            NumUnsoldCopies integer not null check(NumUnsoldCopies>=0),
            BookCipher integer not null check(BookCipher>0),
            IDPublishingHouse int not null check(IDPublishingHouse>0),
            foreign key(BookCipher) references Books(Cipher) on update cascade,
            foreign key(IDPublishingHouse) references PublishingHouse(PublishingHouseID) on update cascade,
            check(NumReceivedCopies>NumUnsoldCopies)
        );
        create table Cheque(
            ChequeID integer not null primary key
            check(ChequeID>0),
            PurchaseDate date not null,
            CashierFIO varchar(40) not null
        );
        create table Wrote(
            IDAuthor integer not null check(IDAuthor>0),
            BookCipher integer not null check(BookCipher>0),
            foreign key(IDAuthor) references Authors(AuthorID) on update cascade,
            foreign key(BookCipher) references Books(Cipher) on update cascade,
            primary key(IDAuthor,BookCipher)
        );
        create table Contains(
            IDCheque int not null check(IDCheque>0),
            IDCirculation int not null check(IDCirculation>0),
            BooksQuantity int not null check(BooksQuantity>0),
            foreign key(IDCheque) references Cheque(ChequeID) on update cascade,
            foreign key(IDCirculation) references Circulation(CirculationID) on update cascade,
            primary key(IDCheque,IDCirculation)
        );
        """;

    public String generateSql(String question, Map<String, Boolean> tableSelected) {
        try {
            logger.info("Starting SQL generation for question: {}", question);
            long startTime = System.currentTimeMillis();

            // Формируем JSON-запрос для ollama с полной схемой
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "hf.co/eliza555beth2002/DeepSeek-R1-Distill-Text2SQL-OneEpoch-GGUF-q4:Q4_K_M");
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", "Prompt: \"" + question + "\"\nContext: " + SCHEMA);
            requestBody.add("messages", gson.toJsonTree(new JsonObject[]{message}));
            requestBody.addProperty("stream", false);

            // Отправляем POST-запрос
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            // Парсим ответ
            JsonObject responseJson = gson.fromJson(responseBody, JsonObject.class);
            String responseText = responseJson.get("message").getAsJsonObject().get("content").getAsString();

            // Извлекаем SQL
            String sqlQuery = extractSqlQuery(responseText);
            logger.info("SQL generation took {} ms", System.currentTimeMillis() - startTime);
            return sqlQuery != null ? sqlQuery : "No SQL query found.";
        } catch (Exception e) {
            logger.error("Error generating SQL: {}", e.getMessage(), e);
            return "Error: " + e.getMessage();
        }
    }

    private String extractSqlQuery(String responseText) {
        try {
            int startIdx = responseText.indexOf("```sql");
            int endIdx = responseText.lastIndexOf("```");
            if (startIdx == -1 || endIdx == -1) {
                return null;
            }
            return responseText.substring(startIdx + 6, endIdx).trim();
        } catch (Exception e) {
            return null;
        }
    }
}