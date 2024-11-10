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

            String id = categoryCollection.document().getId();  // Генерируем уникальный ID
            category.id = id;
            category.name = categoryName;
            category.type = type;
            category.categoryImage = categoryImage;
            category.categoryColor = categoryColor;
            category.userId = userId;

            categoryCollection.document(id).set(category)
                    .addOnSuccessListener(aVoid -> future.complete(null))
                    .addOnFailureListener(future::completeExceptionally);

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

        public CompletableFuture<Void> createDefaultCategories(String userId, String type) {
            CompletableFuture<Void> future = new CompletableFuture<>();

            // Список названий категорий для проверки
            List<String> defaultCategoryNames = type.equals("income") ?
                    List.of("Зарплата", "Подарок", "Инвестиции", "Другое") :
                    List.of("Продукты", "Транспорт", "Развлечения", "Другое");

            // Выполняем запрос для проверки существующих категорий с тем же userId, type и названием
            categoryCollection.whereEqualTo("userId", userId)
                    .whereEqualTo("type", type)
                    .whereIn("name", defaultCategoryNames)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<String> existingCategoryNames = new ArrayList<>();

                            if (task.getResult() != null) {
                                task.getResult().forEach(document -> {
                                    String name = document.getString("name");
                                    if (name != null) {
                                        existingCategoryNames.add(name);
                                    }
                                });
                            }

                            // Создаем только категории, которых нет в existingCategoryNames
                            List<Category> defaultCategories = new ArrayList<>();

                            if (type.equals("income")) {
                                if (!existingCategoryNames.contains("Зарплата")) {
                                    defaultCategories.add(new Category(categoryCollection.document().getId(), "Зарплата", "Salary", R.drawable.ic_salary, Color.parseColor("#F7EC2E"), "income", userId, true, null));
                                }
                                if (!existingCategoryNames.contains("Подарок")) {
                                    defaultCategories.add(new Category(categoryCollection.document().getId(), "Подарок", "Gift", R.drawable.ic_gift, Color.parseColor("#C1559B"), "income", userId, true, null));
                                }
                                if (!existingCategoryNames.contains("Инвестиции")) {
                                    defaultCategories.add(new Category(categoryCollection.document().getId(), "Инвестиции", "Investments", R.drawable.category_ic_investment, Color.parseColor("#33B7B6"), "income", userId, true, null));
                                }
                                if (!existingCategoryNames.contains("Другое")) {
                                    defaultCategories.add(new Category(categoryCollection.document().getId(), "Другое", "Other", R.drawable.ic_other, Color.parseColor("#704F9B"), "income", userId, true, null));
                                }
                            } else if (type.equals("expense")) {
                                if (!existingCategoryNames.contains("Продукты")) {
                                    defaultCategories.add(new Category(categoryCollection.document().getId(), "Продукты", "Groceries", R.drawable.category_ic_food, Color.parseColor("#00A55D"), "expense", userId, true, null));
                                }
                                if (!existingCategoryNames.contains("Транспорт")) {
                                    defaultCategories.add(new Category(categoryCollection.document().getId(), "Транспорт", "Transport", R.drawable.category_ic_taxi, Color.parseColor("#1EB0E6"), "expense", userId, true, null));
                                }
                                if (!existingCategoryNames.contains("Развлечения")) {
                                    defaultCategories.add(new Category(categoryCollection.document().getId(), "Развлечения", "Entertainment", R.drawable.category_ic_entertainment, Color.parseColor("#ED407B"), "expense", userId, true, null));
                                }
                                if (!existingCategoryNames.contains("Другое")) {
                                    defaultCategories.add(new Category(categoryCollection.document().getId(), "Другое", "Other", R.drawable.ic_other, Color.parseColor("#704F9B"), "expense", userId, true, null));
                                }
                            }

                            // Добавляем только недостающие категории
                            CompletableFuture<Void> creationFuture = CompletableFuture.allOf(
                                    defaultCategories.stream().map(category -> {
                                        CompletableFuture<Void> completableFuture = new CompletableFuture<>();
                                        categoryCollection.document(category.id).set(category)
                                                .addOnSuccessListener(aVoid -> completableFuture.complete(null))
                                                .addOnFailureListener(completableFuture::completeExceptionally);
                                        return completableFuture;
                                    }).toArray(CompletableFuture[]::new)
                            );

                            creationFuture.whenComplete((v, ex) -> {
                                if (ex != null) {
                                    future.completeExceptionally(ex);
                                } else {
                                    future.complete(null);
                                }
                            });
                        } else {
                            future.completeExceptionally(task.getException());
                        }
                    });

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