package com.mgke.da.models;

import java.util.Date;

public class Goal {
    public String id;
    public String goalName;
    public double targetAmount;
    public double progress;
    public String userId;  // Измените тип на String
    public Date dateEnd;
    public boolean isCompleted;
    public String note;

    public Goal() {
    }

    public Goal(String id, String goalName, double targetAmount, double progress, String userId, Date dateEnd, boolean isCompleted, String note) {
        this.id = id;
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.progress = progress;
        this.userId = userId;  // Идентификатор пользователя
        this.dateEnd = dateEnd;
        this.isCompleted = isCompleted;
        this.note = note;
    }
}