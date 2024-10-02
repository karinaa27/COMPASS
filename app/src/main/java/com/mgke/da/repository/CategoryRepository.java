package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Category;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CategoryRepository {
    private CollectionReference categoryCollection;

    public CategoryRepository(FirebaseFirestore db) {
        categoryCollection = db.collection("category");
    }

    public Category addCategory(Category category) {
        String id = categoryCollection.document().getId();
        category.id = id;
        categoryCollection.document(id).set(category);
        return category;
    }

    public void deleteCategory(String id) {
        categoryCollection.document(id).delete();
    }

    public void updateCategory(Category category) {
        categoryCollection.document(category.id).set(category);
    }

    public CompletableFuture<List<Category>> getAllCategory() {
        CompletableFuture<List<Category>> future = new CompletableFuture<>();
        List<Category> categories = new ArrayList<>();

        categoryCollection.get().addOnCompleteListener(task -> {
           if (task.isSuccessful()) {
               for (QueryDocumentSnapshot document : task.getResult()) {
                   Category category = document.toObject(Category.class);
                   categories.add(category);
               }
               future.complete(categories);
           }
        });

        return future;
    }
}
