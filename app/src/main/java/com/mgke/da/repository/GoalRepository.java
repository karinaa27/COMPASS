package com.mgke.da.repository;

<<<<<<< HEAD
import com.google.android.gms.tasks.Task;
=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Goal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GoalRepository {
    private CollectionReference goalCollection;

    public GoalRepository(FirebaseFirestore db) {
        goalCollection = db.collection("goal");
    }

    public Goal addGoal(Goal goal) {
        String id = goalCollection.document().getId();
        goal.id = id;
        goalCollection.document(id).set(goal);
        return goal;
    }

    public void deleteGoal(String id) {
        goalCollection.document(id).delete();
    }

<<<<<<< HEAD
    public Task<Void> updateGoal(Goal goal) {
        return goalCollection.document(goal.id).set(goal);
=======
    public void updateGoal(Goal goal) {
       goalCollection.document(goal.id).set(goal);
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    }

    public CompletableFuture<List<Goal>> getAllGoal() {
        CompletableFuture<List<Goal>> future = new CompletableFuture<>();
        List<Goal> goals = new ArrayList<>();

       goalCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                   Goal goal = document.toObject(Goal.class);
                   goals.add(goal);
                }
                future.complete(goals);
            }
        });
        return future;
    }
<<<<<<< HEAD
    public CompletableFuture<Goal> getGoalById(String id) {
        CompletableFuture<Goal> future = new CompletableFuture<>();
        goalCollection.document(id).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Goal goal = task.getResult().toObject(Goal.class);
                future.complete(goal);
            } else {
                future.complete(null);
            }
        });
        return future;
    }
=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
}
