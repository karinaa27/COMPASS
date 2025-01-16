package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.mgke.da.models.Comment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CommentRepository {
    private final FirebaseFirestore db;

    public CommentRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public CompletableFuture<Void> deleteComment(String articleId, String commentId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        getCommentsCollection(articleId).document(commentId).delete()
                .addOnSuccessListener(aVoid -> future.complete(null))
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }


    private CollectionReference getCommentsCollection(String articleId) {
        return db.collection("articles").document(articleId).collection("comments");
    }

    public CompletableFuture<Void> addComment(String articleId, Comment comment) {
        return CompletableFuture.runAsync(() -> getCommentsCollection(articleId).add(comment));
    }

    public CompletableFuture<List<Comment>> getCommentsForArticle(String articleId) {
        return getComments(articleId);
    }
    public CompletableFuture<Void> removeAllComments(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        // Получаем все комментарии из коллекции comments
        db.collectionGroup("comments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<CompletableFuture<Void>> deleteCommentFutures = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Получаем комментарий
                            Comment comment = document.toObject(Comment.class);
                            // Проверяем, является ли userId равным переданному
                            if (comment.getUserId().equals(userId)) {
                                String articleId = document.getReference().getParent().getParent().getId();
                                String commentId = document.getId();

                                // Создаем CompletableFuture для удаления комментария
                                CompletableFuture<Void> deleteFuture = new CompletableFuture<>();
                                getCommentsCollection(articleId).document(commentId).delete()
                                        .addOnCompleteListener(deleteTask -> {
                                            if (deleteTask.isSuccessful()) {
                                                deleteFuture.complete(null);
                                            } else {
                                                deleteFuture.completeExceptionally(deleteTask.getException());
                                            }
                                        });
                                deleteCommentFutures.add(deleteFuture);
                            }
                        }
                        // Ждем завершения всех операций удаления комментариев
                        CompletableFuture.allOf(deleteCommentFutures.toArray(new CompletableFuture[0]))
                                .thenRun(() -> future.complete(null))
                                .exceptionally(e -> {
                                    future.completeExceptionally(e);
                                    return null;
                                });
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                });
        return future;
    }

    public CompletableFuture<Void> updateUserComments(String userId, String newUserName, String newUserImage) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        db.collectionGroup("comments")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot document : task.getResult().getDocuments()) {
                            DocumentReference commentRef = document.getReference();
                            batch.update(commentRef, "userName", newUserName);
                            batch.update(commentRef, "userImage", newUserImage != null ? newUserImage : "default_user_icon_url");
                        }
                        batch.commit().addOnCompleteListener(batchTask -> {
                            if (batchTask.isSuccessful()) {
                                future.complete(null);
                            } else {
                                future.completeExceptionally(batchTask.getException());
                            }
                        });
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                });
        return future;
    }

    public CompletableFuture<List<Comment>> getCommentsForUser(String userId) {
        CompletableFuture<List<Comment>> future = new CompletableFuture<>();
        db.collectionGroup("comments")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Comment> comments = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            comments.add(document.toObject(Comment.class));
                        }
                        future.complete(comments);
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                });
        return future;
    }
    public CompletableFuture<Void> updateComment(String articleId, Comment comment) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        getCommentsCollection(articleId).document(comment.id)
                .set(comment)
                .addOnSuccessListener(aVoid -> future.complete(null))
                .addOnFailureListener(future::completeExceptionally);
        return future;
    }



    public CompletableFuture<List<Comment>> getComments(String articleId) {
        CompletableFuture<List<Comment>> future = new CompletableFuture<>();
        getCommentsCollection(articleId).get().addOnSuccessListener(querySnapshot -> {
            List<Comment> comments = querySnapshot.toObjects(Comment.class);
            future.complete(comments);
        }).addOnFailureListener(future::completeExceptionally);
        return future;
    }
}
