package com.mgke.da.repository;

import android.util.Log;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Article;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ArticleRepository {
    private final CollectionReference articleCollection;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ArticleRepository(FirebaseFirestore db) {
        articleCollection = db.collection("article");
    }

    public CompletableFuture<Article> addOrUpdateArticle(Article article) {
        CompletableFuture<Article> future = new CompletableFuture<>();

        if (article.id == null || article.id.isEmpty()) {
            // New article, add it
            String id = articleCollection.document().getId();
            article.id = id;
            articleCollection.document(id)
                    .set(article)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            future.complete(article);
                        } else {
                            future.completeExceptionally(task.getException());
                        }
                    });
        } else {
            // Existing article, update it
            articleCollection.document(article.id).set(article)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            future.complete(article);
                        } else {
                            future.completeExceptionally(task.getException());
                        }
                    });
        }

        return future;
    }

    public CompletableFuture<Article> getArticleById(String articleId) {
        CompletableFuture<Article> future = new CompletableFuture<>();

        db.collection("article").document(articleId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Преобразуем документ в объект Article
                        Article article = documentSnapshot.toObject(Article.class);
                        if (article != null) {
                            future.complete(article);  // Завершаем CompletableFuture с полученной статьёй
                        } else {
                            future.completeExceptionally(new Exception("Ошибка преобразования статьи"));
                        }
                    } else {
                        future.completeExceptionally(new Exception("Статья не найдена"));
                    }
                })
                .addOnFailureListener(e -> {
                    future.completeExceptionally(e);  // Завершаем CompletableFuture с исключением в случае ошибки
                });

        return future;
    }

    public void deleteArticle(String id) {
        articleCollection.document(id).delete()
                .addOnSuccessListener(aVoid -> Log.d("ArticleRepository", "Article successfully deleted"))
                .addOnFailureListener(e -> Log.e("ArticleRepository", "Error deleting article", e));
    }

    public CompletableFuture<List<Article>> getAllArticles() {
        CompletableFuture<List<Article>> future = new CompletableFuture<>();
        List<Article> articles = new ArrayList<>();

        articleCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Article article = document.toObject(Article.class);
                    articles.add(article);
                }
                future.complete(articles);
            } else {
                future.completeExceptionally(task.getException());
            }
        });

        return future;
    }
}
