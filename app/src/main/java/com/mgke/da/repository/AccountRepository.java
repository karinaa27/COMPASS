package com.mgke.da.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Account;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AccountRepository {
    private CollectionReference accountCollection;
    private FirebaseFirestore firebaseFirestore;
    private Map<String, Account> accountCache = new ConcurrentHashMap<>();
    public AccountRepository(FirebaseFirestore db) {
        this.firebaseFirestore = db;
        accountCollection = db.collection("accounts");
    }

    public CompletableFuture<Void> deleteAccount(String id) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (id == null) {
            future.completeExceptionally(new IllegalArgumentException("ID не может быть null"));
            return future;
        }
        accountCollection.document(id).delete()
                .addOnSuccessListener(aVoid -> future.complete(null))
                .addOnFailureListener(e -> future.completeExceptionally(e));
        return future;
    }

    public Task<Void> addAccount(Account account) {
        return firebaseFirestore.collection("accounts")
                .add(account)
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    String documentId = task.getResult().getId();
                    account.id = documentId;
                    return null;
                }).continueWithTask(task -> {
                    return firebaseFirestore.collection("accounts")
                            .document(account.id)
                            .update("id", account.id);
                });
    }

    public Task<Void> updateAccount(Account account) {
        return firebaseFirestore.collection("accounts")
                .document(account.id)
                .set(account);
    }

    public CompletableFuture<Account> getAccountByName(String accountName) {
        CompletableFuture<Account> future = new CompletableFuture<>();
        accountCollection.whereEqualTo("accountName", accountName).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        Account account = document.toObject(Account.class);
                        future.complete(account);
                    } else {
                        future.complete(null);
                    }
                })
                .addOnFailureListener(e -> future.completeExceptionally(e));
        return future;
    }

    public CompletableFuture<List<Account>> getAllAccounts() {
        CompletableFuture<List<Account>> future = new CompletableFuture<>();
        List<Account> accounts = new ArrayList<>();
        accountCollection.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Account account = document.toObject(Account.class);
                            accounts.add(account);
                        }
                        future.complete(accounts);
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                })
                .addOnFailureListener(e -> future.completeExceptionally(e));
        return future;
    }

    public CompletableFuture<Account> getAccountById(String accountId) {
        CompletableFuture<Account> future = new CompletableFuture<>();

        // Поиск по accountId в документе, но Firestore использует поле "id" внутри документа
        accountCollection.whereEqualTo("id", accountId) // Ищем по полю id в документе
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        // Получаем первый результат, так как у нас должен быть только один аккаунт с таким id
                        Account account = task.getResult().getDocuments().get(0).toObject(Account.class);
                        future.complete(account);
                    } else {
                        future.complete(null); // Если аккаунт не найден
                    }
                })
                .addOnFailureListener(e -> future.completeExceptionally(e)); // Обработка ошибок

        return future;
    }


    public CompletableFuture<Account> getCachedAccountById(String accountId) {
        if (accountId == null) {
            // Возвращаем пустое значение или завершаем CompletableFuture с null
            return CompletableFuture.completedFuture(null);
        }

        if (accountCache.containsKey(accountId)) {
            return CompletableFuture.completedFuture(accountCache.get(accountId));
        } else {
            return getAccountById(accountId).thenApply(account -> {
                if (account != null) {
                    accountCache.put(accountId, account);
                }
                return account;
            });
        }
    }

    public CompletableFuture<List<Account>> getAccountsByUserId(String userId) {
        CompletableFuture<List<Account>> future = new CompletableFuture<>();
        List<Account> accounts = new ArrayList<>();
        accountCollection.whereEqualTo("userId", userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Account account = document.toObject(Account.class);
                            accounts.add(account);
                        }
                        future.complete(accounts);
                    } else {
                        future.completeExceptionally(task.getException());
                    }
                })
                .addOnFailureListener(e -> future.completeExceptionally(e));
        return future;
    }
}