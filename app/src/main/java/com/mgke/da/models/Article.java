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

    public String getFormattedTimestamp() {
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(timestamp);
        } else {
            // Вернуть значение по умолчанию или обработать случай с null
            return "Нет доступной метки времени";
        }
    }

}
