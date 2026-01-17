package org.texttosql.server;

import com.google.errorprone.annotations.ThreadSafe;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Связь с локальным Ollama для генерации SQL
 */
@ThreadSafe
public class ConnectToDeepSeek {
    /**
     * Логирования событий и ошибок
     */
    private static final Logger logger = LoggerFactory.getLogger(ConnectToDeepSeek.class);
    /**
     * Взаимодействия с локальным сервером ollama для генерации SQL
     * http://localhost:11434/api/chat
     * http://hv1.seclab.local:11434/api/chat
     *
     */
    private static final String OLLAMA_URL = "http://hv1.seclab.local:11434/api/chat";
    /**
     * Сериализации/десериализации JSON
     */
    private static final Gson gson = new Gson();
    /**
     * Отправки HTTP-запросов к API ollama
     */
    private static final HttpClient client = HttpClient.newHttpClient();
    /**
     * Кэш system-сообщения для модели
     */
    private static final ConcurrentHashMap<String, JsonObject> SYSTEM_MESSAGE_CACHE = new ConcurrentHashMap<>();
    /**
     * Имя модели
     */
    private static final String MODEL_NAME = "hf.co/eliza555beth2002/DeepSeek-R1-Distill-Text2SQL-OneEpoch-GGUF-q4:Q4_K_M";
    /**
     * Полная схема базы данных
     */
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
            CREATE TABLE Circulation (
                CirculationID INTEGER NOT NULL PRIMARY KEY CHECK (CirculationID > 0),
                DeliveryDate DATE NOT NULL,
                PurchasePrice DECIMAL(18,4) DEFAULT 1000.0000 NOT NULL CHECK (PurchasePrice > 0),
                NumReceivedCopies INTEGER NOT NULL CHECK (NumReceivedCopies >= 0),
                NumUnsoldCopies INTEGER NOT NULL CHECK (NumUnsoldCopies >= 0),
                BookCipher INTEGER NOT NULL CHECK (BookCipher > 0),
                IDPublishingHouse INTEGER NOT NULL CHECK (IDPublishingHouse > 0),
                FOREIGN KEY (BookCipher) REFERENCES Books(Cipher) ON UPDATE CASCADE,
                FOREIGN KEY (IDPublishingHouse) REFERENCES PublishingHouse(PublishingHouseID) ON UPDATE CASCADE,
                CHECK (NumReceivedCopies > NumUnsoldCopies)
            );
            CREATE TABLE Cheque (
                ChequeID INTEGER NOT NULL PRIMARY KEY CHECK (ChequeID > 0),
                PurchaseDate DATE NOT NULL,
                CashierFIO TEXT NOT NULL
            );
            CREATE TABLE Wrote (
                IDAuthor INTEGER NOT NULL CHECK (IDAuthor > 0),
                BookCipher INTEGER NOT NULL CHECK (BookCipher > 0),
                FOREIGN KEY (IDAuthor) REFERENCES Authors(AuthorID) ON UPDATE CASCADE,
                FOREIGN KEY (BookCipher) REFERENCES Books(Cipher) ON UPDATE CASCADE,
                PRIMARY KEY (IDAuthor, BookCipher)
            );
            CREATE TABLE Contains (
                IDCheque INTEGER NOT NULL CHECK (IDCheque > 0),
                IDCirculation INTEGER NOT NULL CHECK (IDCirculation > 0),
                BooksQuantity INTEGER NOT NULL CHECK (BooksQuantity > 0),
                FOREIGN KEY (IDCheque) REFERENCES Cheque(ChequeID) ON UPDATE CASCADE,
                FOREIGN KEY (IDCirculation) REFERENCES Circulation(CirculationID) ON UPDATE CASCADE,
                PRIMARY KEY (IDCheque, IDCirculation)
            );
            """;


    /**
     * Создаём system-сообщение один раз
     *
     * @return system-сообщение
     */
    private static JsonObject getSystemMessage() {
        return ConnectToDeepSeek.SYSTEM_MESSAGE_CACHE.computeIfAbsent(ConnectToDeepSeek.MODEL_NAME, model -> {
            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content",
                    "Ты — модель Text-to-SQL. Используй следующую схему базы данных:\n\n" +
                            ConnectToDeepSeek.SCHEMA + "\n\n" +
                            "Связи таблиц в БД:\n" +
                            "Authors (Автор) — содержит сведения об авторах.\n" +
                            "Связана с Books через таблицу Wrote (многие-ко-многим).\n" +
                            "Books (Книги) — хранит данные о книгах.\n" +
                            "Связана с Authors через Wrote и с Circulation (один-ко-многим: одна книга может иметь несколько тиражей).\n" +
                            "PublishingHouse (Издательство) — содержит данные об издательствах.\n" +
                            "Связана с Circulation (один-ко-многим: одно издательство выпускает много тиражей).\n" +
                            "Circulation (Тираж) — связывает книги и издательства, хранит информацию о поставках.\n" +
                            "Связана с Contains (один-ко-многим: один тираж может быть продан в нескольких чеках).\n" +
                            "Cheque (Чек) — отражает продажи книг.\n" +
                            "Связана с Contains (один чек может содержать несколько тиражей книг).\n" +
                            "Wrote (Авторство) — таблица связи «многие-ко-многим» между Authors и Books.\n" +
                            "Contains (Содержимое чека) — таблица связи «многие-ко-многим» между Cheque и Circulation." +
                            "Будь внимательна к названиям колонок и ограничениям. Не добавляй лишнюю информацию в запрос," +
                            "выводи строго то, что требуется. Это важно, потому что этот запрос сразу же выполняется в БД, и " +
                            "некорректная формулировка или лишняя информация могут привести к " +
                            "необратимым последствиям.\""
            );
            return system;
        });
    }

    /**
     * Генерирует SQL-запрос из текста на естественном языке, используя API ollama
     *
     * @param question запрос на естественном языке
     * @return SQL-запрос
     * @throws Exception если произошла ошибка при генерации
     */
    public String generateSql(String question) {
        try {
            ConnectToDeepSeek.logger.info("Генерация SQL для: {}", question);

            JsonObject requestBody = ConnectToDeepSeek.getJsonObject(question);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ConnectToDeepSeek.OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).join();
            JsonObject json = ConnectToDeepSeek.gson.fromJson(response.body(), JsonObject.class);

            String content = json.getAsJsonObject("message").get("content").getAsString();
            String sql = extractSql(content);

            return sql != null ? sql : "SQL не найден в ответе модели.";

        } catch (Exception e) {
            ConnectToDeepSeek.logger.error("Ошибка генерации SQL", e);
            return "Ошибка: " + e.getMessage();
        }
    }

    /**
     * Создает тело запроса
     *
     * @param question запрос на естественном языке
     * @return тело запроса
     */
    private static JsonObject getJsonObject(String question) {
        JsonObject systemMessage = ConnectToDeepSeek.getSystemMessage();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", question);

        JsonArray messages = new JsonArray();
        messages.add(systemMessage);
        messages.add(userMessage);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", ConnectToDeepSeek.MODEL_NAME);
        requestBody.add("messages", messages);
        requestBody.addProperty("stream", false);
        return requestBody;
    }

    /**
     * Извлекает SQL-запрос
     *
     * @param text ответ модели
     * @return SQL-запрос
     */
    private String extractSql(String text) {
        try {
            int start = text.indexOf("```sql");
            int end = text.lastIndexOf("```");
            if (start == -1 || end == -1) return null;
            return text.substring(start + 6, end).trim();
        } catch (Exception e) {
            return null;
        }
    }
}