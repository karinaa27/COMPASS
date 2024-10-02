package com.mgke.da.models;

public class Category {
    public String id;
    public String categoryName;
    public int categoryImage;
    public int categoryColor;
    public String type;
    public String userId;
    public boolean isDefault;

    public Category() {
    }

    public Category(String id, String categoryName, int categoryImage, int categoryColor, String type, String userId, boolean isDefault) {
        this.id = id;
        this.categoryName = categoryName;
        this.categoryImage = categoryImage;
        this.categoryColor = categoryColor;
        this.type = type;
        this.userId = userId;
        this.isDefault = isDefault;
    }
}
