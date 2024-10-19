package com.mgke.da.repository;

<<<<<<< HEAD
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Transaction;
=======
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.models.Goal;
import com.mgke.da.models.Transaction;

>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TransactionRepository {
    private CollectionReference transactionCollection;
<<<<<<< HEAD
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
=======

    public TransactionRepository(FirebaseFirestore db) {
        transactionCollection = db.collection("transaction");
    }

    public Transaction addTransaction(Transaction transaction) {
        String id = transactionCollection.document().getId();
        transaction.id = id;
        transactionCollection.document(id).set(transaction);
        return transaction;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    }

    public void deleteTransaction(String id) {
        transactionCollection.document(id).delete();
    }

    public void updateTransaction(Transaction transaction) {
<<<<<<< HEAD
        transactionCollection.document(transaction.id).set(transaction);
=======
       transactionCollection.document(transaction.id).set(transaction);
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
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
<<<<<<< HEAD
            } else {
                future.completeExceptionally(task.getException()); // Обработка ошибки
=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
            }
        });
        return future;
    }
<<<<<<< HEAD
}
=======
}
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
