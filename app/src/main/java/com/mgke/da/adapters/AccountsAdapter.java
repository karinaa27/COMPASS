package com.mgke.da.adapters;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
                        shakeAndEnlargeItem(holder.itemView);  // Применяем анимацию
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
    private void shakeAndEnlargeItem(View itemView) {
        // Анимация увеличения (масштабирование)
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(itemView, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(itemView, "scaleY", 1f, 1.05f, 1f);
        scaleX.setDuration(300);  // Время анимации (в миллисекундах)
        scaleY.setDuration(300);

        // Мягкая анимация покачивания (движение вверх-вниз с уменьшенной амплитудой)
        ObjectAnimator shakeY = ObjectAnimator.ofFloat(itemView, "translationY", 0f, 10f, -10f, 10f, -10f, 0f);
        shakeY.setDuration(600);  // Время одной итерации анимации
        shakeY.setRepeatCount(ObjectAnimator.INFINITE); // Бесконечный повтор
        shakeY.setRepeatMode(ObjectAnimator.RESTART); // Перезапуск анимации с начала

        // Запускаем анимации одновременно
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY, shakeY);
        animatorSet.start();
    }
    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            return amount;
        }

        double rate = 1.0;

        if (fromCurrency.equals("USD")) {
            if (toCurrency.equals("EUR")) {
                rate = 0.97;
            } else if (toCurrency.equals("RUB")) {
                rate = 97.83;
            } else if (toCurrency.equals("BYN")) {
                rate = 3.38;
            } else if (toCurrency.equals("UAH")) {
                rate = 41.69;
            } else if (toCurrency.equals("PLN")) {
                rate = 4.0;
            }
        } else if (fromCurrency.equals("EUR")) {
            if (toCurrency.equals("USD")) {
                rate = 1.03;
            } else if (toCurrency.equals("RUB")) {
                rate = 101.02;
            } else if (toCurrency.equals("BYN")) {
                rate = 3.49;
            } else if (toCurrency.equals("UAH")) {
                rate = 42.95;
            } else if (toCurrency.equals("PLN")) {
                rate = 4.2;
            }
        } else if (fromCurrency.equals("RUB")) {
            if (toCurrency.equals("USD")) {
                rate = 0.01;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.01;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.035;
            } else if (toCurrency.equals("UAH")) {
                rate = 0.43;
            } else if (toCurrency.equals("PLN")) {
                rate = 0.04;
            }
        } else if (fromCurrency.equals("BYN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.3;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.3;
            } else if (toCurrency.equals("RUB")) {
                rate = 28.93;
            } else if (toCurrency.equals("UAH")) {
                rate = 12.34;
            } else if (toCurrency.equals("PLN")) {
                rate = 1.2;
            }
        } else if (fromCurrency.equals("UAH")) {
            if (toCurrency.equals("USD")) {
                rate = 0.024;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.023;
            } else if (toCurrency.equals("RUB")) {
                rate = 2.33;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.08;
            } else if (toCurrency.equals("PLN")) {
                rate = 0.1;
            }
        } else if (fromCurrency.equals("PLN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.25;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.24;
            } else if (toCurrency.equals("RUB")) {
                rate = 24.1;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.83;
            } else if (toCurrency.equals("UAH")) {
                rate = 10.26;
            }
        }

        return amount * rate;
    }
}