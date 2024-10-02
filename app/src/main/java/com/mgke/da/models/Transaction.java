package com.mgke.da.models;
import java.util.Date;
public class Transaction{

    public String id;
    public String type;
    public String category;
    public String account;
    public Date date;
    public double amount;
    public String username;


    public Transaction() {
    }
    public Transaction(String id, String type, String category, String account, Date date, double amount) {
        this.id = id;
        this.type = type;
        this.category = category;
        this.account = account;
        this.date = date;
        this.amount = amount;
    }
}


