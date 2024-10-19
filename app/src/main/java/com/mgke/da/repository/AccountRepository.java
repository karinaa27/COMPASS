package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
<<<<<<< HEAD
import com.google.firebase.firestore.DocumentSnapshot;
=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
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
<<<<<<< HEAD
        accountCollection.document(id).set(account).addOnSuccessListener(aVoid -> {
            // Успешное сохранение
        }).addOnFailureListener(e -> {
            // Обработка ошибок при сохранении
        });
=======
        accountCollection.document(id).set(account);
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
        return account;
    }

    public void deleteAccount(String id) {
        accountCollection.document(id).delete();
    }

    public void updateAccount(Account account) {
        accountCollection.document(account.id).set(account);
    }

<<<<<<< HEAD
    public CompletableFuture<Account> getAccountByName(String accountName) {
        CompletableFuture<Account> future = new CompletableFuture<>();

        accountCollection.whereEqualTo("accountName", accountName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                DocumentSnapshot document = task.getResult().getDocuments().get(0); // Измените здесь
                Account account = document.toObject(Account.class);
                future.complete(account);
            } else {
                future.complete(null); // Если не найден
            }
        }).addOnFailureListener(e -> {
            future.completeExceptionally(e);
        });

        return future;
    }

=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
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
<<<<<<< HEAD
    // Обновленный метод для получения всех счетов по userId
    public CompletableFuture<List<Account>> getAccountsByUserId(String userId) {
        CompletableFuture<List<Account>> future = new CompletableFuture<>();
        List<Account> accounts = new ArrayList<>();

        accountCollection.whereEqualTo("userId", userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Account account = document.toObject(Account.class);
                    accounts.add(account);
                }
                future.complete(accounts);
            } else {
                future.completeExceptionally(task.getException());
            }
        }).addOnFailureListener(e -> {
            future.completeExceptionally(e);
        });

        return future;
    }
=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
}
