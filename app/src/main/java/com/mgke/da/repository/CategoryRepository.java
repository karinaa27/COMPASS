<<<<<<< HEAD
    package com.mgke.da.repository;

    import android.graphics.Color;
    import android.util.Log;

    import com.google.android.gms.tasks.Task;
    import com.google.firebase.firestore.CollectionReference;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.QueryDocumentSnapshot;
    import com.mgke.da.R;
    import com.mgke.da.models.Category;

    import java.util.ArrayList;
    import java.util.List;
    import java.util.Locale;
    import java.util.concurrent.CompletableFuture;

    public class CategoryRepository {
        private CollectionReference categoryCollection;

        public CategoryRepository(FirebaseFirestore db) {
            categoryCollection = db.collection("category");
        }

        public CompletableFuture<Void> addCategory(String categoryName, String type, int categoryImage, int categoryColor, String userId) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            Category category = new Category();

            String id = categoryCollection.document().getId();
            category.id = id;
            category.name = categoryName; // Используем поле name
            category.type = type;
            category.categoryImage = categoryImage;
            category.categoryColor = categoryColor;
            category.userId = userId;

            categoryCollection.document(id).set(category)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("CategoryRepository", "Категория добавлена: " + categoryName);
                        future.complete(null);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("CategoryRepository", "Ошибка при добавлении категории", e);
                        future.completeExceptionally(e);
                    });

            return future;
        }

        public void deleteCategory(String id) {
            categoryCollection.document(id).delete()
                    .addOnSuccessListener(aVoid -> Log.d("CategoryRepository", "Category deleted: " + id))
                    .addOnFailureListener(e -> Log.e("CategoryRepository", "Error deleting category", e));
        }

        public void removeCategory(Category category, String language) {
            String categoryName = category.name; // Изменено на поле name

            if (!category.isDefault) {
                deleteCategory(category.id); // Удаляем, если это не базовая категория
                Log.d("CategoryRepository", "Категория удалена: " + categoryName);
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

        public String getCategoryName(Category category) {
            return category.name; // Изменено для использования поля name
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

        public CompletableFuture<Category> getCategoryByName(String categoryName) {
            CompletableFuture<Category> future = new CompletableFuture<>();

            categoryCollection.whereEqualTo("name", categoryName).get() // Изменено на поле name
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            Category category = document.toObject(Category.class);
                            future.complete(category); // Возвращаем найденную категорию
                        } else {
                            future.complete(null); // Категория не найдена
                        }
                    });

            return future;
        }

        public void createDefaultCategories(String userId) {
            List<Category> defaultCategories = new ArrayList<>();
            defaultCategories.add(new Category("1", "Зарплата", "Salary", R.drawable.ic_salary, Color.parseColor("#F7EC2E"), "income", userId, true,null));
            defaultCategories.add(new Category("2", "Подарок", "Gift", R.drawable.ic_gift, Color.parseColor("#C1559B"), "income", userId, true, null));
            defaultCategories.add(new Category("3", "Инвестиции", "Investments", R.drawable.category_ic_investment, Color.parseColor("#33B7B6"), "income", userId, true, null));
            defaultCategories.add(new Category("4", "Другое", "Other", R.drawable.ic_other, Color.parseColor("#704F9B"), "income", userId, false, null));

            for (Category category : defaultCategories) {
                String id = categoryCollection.document().getId();
                category.id = id;
                categoryCollection.document(id).set(category)
                        .addOnSuccessListener(aVoid -> Log.d("CategoryRepository", "Default category added: " + category.categoryNameRu))
                        .addOnFailureListener(e -> Log.e("CategoryRepository", "Error adding default category", e));
            }
        }

        public CompletableFuture<Void> createDefaultExpenseCategories(String userId) {
            List<Category> defaultExpenseCategories = new ArrayList<>();
            defaultExpenseCategories.add(new Category("1", "Продукты", "Groceries", R.drawable.category_ic_food, Color.parseColor("#00A55D"), "expense", userId, true, null));
            defaultExpenseCategories.add(new Category("2", "Транспорт", "Transport", R.drawable.category_ic_taxi, Color.parseColor("#1EB0E6"), "expense", userId, true, null));
            defaultExpenseCategories.add(new Category("3", "Развлечения", "Entertainment", R.drawable.category_ic_entertainment, Color.parseColor("#ED407B"), "expense", userId, true, null));
            defaultExpenseCategories.add(new Category("4", "Другое", "Other", R.drawable.ic_other, Color.parseColor("#704F9B"), "expense", userId, false, null));

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            CompletableFuture<Void> future = CompletableFuture.allOf(
                    defaultExpenseCategories.stream().map(category -> {
                        String id = categoryCollection.document().getId(); // Генерируем новый ID
                        category.id = id; // Присваиваем ID категории
                        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

                        categoryCollection.document(id).set(category)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("CategoryRepository", "Default expense category added: " + category.categoryNameRu);
                                    completableFuture.complete(null);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("CategoryRepository", "Error adding default expense category", e);
                                    completableFuture.completeExceptionally(e);
                                });

                        return completableFuture;
                    }).toArray(CompletableFuture[]::new)
            );

            return future;
        }

        // Получение категории по ID
        public CompletableFuture<Category> getCategoryById(String id) {
            CompletableFuture<Category> future = new CompletableFuture<>();
            categoryCollection.document(id).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Category category = task.getResult().toObject(Category.class);
                            future.complete(category);
                        } else {
                            future.complete(null); // Если категория не найдена
                        }
                    });
            return future;
        }

        // Обновление категории по ID
        public CompletableFuture<Void> updateCategoryById(String id, Category updatedCategory) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            categoryCollection.document(id).set(updatedCategory)
                    .addOnSuccessListener(aVoid -> future.complete(null))
                    .addOnFailureListener(e -> future.completeExceptionally(e));
            return future;
        }

        // Получение всех категорий для пользователя
        public CompletableFuture<List<Category>> getAllCategories(String userId) {
            CompletableFuture<List<Category>> future = new CompletableFuture<>();
            List<Category> categories = new ArrayList<>();

            categoryCollection.whereEqualTo("userId", userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                categories.add(document.toObject(Category.class));
                            }
                            future.complete(categories);
                        } else {
                            future.completeExceptionally(task.getException());
                        }
                    });

            return future;
        }

        // Проверка существования категории
        public CompletableFuture<Boolean> doesCategoryExist(String categoryName, String userId) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            categoryCollection.whereEqualTo("name", categoryName) // Изменено на поле name
                    .whereEqualTo("userId", userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            future.complete(!task.getResult().isEmpty());
                        } else {
                            future.completeExceptionally(task.getException());
                        }
                    });
            return future;
        }

        public void updateCategoryNames(String userId, String newLanguage) {
            categoryCollection.whereEqualTo("userId", userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Category category = document.toObject(Category.class);
                                // Здесь можно добавить логику для обновления названий, если это необходимо.
                                updateCategory(category);
                            }
                        } else {
                            Log.e("CategoryRepository", "Ошибка при получении категорий", task.getException());
                        }
                    });
        }
    }
=======
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
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
