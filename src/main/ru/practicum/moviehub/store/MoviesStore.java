package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    int idMovie = 0;

    public Movie addMovie(String title, int year) {
        idMovie++;
        movies.put(idMovie, new Movie(idMovie, title, year));
        return movies.get(idMovie);
    }

    public List<Movie> getAllMovies() {
        return new ArrayList<>(movies.values());
    }

    public Optional<Movie> getMovie(int id) {
        if (movies.containsKey(id)) {
            return Optional.of(movies.get(id));
        } else {
            return Optional.empty();
        }
    }

    public List<Movie> getFilteredMovies(Integer year) {
         return movies
                 .values()
                 .stream()
                .filter(movie -> movie.getYear().equals(year))
                .toList();
    }

    public boolean deleteMovie(int id) {
        if (movies.containsKey(id)) {
            movies.remove(id);
            return true;
        } else {
            return false;
        }
    }

    public void clearMovies() {
        movies.clear();
        idMovie = 0;
    }
}