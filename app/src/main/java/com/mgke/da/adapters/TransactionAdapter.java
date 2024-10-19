package com.mgke.da.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Transaction;
import com.mgke.da.databinding.RowTransactionBinding;
import com.mgke.da.repository.AccountRepository;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<Transaction> transactions;
    private Context context;
    private AccountRepository accountRepository;

    public TransactionAdapter(Context context, List<Transaction> transactions, AccountRepository accountRepository) {
        this.context = context;
        this.transactions = transactions;
        this.accountRepository = accountRepository;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        RowTransactionBinding binding = RowTransactionBinding.inflate(inflater, parent, false);
        return new TransactionViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction);
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0; // Избегаем NPE
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private RowTransactionBinding binding;

        public TransactionViewHolder(RowTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Установка обработчика клика
            itemView.setOnClickListener(v -> {
                Transaction transaction = transactions.get(getAdapterPosition());
                // Передаем данные транзакции в новый фрагмент
                Bundle bundle = new Bundle();
                bundle.putSerializable("transaction", transaction); // Предполагается, что Transaction реализует Serializable
                Navigation.findNavController(v).navigate(R.id.fragment_update_transactions, bundle);
            });
        }

        public void bind(Transaction transaction) {
            if (transaction != null) {
                binding.setTransaction(transaction);
                Log.d("TransactionBinding", "Binding transaction: " + transaction.account);

                // Установить иконку категории
                binding.categoryicon.setImageResource(transaction.categoryImage); // Установка иконки

                // Установить цвет фона для иконки
                ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
                shapeDrawable.getPaint().setColor(transaction.categoryColor); // Установите цвет фона
                shapeDrawable.setBounds(0, 0, binding.categoryicon.getWidth(), binding.categoryicon.getHeight());
                binding.categoryicon.setBackground(shapeDrawable);

                // Получаем фон счета по имени
                accountRepository.getAccountByName(transaction.account).thenAccept(account -> {
                    if (account != null) {
                        // Устанавливаем фон из найденного счета
                        int backgroundResource = getBackgroundResource(account.background);
                        binding.accountLbl.setBackgroundResource(backgroundResource);
                    } else {
                        // Установите фон по умолчанию, если счет не найден
                        binding.accountLbl.setBackgroundResource(R.drawable.account_fon1);
                    }
                }).exceptionally(e -> {
                    Log.e("TransactionBinding", "Error fetching account", e);
                    binding.accountLbl.setBackgroundResource(R.drawable.account_fon1); // Установите фон по умолчанию
                    return null;
                });

                // Установка текста суммы и валюты
                binding.transactionAmount.setText(String.valueOf(transaction.amount)); // Прямой доступ к полю
                binding.currencyLbl.setText(transaction.currency); // Прямой доступ к полю

                // Установка цвета текста для суммы и валюты
                if (transaction.amount < 0) {
                    binding.transactionAmount.setTextColor(Color.RED); // Красный для отрицательной суммы
                    binding.currencyLbl.setTextColor(Color.RED); // Красный для валюты
                } else {
                    binding.transactionAmount.setTextColor(Color.parseColor("#006400")); // Темно-зеленый для положительной суммы
                    binding.currencyLbl.setTextColor(Color.parseColor("#006400")); // Темно-зеленый для валюты
                }

                binding.executePendingBindings();
            } else {
                Log.e("TransactionBinding", "Transaction is null");
                binding.accountLbl.setBackgroundResource(R.drawable.account_fon1); // Установите фон по умолчанию
            }
        }

        // Метод для получения ресурса фона
        private int getBackgroundResource(String backgroundName) {
            switch (backgroundName) {
                case "account_fon1":
                    return R.drawable.account_fon1;
                case "account_fon2":
                    return R.drawable.account_fon2;
                case "account_fon3":
                    return R.drawable.account_fon3;
                case "account_fon4":
                    return R.drawable.account_fon4;
                case "account_fon5":
                    return R.drawable.account_fon5;
                default:
                    return R.drawable.balance_fon; // Фон по умолчанию
            }
        }
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        if (newTransactions != null) {
            this.transactions.clear(); // Сначала очищаем старый список
            this.transactions.addAll(newTransactions); // Добавляем новые транзакции
            notifyDataSetChanged();
        } else {
            Log.e("TransactionAdapter", "New transactions list is null");
        }
    }
}