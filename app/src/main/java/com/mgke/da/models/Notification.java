package com.mgke.da.models;

import java.io.Serializable;
import java.util.Date;

public class Notification implements Serializable {
    public String id;
    public String title;
    public double amount;
    public Date dateTime;
    public String repeatType;
    public String repeatInterval;
    public String userId;

    public Notification() {
    }

    public Notification(String id, String title, double amount, Date dateTime, String repeatType, String repeatInterval, String userId) {
        this.id = id;
        this.title = title;
        this.amount = amount;
        this.dateTime = dateTime;
        this.repeatType = repeatType;
        this.repeatInterval = repeatInterval;
        this.userId = userId;
    }
}
