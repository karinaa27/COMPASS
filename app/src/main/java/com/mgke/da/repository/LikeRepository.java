package com.mgke.da.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Like;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LikeRepository {
    private final FirebaseFirestore db;
    private final String userId;

    public LikeRepository(FirebaseFirestore db) {
        this.db = db;
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private CollectionReference getLikesCollection(String articleId) {
        return db.collection("articles").document(articleId).collection("likes");
    }

    public CompletableFuture<Boolean> isLiked(String articleId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        getLikesCollection(articleId).document(userId).get().addOnSuccessListener(documentSnapshot -> {
            future.complete(documentSnapshot.exists());
        }).addOnFailureListener(future::completeExceptionally);
        return future;
    }
    public CompletableFuture<List<String>> getAllLikedArticles() {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        db.collectionGroup("likes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<String> articleIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Предполагается, что ID статьи хранится в документе
                            String articleId = document.getReference().getParent().getParent().getId(); // Получаем ID статьи
                            articleIds.add(articleId);
                        }
                        future.complete(articleIds);
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                });
        return future;
    }

    public CompletableFuture<Void> addLike(String articleId) {
        Like like = new Like(userId);
        return CompletableFuture.runAsync(() -> getLikesCollection(articleId).document(userId).set(like));
    }

    public CompletableFuture<Void> removeAllLikes(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        // Получаем все лайки из коллекции "likes" по каждому артиклю
        db.collection("articles").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<CompletableFuture<Void>> deleteLikesFutures = new ArrayList<>();
                for (QueryDocumentSnapshot articleDoc : task.getResult()) {
                    String articleId = articleDoc.getId();
                    // Проверяем, есть ли лайк для данного пользователя
                    getLikesCollection(articleId).document(userId).get().addOnCompleteListener(likeTask -> {
                        if (likeTask.isSuccessful() && likeTask.getResult().exists()) {
                            // Если лайк существует, добавляем задачу на удаление лайка
                            deleteLikesFutures.add(removeLike(articleId));
                        }

                        // Проверяем, завершены ли все удаления лайков
                        if (deleteLikesFutures.size() == task.getResult().size()) {
                            CompletableFuture.allOf(deleteLikesFutures.toArray(new CompletableFuture[0]))
                                    .thenRun(() -> future.complete(null))
                                    .exceptionally(e -> {
                                        future.completeExceptionally(e);
                                        return null;
                                    });
                        }
                    });
                }
            } else {
                future.completeExceptionally(task.getException());
            }
        });

        return future;
    }


    public CompletableFuture<Void> removeLike(String articleId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        getLikesCollection(articleId).document(userId).delete()
                .addOnSuccessListener(aVoid -> future.complete(null)) // Успешное удаление
                .addOnFailureListener(future::completeExceptionally); // Обработка ошибки
        return future;
    }

    public CompletableFuture<Integer> getLikeCount(String articleId) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        getLikesCollection(articleId).get().addOnSuccessListener(querySnapshot -> {
            future.complete(querySnapshot.size());
        }).addOnFailureListener(future::completeExceptionally);
        return future;
    }
}
