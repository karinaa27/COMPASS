package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Article;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ArticleRepository {
    private CollectionReference articleCollection;

    public ArticleRepository(FirebaseFirestore db) {
        articleCollection = db.collection("article");
    }

    public Article addArticle(Article article) {
        String id = articleCollection.document().getId();
        article.id = id;
        articleCollection.document(id).set(article);
        return article;
    }

    public void deleteArticle(String id) {
        articleCollection.document(id).delete();
    }

    public void updateArticle(Article article) {
        articleCollection.document(article.id).set(article);
    }

    public CompletableFuture<List<Article>> getAllAccount() {
        CompletableFuture<List<Article>> future = new CompletableFuture<>();
        List<Article> articles = new ArrayList<>();

        articleCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Article article = document.toObject(Article.class);
                    articles.add(article);
                }
                future.complete(articles);
            }
        });
        return future;
    }
}
