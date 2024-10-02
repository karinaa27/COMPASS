package com.mgke.da.repository;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Goal;
import com.mgke.da.models.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TransactionRepository {
    private CollectionReference transactionCollection;

    public TransactionRepository(FirebaseFirestore db) {
        transactionCollection = db.collection("transaction");
    }

    public Transaction addTransaction(Transaction transaction) {
        String id = transactionCollection.document().getId();
        transaction.id = id;
        transactionCollection.document(id).set(transaction);
        return transaction;
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
            }
        });
        return future;
    }
}
