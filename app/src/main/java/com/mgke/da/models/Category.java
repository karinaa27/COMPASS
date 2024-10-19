package com.mgke.da.models;

<<<<<<< HEAD
import java.util.Locale;

public class Category {
    public String id;
    public String categoryNameRu; // Название на русском
    public String categoryNameEn; // Название на английском
    public int categoryImage;
    public int categoryColor;
    public String type;
    public String userId;
    public boolean isDefault;
    public String name;
=======
public class Category {
    public String id;
    public String categoryName;
    public int categoryImage;
    public int categoryColor;
    public String type;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee

    public Category() {
    }

<<<<<<< HEAD
    public Category(String id, String categoryNameRu, String categoryNameEn, int categoryImage, int categoryColor, String type, String userId, boolean isDefault,  String name) {
        this.id = id;
        this.categoryNameRu = categoryNameRu;
        this.categoryNameEn = categoryNameEn;
        this.categoryImage = categoryImage;
        this.categoryColor = categoryColor;
        this.type = type;
        this.userId = userId;
        this.isDefault = isDefault;
        this.name = name;
    }
    public String getName() {
        return name; // Просто возвращаем поле name
    }
    public String getNameLan(String language) {
        if (categoryNameRu != null && !categoryNameRu.isEmpty() && "ru".equals(language)) {
            return categoryNameRu; // Возвращаем русское имя, если оно не null и выбран русский язык
        } else if (categoryNameEn != null && !categoryNameEn.isEmpty()) {
            return categoryNameEn; // Возвращаем английское имя, если оно не null
        } else {
            return name; // Возвращаем поле name, если оба поля null
        }
    }
}
=======
    public Category(String id, String categoryName, int categoryImage, int categoryColor, String type) {
        this.id = id;
        this.categoryName = categoryName;
        this.categoryImage = categoryImage;
        this.categoryColor = categoryColor;
        this.type = type;
    }
}
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
