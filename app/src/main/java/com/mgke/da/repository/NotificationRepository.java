package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.models.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NotificationRepository {
    private final CollectionReference notificationCollection;

    public NotificationRepository(FirebaseFirestore db) {
        this.notificationCollection = db.collection("notifications");
    }

    public Notification addNotification(Notification notification) {
        String id = notificationCollection.document().getId();
        notification.id = id;
        notificationCollection.document(id).set(notification);
        return notification;
    }

    public CompletableFuture<List<Notification>> getUserNotifications(String userId) {
        CompletableFuture<List<Notification>> future = new CompletableFuture<>();
        List<Notification> notifications = new ArrayList<>();

        notificationCollection.whereEqualTo("userId", userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                task.getResult().forEach(document -> {
                    Notification notification = document.toObject(Notification.class);
                    notifications.add(notification);
                });
                future.complete(notifications);
            } else {
                future.completeExceptionally(task.getException());
            }
        });

        return future;
    }
}
