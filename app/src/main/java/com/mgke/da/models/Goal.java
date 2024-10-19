package com.mgke.da.models;

<<<<<<< HEAD
import java.util.Date;

=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
public class Goal {
    public String id;
    public String goalName;
    public double targetAmount;
    public double progress;
<<<<<<< HEAD
    public String userId;  // Измените тип на String
    public Date dateEnd;
    public boolean isCompleted;
    public String note;
=======
    public Long userId;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee

    public Goal() {
    }

<<<<<<< HEAD
    public Goal(String id, String goalName, double targetAmount, double progress, String userId, Date dateEnd, boolean isCompleted, String note) {
=======
    public Goal(String id, String goalName, double targetAmount, double progress, Long userId) {
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
        this.id = id;
        this.goalName = goalName;
        this.targetAmount = targetAmount;
        this.progress = progress;
<<<<<<< HEAD
        this.userId = userId;  // Идентификатор пользователя
        this.dateEnd = dateEnd;
        this.isCompleted = isCompleted;
        this.note = note;
=======
        this.userId = userId;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    }
}