package com.mgke.da.models;

public class Account {

    public String id;
    public double accountAmount;
    public String accountName;
    public String userId;
    public String currency;
    public String background;

    public Account() {
    }

    // Обновленный конструктор
    public Account(String id, double accountAmount, String accountName, String userId, String currency, String background) {
        this.id = id;
        this.accountAmount = accountAmount;
        this.accountName = accountName;
        this.userId = userId;
        this.currency = currency;
        this.background = background;
    }

}