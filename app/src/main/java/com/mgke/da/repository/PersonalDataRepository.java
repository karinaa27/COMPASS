package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
<<<<<<< HEAD
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
=======
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Goal;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
import com.mgke.da.models.PersonalData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PersonalDataRepository {
<<<<<<< HEAD
    private final CollectionReference personalDataCollection;
=======
    private CollectionReference personalDataCollection;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee

    public PersonalDataRepository(FirebaseFirestore db) {
        personalDataCollection = db.collection("personalData");
    }

<<<<<<< HEAD
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
=======
    public PersonalData addPersonalData(PersonalData personalData) {
        String id = personalDataCollection.document().getId();
        personalData.id = id;
        personalDataCollection.document(id).set(personalData);
        return personalData;
    }

    public void deletePersonalData(String id) {
        personalDataCollection.document(id).delete();
    }

    public void updatePersonalData(PersonalData personalData) {
        personalDataCollection.document(personalData.id).set(personalData);
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
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
<<<<<<< HEAD
            } else {
                future.completeExceptionally(task.getException());
=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
            }
        });
        return future;
    }
<<<<<<< HEAD
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
=======
}
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
