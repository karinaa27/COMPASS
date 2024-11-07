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
            category.name = categoryName;
            category.type = type;
            category.categoryImage = categoryImage;
            category.categoryColor = categoryColor;
            category.userId = userId;

            categoryCollection.document(id).set(category)
                    .addOnSuccessListener(aVoid -> {
                        future.complete(null);
                    })
                    .addOnFailureListener(e -> {
                        future.completeExceptionally(e);
                    });

            return future;
        }

        public Task<Void> deleteCategory(String id) {
            return categoryCollection.document(id).delete();
        }


        public void removeCategory(Category category, String language) {
            String categoryName = category.name;
            if (!category.isDefault) {
                deleteCategory(category.id);

            } else {

                deleteCategory(category.id);
            }
        }

        public CompletableFuture<List<Category>> getAllCategory(String userId) {
            return getAllCategoriesByType(userId, "income");
        }

        public CompletableFuture<List<Category>> getAllExpenseCategories(String userId) {
            return getAllCategoriesByType(userId, "expense");
        }

        public String getCategoryName(Category category) {
            return category.name;
        }

        private CompletableFuture<List<Category>> getAllCategoriesByType(String userId, String type) {
            CompletableFuture<List<Category>> future = new CompletableFuture<>();
            List<Category> categories = new ArrayList<>();

            categoryCollection.whereEqualTo("userId", userId)
                    .whereEqualTo("type", type)
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
            defaultCategories.add(new Category("1", "Зарплата", "Salary", R.drawable.ic_salary, Color.parseColor("#F7EC2E"), "income", userId, true,null));
            defaultCategories.add(new Category("2", "Подарок", "Gift", R.drawable.ic_gift, Color.parseColor("#C1559B"), "income", userId, true, null));
            defaultCategories.add(new Category("3", "Инвестиции", "Investments", R.drawable.category_ic_investment, Color.parseColor("#33B7B6"), "income", userId, true, null));
            defaultCategories.add(new Category("4", "Другое", "Other", R.drawable.ic_other, Color.parseColor("#704F9B"), "income", userId, true, null));

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
            defaultExpenseCategories.add(new Category("4", "Другое", "Other", R.drawable.ic_other, Color.parseColor("#704F9B"), "expense", userId, true, null));

            List<CompletableFuture<Void>> futures = new ArrayList<>();
            CompletableFuture<Void> future = CompletableFuture.allOf(
                    defaultExpenseCategories.stream().map(category -> {
                        String id = categoryCollection.document().getId(); // Генерируем новый ID
                        category.id = id; // Присваиваем ID категории
                        CompletableFuture<Void> completableFuture = new CompletableFuture<>();

                        categoryCollection.document(id).set(category)
                                .addOnSuccessListener(aVoid -> {
                                    completableFuture.complete(null);
                                })
                                .addOnFailureListener(e -> {
                                    completableFuture.completeExceptionally(e);
                                });

                        return completableFuture;
                    }).toArray(CompletableFuture[]::new)
            );

            return future;
        }
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
    }