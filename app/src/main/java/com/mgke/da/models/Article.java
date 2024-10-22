package com.mgke.da.models;

import java.time.LocalDateTime;
import java.util.Date;

public class Article {
    public String id;
    public String name;
    public String description;
    public String text;
    public String image;
    public Date timestamp;

    public Article() {
    }

    public Article(String id, String name,String description, String text, String image, Date timestamp) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.text = text;
        this.image = image;
        this.timestamp = timestamp;
    }
}