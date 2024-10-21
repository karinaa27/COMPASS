package com.mgke.da.models;

import java.io.Serializable;
import java.util.Date;


public class Goal implements Serializable {
    // Ваши поля
    public String id;
    public String goalName;
    public double targetAmount;
    public double progress;
    public String userId;
    public Date dateEnd;
    public boolean isCompleted;
    public String note;
    public String currency; // Поле для валюты

    public Goal() {
    }

    public Goal(String id, String goalName, double targetAmount, double progress, String userId, Date dateEnd, boolean isCompleted, String note, String currency) {
        this.id = id;
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.progress = progress;
        this.userId = userId;
        this.dateEnd = dateEnd;
        this.isCompleted = isCompleted;
        this.note = note;
        this.currency = currency; // Инициализация поля для валюты
    }
}
