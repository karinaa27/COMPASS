package com.mgke.da.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TransactionRepository {
    private CollectionReference transactionCollection;
    private FirebaseFirestore db;

    public TransactionRepository(FirebaseFirestore db) {
        this.db = db;
        transactionCollection = db.collection("transactions");
    }

    public Task<String> addTransaction(Transaction transaction) {
        DocumentReference documentReference = transactionCollection.document();
        transaction.id = documentReference.getId();
        return documentReference.set(transaction).continueWith(task -> {
            if (task.isSuccessful()) {
                return transaction.id;
            } else {
                throw task.getException();
            }
        });
    }

    public Task<Void> deleteTransaction(String id) {
        return transactionCollection.document(id).delete();
    }

    public Task<Void> updateTransaction(Transaction transaction) {
        return transactionCollection.document(transaction.id).set(transaction);
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
                future.completeExceptionally(task.getException());
            }
        });
        return future;
    }

    public CompletableFuture<List<Transaction>> getTransactionsForGoalId(String goalId) {
        CompletableFuture<List<Transaction>> future = new CompletableFuture<>();
        List<Transaction> transactions = new ArrayList<>();

        transactionCollection.whereEqualTo("goalId", goalId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Transaction transaction = document.toObject(Transaction.class);
                    transactions.add(transaction);
                }
                future.complete(transactions);
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        return future;
    }

    public CompletableFuture<List<Transaction>> getTransactionsForUserId(String userId) {
        CompletableFuture<List<Transaction>> future = new CompletableFuture<>();
        List<Transaction> transactions = new ArrayList<>();

        transactionCollection.whereEqualTo("userId", userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Transaction transaction = document.toObject(Transaction.class);
                    transactions.add(transaction);
                }
                future.complete(transactions);
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        return future;
    }

    public CompletableFuture<List<Transaction>> getTransactionsForAccount(String accountName) {
        CompletableFuture<List<Transaction>> future = new CompletableFuture<>();
        List<Transaction> transactions = new ArrayList<>();

        transactionCollection.whereEqualTo("account", accountName).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Transaction transaction = document.toObject(Transaction.class);
                    transactions.add(transaction);
                }
                future.complete(transactions);
            } else {
                future.completeExceptionally(task.getException());
            }
        });
        return future;
    }
}
