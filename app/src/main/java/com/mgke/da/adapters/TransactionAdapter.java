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
        return transactions != null ? transactions.size() : 0;
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private RowTransactionBinding binding;

        public TransactionViewHolder(RowTransactionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(v -> {
                Transaction transaction = transactions.get(getAdapterPosition());
                Bundle bundle = new Bundle();
                bundle.putSerializable("transaction", transaction);
                Navigation.findNavController(v).navigate(R.id.fragment_update_transactions, bundle);
            });
        }

        public void bind(Transaction transaction) {
            if (transaction != null) {
                binding.setTransaction(transaction);
                binding.categoryicon.setImageResource(transaction.categoryImage);

                ShapeDrawable shapeDrawable = new ShapeDrawable(new OvalShape());
                shapeDrawable.getPaint().setColor(transaction.categoryColor);
                shapeDrawable.setBounds(0, 0, binding.categoryicon.getWidth(), binding.categoryicon.getHeight());
                binding.categoryicon.setBackground(shapeDrawable);

                // Получаем имя аккаунта по accountId и устанавливаем в accountLbl
                accountRepository.getCachedAccountById(transaction.accountId).thenAccept(account -> {
                    if (account != null) {
                        binding.accountLbl.setText(account.accountName); // Устанавливаем имя аккаунта
                        int backgroundResource = getBackgroundResource(account.background);
                        binding.accountLbl.setBackgroundResource(backgroundResource);
                    } else {
                        binding.accountLbl.setText("счёт"); // Если аккаунт не найден
                        binding.accountLbl.setBackgroundResource(R.drawable.account_fon1);
                    }
                }).exceptionally(e -> {
                    binding.accountLbl.setText("Ошибка загрузки аккаунта"); // Обработка ошибок
                    binding.accountLbl.setBackgroundResource(R.drawable.account_fon1);
                    return null;
                });

                binding.transactionAmount.setText(String.valueOf(transaction.amount));
                binding.currencyLbl.setText(transaction.currency);

                int color = transaction.amount < 0 ? Color.RED : Color.GREEN;
                binding.transactionAmount.setTextColor(color);
                binding.currencyLbl.setTextColor(color);

                binding.executePendingBindings();
            } else {
                binding.accountLbl.setBackgroundResource(R.drawable.account_fon1);
            }
        }


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
                    return R.drawable.balance_fon;
            }
        }
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        if (newTransactions != null) {
            this.transactions.clear();
            this.transactions.addAll(newTransactions);
            notifyDataSetChanged();
        } else {
        }
    }
}