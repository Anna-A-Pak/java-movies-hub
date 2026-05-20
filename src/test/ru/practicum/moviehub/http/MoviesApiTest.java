package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore = new MoviesStore();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(/*new MoviesStore()*/moviesStore, 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.clearMovies();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop(); }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        URI uri = URI.create(BASE + "/movies");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsArray() throws Exception {
        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);

        URI uri = URI.create(BASE + "/movies");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("[{\"id\":1,\"title\":\"Константин: Повелитель тьмы\"," +
                "\"year\":2005},{\"id\":2,\"title\":\"Молчание ягнят\",\"year\":1991}]", body);
    }

    @Test
    void postMovie_withCorrectTitleAndCorrectYear_returnsMovie() throws Exception {

        String json = "{\"title\":\"Константин: Повелитель тьмы\",\"year\":2005}";

        URI uri = URI.create(BASE + "/movies");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .headers("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"id\":1,\"title\":\"Константин: Повелитель тьмы\",\"year\":2005}", body);
    }

    @Test
    void postMovie_withTitleIsEmptyAndCorrectYear_returnsError() throws Exception {

        String json = "{\"title\":\"\",\"year\":2005}";

        URI uri = URI.create(BASE + "/movies");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .headers("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Ошибка валидации\",\"details\":[\"название не должно быть пустым\"]}", body);
    }

    @Test
    void postMovie_withTitleLengthMore100AndCorrectYear_returnsError() throws Exception {
        String name = "Жизнь, необыкновенные и удивительные приключения Робинзона Крузо," +
                "моряка из Йо́рка, прожившего 28 лет в полном одиночестве на необитаемом острове" +
                "у берегов Америки близ устьев реки Орино́ко, куда он был выброшен кораблекрушением," +
                "во время которого весь экипаж корабля, кроме него, погиб;" +
                "с изложением его неожиданного освобождения пиратами, написанные им самим";

        String json = "{\"title\": \"" + name + "\",\"year\":2005}";

        URI uri = URI.create(BASE + "/movies");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .headers("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Ошибка валидации\"," +
                "\"details\":[\"длина названия не должна превышать 100 символов\"]}", body);
    }

    @Test
    void postMovie_withCorrectTitleAndWrongYear_returnsError() throws Exception {

        String json = "{\"title\":\"Константин: Повелитель тьмы\",\"year\":1880}";

        URI uri = URI.create(BASE + "/movies");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .headers("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Ошибка валидации\"," +
                "\"details\":[\"год должен быть между 1888 и 2027\"]}", body);
    }

    @Test
    void postMovie_withCorrectTitleAndCorrectYearAndWrongContentType_returnsError() throws Exception {

        String json = "{\"title\":\"Константин: Повелитель тьмы\",\"year\":2005}";

        URI uri = URI.create(BASE + "/movies");
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Сервер принимает данные только в формате json\"}", body);
    }

    @Test
    void getMovieID_withCorrectId_returnsMovie() throws Exception {

        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);
        int movieId = 2;

        URI uri = URI.create(BASE + "/movies" + "/" + movieId);
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"id\":2,\"title\":\"Молчание ягнят\",\"year\":1991}", body);
    }

    @Test
    void getMovieID_returnsNotFound() throws Exception {

        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);
        int movieId = 3;

        URI uri = URI.create(BASE + "/movies" + "/" + movieId);
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies/{id} должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Фильм не найден\"}", body);
    }

    @Test
    void getMovieID_withWrongId_returnsError() throws Exception {

        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);
        String movieId = "abc";

        URI uri = URI.create(BASE + "/movies" + "/" + movieId);
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies/{id} должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Некорректный ID\"}", body);
    }

    @Test
    void deleteMovieID_withCorrectId() throws Exception {

        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);
        int movieId = 2;

        URI uri = URI.create(BASE + "/movies" + "/" + movieId);
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("", body);
    }

    @Test
    void deleteMovieID_returnsNotFound() throws Exception {

        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);
        int movieId = 3;

        URI uri = URI.create(BASE + "/movies" + "/" + movieId);
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "DELETE /movies/{id} должен вернуть 404");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Фильм не найден\"}", body);
    }

    @Test
    void deleteMovieID_withWrongId_returnsError() throws Exception {

        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);
        String movieId = "abc";

        URI uri = URI.create(BASE + "/movies" + "/" + movieId);
        HttpRequest req = HttpRequest.newBuilder()
                .DELETE()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "DELETE /movies/{id} должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Некорректный ID\"}", body);
    }

    @Test
    void getMoviesYear_withCorrectYear_returnsMovies() throws Exception {

        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);
        moviesStore.addMovie("Гарри Поттер и Кубок огня", 2005);
        int year = 2005;

        URI uri = URI.create(BASE + "/movies?year=" + year);
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("[{\"id\":1,\"title\":\"Константин: Повелитель тьмы\"," +
                "\"year\":2005},{\"id\":3,\"title\":\"Гарри Поттер и Кубок огня\",\"year\":2005}]", body);
    }

    @Test
    void getMoviesYear_withCorrectYear_returnsEmptyArray() throws Exception {

        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);
        moviesStore.addMovie("Гарри Поттер и Кубок огня", 2005);
        int year = 2006;

        URI uri = URI.create(BASE + "/movies?year=" + year);
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMoviesYear_withWrongYear_returnsError() throws Exception {

        moviesStore.addMovie("Константин: Повелитель тьмы", 2005);
        moviesStore.addMovie("Молчание ягнят", 1991);
        moviesStore.addMovie("Гарри Поттер и Кубок огня", 2005);
        String year = "abcd";

        URI uri = URI.create(BASE + "/movies?year=" + year);
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .headers("Content-Type", "application/xml; charset=UTF-8")
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies?year=YYYY должен вернуть 400");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertEquals("{\"error\":\"Некорректный параметр запроса — \\u0027year\\u0027\"}", body);
    }
}