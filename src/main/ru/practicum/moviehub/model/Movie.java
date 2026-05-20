package ru.practicum.moviehub.model;

public class Movie {
    private final int id;
    private final String title;
    private final Integer year;

    public Movie(Integer id, String title, Integer year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getYear() {
        return year;
    }
}