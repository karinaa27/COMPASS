package com.mgke.da.models;

public class Account{

    public String id;
    public double accountAmount;
    public String accountName;
    public String username;

    public Account() {
    }

    public Account(String id, double accountAmount, String accountName, String username) {
        this.id = id;
        this.accountAmount = accountAmount;
        this.accountName = accountName;
        this.username = username;
    }
}
