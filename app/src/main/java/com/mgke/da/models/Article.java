package com.mgke.da.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Article {
    public String id;
    public String name;
    public String description;
    public String text;
    public String image;
    public Date timestamp;  // Используем тип Date

    public Article() {
    }

    public Article(String id, String name, String description, String text, String image, Date timestamp) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.text = text;
        this.image = image;
        this.timestamp = timestamp;
    }

    // Метод для форматирования даты
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(timestamp);
    }
}
