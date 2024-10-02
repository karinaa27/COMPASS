package com.mgke.da.repository;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.R;
import com.mgke.da.models.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CategoryRepository {
    private CollectionReference categoryCollection;

    public CategoryRepository(FirebaseFirestore db) {
        categoryCollection = db.collection("category");
    }

    public Task<Void> addCategory(Category category) {
        return categoryCollection.whereEqualTo("categoryName", category.categoryName)
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().isEmpty()) {
                        String id = categoryCollection.document().getId();
                        category.id = id;
                        return categoryCollection.document(id).set(category);
                    } else {
                        Log.d("CategoryRepository", "Категория уже существует: " + category.categoryName);
                        throw new Exception("Категория уже существует");
                    }
                })
                .addOnSuccessListener(aVoid -> Log.d("CategoryRepository", "Категория добавлена: " + category.categoryName))
                .addOnFailureListener(e -> Log.e("CategoryRepository", "Ошибка при добавлении категории", e));
    }

    public void deleteCategory(String id) {
        categoryCollection.document(id).delete()
                .addOnSuccessListener(aVoid -> Log.d("CategoryRepository", "Category deleted: " + id))
                .addOnFailureListener(e -> Log.e("CategoryRepository", "Error deleting category", e));
    }

    public void removeCategory(Category category) {
        if (!category.isDefault) {
            deleteCategory(category.id); // Удаляем, если это не базовая категория
            Log.d("CategoryRepository", "Category deleted: " + category.categoryName);
        } else {
            Log.d("CategoryRepository", "Базовые категории не могут быть удалены, но удаляем для пользователя.");
            deleteCategory(category.id); // Удаляем базовую категорию для пользователя
        }
    }

    public void updateCategory(Category category) {
        categoryCollection.document(category.id).set(category);
    }

    public CompletableFuture<List<Category>> getAllCategory(String userId) {
        return getAllCategoriesByType(userId, "income");
    }

    public CompletableFuture<List<Category>> getAllExpenseCategories(String userId) {
        return getAllCategoriesByType(userId, "expense");
    }

    private CompletableFuture<List<Category>> getAllCategoriesByType(String userId, String type) {
        CompletableFuture<List<Category>> future = new CompletableFuture<>();
        List<Category> categories = new ArrayList<>();

        categoryCollection.whereEqualTo("userId", userId)
                .whereEqualTo("type", type) // Фильтрация по типу категории
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Category category = document.toObject(Category.class);
                            categories.add(category);
                        }
                        future.complete(categories);
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                });

        return future;
    }

    public void createDefaultCategories(String userId) {
        List<Category> defaultCategories = new ArrayList<>();
        defaultCategories.add(new Category("1", "Зарплата", R.drawable.ic_salary, Color.parseColor("#FFD700"), "income", userId, true));
        defaultCategories.add(new Category("2", "Подарок", R.drawable.ic_gift, Color.parseColor("#FF4500"), "income", userId, true));
        defaultCategories.add(new Category("3", "Инвестиции", R.drawable.category_ic_investment, Color.parseColor("#32CD32"), "income", userId, true));
        defaultCategories.add(new Category("4", "Другое", R.drawable.ic_other, Color.parseColor("#808080"), "income", userId, false)); // Категория "Другое"

        for (Category category : defaultCategories) {
            String id = categoryCollection.document().getId(); // Генерируем новый ID
            category.id = id; // Присваиваем ID категории
            categoryCollection.document(id).set(category)
                    .addOnSuccessListener(aVoid -> Log.d("CategoryRepository", "Default category added: " + category.categoryName))
                    .addOnFailureListener(e -> Log.e("CategoryRepository", "Error adding default category", e));
        }
    }

    public void createDefaultExpenseCategories(String userId) {
        List<Category> defaultExpenseCategories = new ArrayList<>();
        defaultExpenseCategories.add(new Category("1", "Продукты", R.drawable.category_ic_food, Color.parseColor("#FF6347"), "expense", userId, true));
        defaultExpenseCategories.add(new Category("2", "Транспорт", R.drawable.category_ic_taxi, Color.parseColor("#4682B4"), "expense", userId, true));
        defaultExpenseCategories.add(new Category("3", "Развлечения", R.drawable.category_ic_entertainment, Color.parseColor("#FFD700"), "expense", userId, true));
        defaultExpenseCategories.add(new Category("4", "Другое", R.drawable.ic_other, Color.parseColor("#808080"), "expense", userId, false)); // Категория "Другое"

        for (Category category : defaultExpenseCategories) {
            String id = categoryCollection.document().getId(); // Генерируем новый ID
            category.id = id; // Присваиваем ID категории
            categoryCollection.document(id).set(category)
                    .addOnSuccessListener(aVoid -> Log.d("CategoryRepository", "Default expense category added: " + category.categoryName))
                    .addOnFailureListener(e -> Log.e("CategoryRepository", "Error adding default expense category", e));
        }
    }
}