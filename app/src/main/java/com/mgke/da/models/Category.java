package com.mgke.da.models;

public class Category {
    public String id;
    public String categoryNameRu;
    public String categoryNameEn;
    public int categoryImage;
    public int categoryColor;
    public String type;
    public String userId;
    public boolean isDefault;
    public String name;

    public Category() {
    }

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
        return name;
    }
    public String getNameLan(String language) {
        if (categoryNameRu != null && !categoryNameRu.isEmpty() && "ru".equals(language)) {
            return categoryNameRu;
        } else if (categoryNameEn != null && !categoryNameEn.isEmpty()) {
            return categoryNameEn;
        } else {
            return name;
        }
    }
}