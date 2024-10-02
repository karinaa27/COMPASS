package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Goal;
import com.mgke.da.models.PersonalData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PersonalDataRepository {
    private CollectionReference personalDataCollection;

    public PersonalDataRepository(FirebaseFirestore db) {
        personalDataCollection = db.collection("personalData");
    }

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
            }
        });
        return future;
    }
}
