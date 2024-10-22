package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Article;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ArticleRepository {
    private final CollectionReference articleCollection;

    public ArticleRepository(FirebaseFirestore db) {
        articleCollection = db.collection("article");
    }

    public CompletableFuture<Article> addArticle(Article article) {
        CompletableFuture<Article> future = new CompletableFuture<>();

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

        return future;
    }


    public void deleteArticle(String id) {
        articleCollection.document(id).delete();
    }

    public void updateArticle(Article article) {
        articleCollection.document(article.id).set(article);
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
