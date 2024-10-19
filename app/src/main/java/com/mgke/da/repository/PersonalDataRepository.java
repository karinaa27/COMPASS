package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.PersonalData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PersonalDataRepository {
    private final CollectionReference personalDataCollection;

    public PersonalDataRepository(FirebaseFirestore db) {
        personalDataCollection = db.collection("personalData");
    }

    // Получение нового уникального ID
    public String getNewDocumentId() {
        return personalDataCollection.document().getId();
    }

    // Добавление или обновление данных пользователя
    public CompletableFuture<Void> addOrUpdatePersonalData(PersonalData personalData) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        personalDataCollection.document(personalData.id).set(personalData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        future.complete(null); // Успешно завершено
                    } else {
                        future.completeExceptionally(task.getException()); // Ошибка
                    }
                });
        return future;
    }

    // Удаление данных пользователя
    public CompletableFuture<Void> deletePersonalData(String userId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        personalDataCollection.document(userId).delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        future.complete(null); // Успешно завершено
                    } else {
                        future.completeExceptionally(task.getException()); // Ошибка
                    }
                });
        return future;
    }

    public CompletableFuture<PersonalData> getPersonalDataById(String id) {
        CompletableFuture<PersonalData> future = new CompletableFuture<>();
        personalDataCollection.document(id).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                PersonalData personalData = task.getResult().toObject(PersonalData.class);
                future.complete(personalData);
            } else {
                future.complete(null); // Если данные не найдены или ошибка
            }
        });
        return future;
    }

    public CompletableFuture<List<PersonalData>> getAllPersonalData() {
        CompletableFuture<List<PersonalData>> future = new CompletableFuture<>();
        List<PersonalData> personalDatas = new ArrayList<>();

        personalDataCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    PersonalData personalData = document.toObject(PersonalData.class);
                    personalDatas.add(personalData);
                }
                future.complete(personalDatas);
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        return future;
    }
    public CompletableFuture<PersonalData> getUserByEmail(String email) {
        CompletableFuture<PersonalData> future = new CompletableFuture<>();

        personalDataCollection.whereEqualTo("email", email).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                PersonalData personalData = document.toObject(PersonalData.class);
                future.complete(personalData);
            } else {
                future.complete(null);
            }
        });

        return future;
    }
}