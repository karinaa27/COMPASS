package com.mgke.da.models;

public class Goal {
    public String id;
    public String goalName;
    public double targetAmount;
    public double progress;
    public Long userId;

    public Goal() {
    }

    public Goal(String id, String goalName, double targetAmount, double progress, Long userId) {
        this.id = id;
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.progress = progress;
        this.userId = userId;
    }
}