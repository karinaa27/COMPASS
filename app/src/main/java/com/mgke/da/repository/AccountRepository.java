package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Account;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AccountRepository {
    private CollectionReference accountCollection;

    public AccountRepository(FirebaseFirestore db) {
        accountCollection = db.collection("account");
    }

    public Account addAccount(Account account) {
        String id = accountCollection.document().getId();
        account.id = id;
        accountCollection.document(id).set(account).addOnSuccessListener(aVoid -> {
            // Успешное сохранение
        }).addOnFailureListener(e -> {
            // Обработка ошибок при сохранении
        });
        return account;
    }

    public void deleteAccount(String id) {
        accountCollection.document(id).delete();
    }

    public void updateAccount(Account account) {
        accountCollection.document(account.id).set(account);
    }

    public CompletableFuture<List<Account>> getAllAccount() {
        CompletableFuture<List<Account>> future = new CompletableFuture<>();
        List<Account> accounts = new ArrayList<>();

        accountCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Account account = document.toObject(Account.class);
                    accounts.add(account);
                }
                future.complete(accounts);
            }
        });

        return future;
    }
}
