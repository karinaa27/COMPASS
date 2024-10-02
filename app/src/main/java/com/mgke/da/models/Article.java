package com.mgke.da.models;

import java.time.LocalDateTime;

public class Article {
    public String id;
    public String text;
    public String image;
    public LocalDateTime timestamp;

    public Article() {
    }

    public Article(String id, String text, String image, LocalDateTime timestamp) {
        this.id = id;
        this.text = text;
        this.image = image;
        this.timestamp = timestamp;
    }
}