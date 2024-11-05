package com.mgke.da.models;

import java.util.Date;

public class Comment {
    public String id;
    public String userId; // ID пользователя, который оставил комментарий
    public String userName; // Имя пользователя
    public String userImage; // URL изображения пользователя
    public String text; // Текст комментария
    public Date timestamp; // Дата и время создания комментария

    // Пустой конструктор (нужен для Firebase)
    public Comment() {
    }

    // Обновленный конструктор
    public Comment(String userId, String userName, String userImage, String text, long timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.userImage = userImage;
        this.text = text;
        this.timestamp = new Date(timestamp); // Преобразование long в Date
    }

    // Геттеры и сеттеры
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserImage() {
        return userImage;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
