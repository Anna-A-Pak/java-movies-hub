package ru.practicum.moviehub.handler;

import com.google.gson.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore moviesStore;
    private static final int INDEX_PARAM = 2;
    private static final Integer MIN_YEAR = 1888;
    private static final Integer MAX_YEAR = LocalDate.now().getYear() + 1;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            switch (method.toUpperCase()) {
                case "GET":
                    getMethod(ex);
                    break;
                case "POST":
                    postMethod(ex);
                    break;
                case "DELETE":
                    deleteMethod(ex);
                    break;
                default:
                    ErrorResponse errorResponse = new ErrorResponse("Метод не поддерживается");
                    sendError(ex, errorResponse, 405);
            }
        } catch (Error error) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка");
            sendError(ex, errorResponse, 500);
        }
    }

    public void getMethod(HttpExchange ex) throws IOException {
        int param = getParam(ex);
        int filter = getValueFilter(ex);
        if (param >= 0) {
            Optional<Movie> optMovie = moviesStore.getMovie(param);
            if (optMovie.isPresent()) {
                sendJson(ex, 200, gson.toJson(optMovie.get()));
            } else {
                ErrorResponse errorResponse = new ErrorResponse("Фильм не найден");
                sendError(ex, errorResponse, 404);
            }
        } else if (filter > 0) {
           sendJson(ex, 200, gson.toJson(moviesStore.getFilteredMovies(filter)));
        } else {
            sendJson(ex, 200, gson.toJson(moviesStore.getAllMovies()));
        }
    }

    public void postMethod(HttpExchange ex) throws IOException {
        List<String> details = new ArrayList<>();
        Headers requestHeaders = ex.getRequestHeaders();
        InputStream inputStream = ex.getRequestBody();
        String jsonString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        List<String> contentTypeValues = requestHeaders.get("Content-type");
        if ((contentTypeValues == null) || ((!contentTypeValues.contains("application/json")) &&
                (!contentTypeValues.contains("application/json; charset=UTF-8")))) {
            ErrorResponse errorResponse = new ErrorResponse("Сервер принимает данные только в формате json");
            sendError(ex, errorResponse, 415);
            return;
        }

        if (jsonString.isBlank()) {
            ErrorResponse errorResponse = new ErrorResponse("Тело запроса не должно быть пустым");
            sendError(ex, errorResponse, 400);
            return;
        }

        try {
            JsonElement jsonElement = JsonParser.parseString(jsonString);

            if (jsonElement.isJsonObject()) {
                Movie movieFromRequest = gson.fromJson(jsonString, Movie.class);

                if (movieFromRequest.getTitle() == null) {
                    details.add("название является обязательным полем");
                } else if (movieFromRequest.getTitle().isBlank()) {
                    details.add("название не должно быть пустым");
                } else if (movieFromRequest.getTitle().length() > 100) {
                    details.add("длина названия не должна превышать 100 символов");
                }

                if (movieFromRequest.getYear() == null) {
                    details.add("год является обязательным полем");
                } else if (movieFromRequest.getYear() < MIN_YEAR || movieFromRequest.getYear() > MAX_YEAR) {
                    details.add("год должен быть между " + MIN_YEAR + " и " + MAX_YEAR);
                }

                if (details.isEmpty()) {
                    Movie movie = moviesStore.addMovie(movieFromRequest.getTitle(), movieFromRequest.getYear());
                    sendJson(ex, 201, gson.toJson(movie));
                } else {
                    ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", details);
                    sendError(ex, errorResponse, 422);
                }
            } else {
                ErrorResponse errorResponse = new ErrorResponse("Некорректный запрос");
                sendError(ex, errorResponse, 400);
            }
        } catch (JsonSyntaxException e) {
            ErrorResponse errorResponse = new ErrorResponse("Неверный формат JSON");
            sendError(ex, errorResponse, 400);
        }
    }

    private void sendError(HttpExchange ex, ErrorResponse errorResponse, int code) throws IOException {
        sendJson(ex, code, gson.toJson(errorResponse));
    }

    private int getParam(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String[] splitString = path.split("/");
        int param = -1;

        if (splitString.length > INDEX_PARAM) {
            try {
                param = Integer.parseInt(splitString[INDEX_PARAM]);
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse("Некорректный ID");
                sendError(ex, errorResponse, 400);
            }
        }
        return param;
    }

    private int getValueFilter(HttpExchange ex) throws IOException {
        String filter = ex.getRequestURI().toString();
        int valueFilter = -1;

        if (filter.contains("?") && filter.contains("=")) {
            int index = filter.indexOf("=") + 1;
            try {
                valueFilter = Integer.parseInt(filter.substring(index));
                if (valueFilter < MIN_YEAR || valueFilter > MAX_YEAR) {
                    ErrorResponse errorResponse = new ErrorResponse("Некорректный параметр запроса — 'year'");
                    sendError(ex, errorResponse, 400);
                }
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse("Некорректный параметр запроса — 'year'");
                sendError(ex, errorResponse, 400);
            }
        }
        return valueFilter;
    }

    private void deleteMethod(HttpExchange ex) throws IOException {
        int param = getParam(ex);
        if (param >= 0) {
            if (moviesStore.deleteMovie(param)) {
                sendNoContent(ex);
            } else {
                ErrorResponse errorResponse = new ErrorResponse("Фильм не найден");
                sendError(ex, errorResponse, 404);
            }
        } else {
            ErrorResponse errorResponse = new ErrorResponse("Не указан ID");
            sendError(ex, errorResponse, 400);
        }
    }
}
