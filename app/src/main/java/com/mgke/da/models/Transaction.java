package com.mgke.da.models;
<<<<<<< HEAD

import java.io.Serializable;
import java.util.Date;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L; // Уникальный идентификатор версии
=======
import java.util.Date;
public class Transaction{
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee

    public String id;
    public String type;
    public String category;
    public String account;
    public Date date;
    public double amount;
<<<<<<< HEAD
    public String userId;
    public String currency;
    public int categoryImage; // Иконка категории
    public int categoryColor; // Цвет категории
    public String accountBackground; // Фон счета

    public Transaction() {
    }

    public Transaction(String id, String type, String category, String account, Date date, double amount, String userId, String currency, String accountBackground) {
=======
    public String username;


    public Transaction() {
    }
    public Transaction(String id, String type, String category, String account, Date date, double amount) {
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
        this.id = id;
        this.type = type;
        this.category = category;
        this.account = account;
        this.date = date;
        this.amount = amount;
<<<<<<< HEAD
        this.userId = userId;
        this.currency = currency;
        this.accountBackground = accountBackground; // Инициализация фона счета
    }

    public double getAmount() {
        return amount;
    }

    public String getType() {
        return type;
    }
}
=======
    }
}


>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
