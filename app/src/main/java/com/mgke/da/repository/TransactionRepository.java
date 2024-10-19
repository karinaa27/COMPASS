package com.mgke.da.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TransactionRepository {
    private CollectionReference transactionCollection;
    private FirebaseFirestore db; // Добавлено поле для хранения экземпляра Firestore

    public TransactionRepository(FirebaseFirestore db) {
        this.db = db; // Инициализация поля db
        transactionCollection = db.collection("transactions"); // Убедитесь, что название коллекции корректно
    }

    public Task<String> addTransaction(Transaction transaction) {
        return transactionCollection.add(transaction).continueWith(task -> {
            if (task.isSuccessful()) {
                return task.getResult().getId(); // Возвращаем автоматически сгенерированный ID
            } else {
                throw task.getException();
            }
        });
    }

    public void deleteTransaction(String id) {
        transactionCollection.document(id).delete();
    }

    public void updateTransaction(Transaction transaction) {
        transactionCollection.document(transaction.id).set(transaction);
    }

    public CompletableFuture<List<Transaction>> getAllTransaction() {
        CompletableFuture<List<Transaction>> future = new CompletableFuture<>();
        List<Transaction> transactions = new ArrayList<>();

        transactionCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Transaction transaction = document.toObject(Transaction.class);
                    transactions.add(transaction);
                }
                future.complete(transactions);
            } else {
                future.completeExceptionally(task.getException()); // Обработка ошибки
            }
        });
        return future;
    }
}