package com.mgke.da.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Article implements Serializable {
    public String id;
    public String nameRu;
    public String nameEn;
    public String descriptionRu;
    public String descriptionEn;
    public String textRu;
    public String textEn;
    public String image;
    public Date timestamp;

    public Article() {
    }

    public Article(String id, String nameRu, String nameEn, String descriptionRu, String descriptionEn,
                   String textRu, String textEn, String image, Date timestamp) {
        this.id = id;
        this.nameRu = nameRu;
        this.nameEn = nameEn;
        this.descriptionRu = descriptionRu;
        this.descriptionEn = descriptionEn;
        this.textRu = textRu;
        this.textEn = textEn;
        this.image = image;
        this.timestamp = timestamp;
    }

    // Метод для форматирования даты
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(timestamp);
    }
}
