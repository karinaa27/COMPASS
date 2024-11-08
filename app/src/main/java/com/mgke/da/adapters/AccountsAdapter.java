package com.mgke.da.adapters;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Account;
import com.mgke.da.models.Transaction;
import com.mgke.da.repository.AccountRepository;
import com.mgke.da.repository.TransactionRepository;
import java.util.List;

public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.AccountViewHolder> {

    private List<Account> accounts;
    private Context context;
    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;
    private OnAccountClickListener onAccountClickListener;

    public interface OnAccountClickListener {
        void onAccountClick(Account account);
    }

    public AccountsAdapter(List<Account> accounts, Context context, TransactionRepository transactionRepository, AccountRepository accountRepository, OnAccountClickListener onAccountClickListener) {
        this.accounts = accounts;
        this.context = context;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.onAccountClickListener = onAccountClickListener;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accounts.get(position);
        holder.accountNameTextView.setText(account.accountName);

        // Получаем данные по транзакциям
        transactionRepository.getTransactionsForAccount(account.id) // Теперь ищем по account.id
                .thenApply(transactions -> {
                    double totalAmount = account.accountAmount;  // Начальная сумма из базы данных
                    double incomeAmount = 0.0;
                    double expensesAmount = 0.0;

                    for (Transaction transaction : transactions) {
                        double transactionAmount;
                        // Преобразуем валюту, если нужно
                        if (transaction.currency.equals(account.currency)) {
                            transactionAmount = transaction.amount;
                        } else {
                            transactionAmount = convertCurrency(transaction.amount, transaction.currency, account.currency);
                        }

                        // Добавляем сумму в зависимости от типа транзакции
                        if ("DOHOD".equals(transaction.type)) {
                            incomeAmount += transactionAmount;
                        } else if ("RACHOD".equals(transaction.type)) {
                            expensesAmount += transactionAmount;
                        }

                        totalAmount += transactionAmount;
                    }

                    account.accountAmount = totalAmount;  // Обновляем итоговую сумму счета
                    account.setDataLoaded(true);

                    // Обновляем отображение данных
                    holder.accountAmountTextView.setText(String.format("%s %.2f", account.currency, totalAmount));
                    holder.incomeAmount.setText(String.format("%s %.2f", account.currency, incomeAmount));
                    holder.expensesAmount.setText(String.format("%s %.2f", account.currency, expensesAmount));

                    // Выделяем счет, если сумма отрицательная
                    if (totalAmount < 0) {
                        holder.accountAmountTextView.setTextColor(context.getResources().getColor(R.color.red)); // Красный цвет для отрицательной суммы
                        holder.accountAmountTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18); // Увеличиваем размер шрифта для акцента
                    } else {
                        holder.accountAmountTextView.setTextColor(context.getResources().getColor(R.color.green)); // Зеленый цвет для положительного баланса
                        holder.accountAmountTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Стандартный размер шрифта
                    }

                    return null;
                })
                .exceptionally(e -> {
                    // Обработка ошибок
                    return null;
                });

        // Задний фон для счета
        String backgroundName = account.background;
        if (backgroundName.equals("account_fon1")) {
            holder.itemView.setBackgroundResource(R.drawable.account_fon1);
        } else if (backgroundName.equals("account_fon2")) {
            holder.itemView.setBackgroundResource(R.drawable.account_fon2);
        } else if (backgroundName.equals("account_fon3")) {
            holder.itemView.setBackgroundResource(R.drawable.account_fon3);
        } else if (backgroundName.equals("account_fon4")) {
            holder.itemView.setBackgroundResource(R.drawable.account_fon4);
        } else if (backgroundName.equals("account_fon5")) {
            holder.itemView.setBackgroundResource(R.drawable.account_fon5);
        } else {
            holder.itemView.setBackgroundResource(R.drawable.account_fon1);
        }

        // Настроим остальные элементы, если нужно
        holder.itemView.setOnClickListener(v -> onAccountClickListener.onAccountClick(account));
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView accountNameTextView;
        TextView accountAmountTextView;
        TextView incomeAmount;
        TextView expensesAmount;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            accountNameTextView = itemView.findViewById(R.id.account_name);
            accountAmountTextView = itemView.findViewById(R.id.account_amount);
            incomeAmount = itemView.findViewById(R.id.incomeAmount);
            expensesAmount = itemView.findViewById(R.id.expensesAmount);
        }
    }

    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            return amount;
        }

        double rate = 1.0;

        if (fromCurrency.equals("USD")) {
            if (toCurrency.equals("EUR")) {
                rate = 0.85;
            } else if (toCurrency.equals("RUB")) {
                rate = 70.0;
            } else if (toCurrency.equals("BYN")) {
                rate = 2.6;
            } else if (toCurrency.equals("UAH")) {
                rate = 27.0;
            } else if (toCurrency.equals("PLN")) {
                rate = 3.7;
            }
        } else if (fromCurrency.equals("EUR")) {
            if (toCurrency.equals("USD")) {
                rate = 1.18;
            } else if (toCurrency.equals("RUB")) {
                rate = 82.0;
            } else if (toCurrency.equals("BYN")) {
                rate = 3.1;
            } else if (toCurrency.equals("UAH")) {
                rate = 31.0;
            } else if (toCurrency.equals("PLN")) {
                rate = 4.3;
            }
        } else if (fromCurrency.equals("RUB")) {
            if (toCurrency.equals("USD")) {
                rate = 0.014;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.012;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.032;
            } else if (toCurrency.equals("UAH")) {
                rate = 0.36;
            } else if (toCurrency.equals("PLN")) {
                rate = 0.05;
            }
        } else if (fromCurrency.equals("BYN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.38;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.32;
            } else if (toCurrency.equals("RUB")) {
                rate = 31.0;
            } else if (toCurrency.equals("UAH")) {
                rate = 11.0;
            } else if (toCurrency.equals("PLN")) {
                rate = 1.4;
            }
        } else if (fromCurrency.equals("UAH")) {
            if (toCurrency.equals("USD")) {
                rate = 0.037;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.032;
            } else if (toCurrency.equals("RUB")) {
                rate = 2.8;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.091;
            } else if (toCurrency.equals("PLN")) {
                rate = 0.12;
            }
        } else if (fromCurrency.equals("PLN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.27;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.23;
            } else if (toCurrency.equals("RUB")) {
                rate = 20.0;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.71;
            } else if (toCurrency.equals("UAH")) {
                rate = 8.4;
            }
        }
        return amount * rate;
    }
}