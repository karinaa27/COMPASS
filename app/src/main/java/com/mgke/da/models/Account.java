package com.mgke.da.models;

import java.io.Serializable;

public class Account implements Serializable {

    public String id;
    public double accountAmount;
    public String accountName;
    public String userId;
    public String currency;
    public String background;
    private boolean dataLoaded = false;

    public Account() {
    }

    public Account(String id, double accountAmount, String accountName, String userId, String currency, String background) {
        this.id = id;
        this.accountAmount = accountAmount;
        this.accountName = accountName;
        this.userId = userId;
        this.currency = currency;
        this.background = background;
    }
    public boolean isDataLoaded() {
        return dataLoaded;
    }
    public void setDataLoaded(boolean loaded) {
        this.dataLoaded = loaded;
    }
}
