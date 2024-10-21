package com.mgke.da.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Goal;
import com.mgke.da.models.Transaction;
import com.mgke.da.repository.GoalRepository;
import com.mgke.da.repository.TransactionRepository;
import java.util.List;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    private List<Goal> goalsList;
    private Context context;
    private GoalRepository goalRepository;
    private TransactionRepository transactionRepository;
    private OnGoalClickListener listener;
    private String currentCurrency;
    private static final String TAG = "GoalAdapter";

    public interface OnGoalClickListener {
        void onGoalClick(Goal goal);
    }

    public GoalAdapter(List<Goal> goalsList, Context context, GoalRepository goalRepository, TransactionRepository transactionRepository, String currentCurrency, OnGoalClickListener listener) {
        this.goalsList = goalsList;
        this.context = context;
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
        this.currentCurrency = currentCurrency;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.goal_item, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        Goal goal = goalsList.get(position);
        holder.textViewGoalName.setText(goal.goalName);

        Log.d(TAG, "Обновление цели: " + goal.goalName);

        // Получаем транзакции для данной цели
        transactionRepository.getTransactionsForGoalName(goal.goalName).thenAccept(transactions -> {
            double totalProgress = 0.0;
            for (Transaction transaction : transactions) {
                // Сравниваем валюты и суммируем прогресс
                if (transaction.currency.equals(goal.currency)) {
                    totalProgress += transaction.amount;
                } else {
                    // Конвертируем сумму транзакции в валюту цели
                    double convertedAmount = convertCurrency(transaction.amount, transaction.currency, goal.currency);
                    totalProgress += convertedAmount;
                }
            }

            Log.d(TAG, "Общая сумма транзакций для цели " + goal.goalName + ": " + totalProgress); // Лог суммы транзакций

            if (goal.currency != null) {
                double targetAmountInCurrentCurrency = convertCurrency(goal.targetAmount, goal.currency, currentCurrency);
                double progressInCurrentCurrency = convertCurrency(totalProgress, goal.currency, currentCurrency);

                holder.textViewTargetAmount.setText("Целевая сумма: " + targetAmountInCurrentCurrency + " " + currentCurrency);
                holder.textViewProgress.setText("Прогресс: " + progressInCurrentCurrency + " " + currentCurrency);
                holder.progressBar.setProgress((int) (totalProgress / goal.targetAmount * 100)); // Обновление прогресс-бара
            } else {
                holder.textViewTargetAmount.setText("Целевая сумма: N/A");
                holder.textViewProgress.setText("Прогресс: N/A");
                holder.progressBar.setProgress(0);
            }
        });

        holder.textViewDateEnd.setText("Дата завершения: " + goal.dateEnd);
        holder.textViewNote.setText("Заметки: " + goal.note);
        holder.textViewCurrency.setText("Валюта: " + (goal.currency != null ? goal.currency : "N/A"));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGoalClick(goal);
            }
        });

        holder.deleteGoal.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Удалить цель")
                    .setMessage("Вы уверены, что хотите удалить эту цель?")
                    .setPositiveButton("Удалить", (dialog, which) -> {
                        goalRepository.deleteGoal(goal.id);
                        goalsList.remove(position);
                        notifyItemRemoved(position);
                        Log.d(TAG, "Цель удалена: " + goal.goalName); // Логирование удаления цели
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return goalsList.size();
    }

    // Метод для конвертации валют
    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        // Проверка на совпадение валют
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            return amount; // Если валюты совпадают или одна из валют null, возвращаем сумму без изменений
        }

        double rate = 1.0; // Начальное значение

        // Определяем коэффициент конвертации на основе валют
        if (fromCurrency.equals("USD")) {
            if (toCurrency.equals("EUR")) {
                rate = 0.85; // Примерный курс USD в EUR
            } else if (toCurrency.equals("RUB")) {
                rate = 70.0; // Примерный курс USD в RUB
            } else if (toCurrency.equals("BYN")) {
                rate = 2.6; // Примерный курс USD в BYN
            } else if (toCurrency.equals("UAH")) {
                rate = 27.0; // Примерный курс USD в UAH
            } else if (toCurrency.equals("PLN")) {
                rate = 3.7; // Примерный курс USD в PLN
            }
        } else if (fromCurrency.equals("EUR")) {
            if (toCurrency.equals("USD")) {
                rate = 1.18; // Примерный курс EUR в USD
            } else if (toCurrency.equals("RUB")) {
                rate = 82.0; // Примерный курс EUR в RUB
            } else if (toCurrency.equals("BYN")) {
                rate = 3.1; // Примерный курс EUR в BYN
            } else if (toCurrency.equals("UAH")) {
                rate = 31.0; // Примерный курс EUR в UAH
            } else if (toCurrency.equals("PLN")) {
                rate = 4.3; // Примерный курс EUR в PLN
            }
        } else if (fromCurrency.equals("RUB")) {
            if (toCurrency.equals("USD")) {
                rate = 0.014; // Примерный курс RUB в USD
            } else if (toCurrency.equals("EUR")) {
                rate = 0.012; // Примерный курс RUB в EUR
            } else if (toCurrency.equals("BYN")) {
                rate = 0.032; // Примерный курс RUB в BYN
            } else if (toCurrency.equals("UAH")) {
                rate = 0.36; // Примерный курс RUB в UAH
            } else if (toCurrency.equals("PLN")) {
                rate = 0.05; // Примерный курс RUB в PLN
            }
        } else if (fromCurrency.equals("BYN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.38; // Примерный курс BYN в USD
            } else if (toCurrency.equals("EUR")) {
                rate = 0.32; // Примерный курс BYN в EUR
            } else if (toCurrency.equals("RUB")) {
                rate = 31.0; // Примерный курс BYN в RUB
            } else if (toCurrency.equals("UAH")) {
                rate = 11.0; // Примерный курс BYN в UAH
            } else if (toCurrency.equals("PLN")) {
                rate = 1.4; // Примерный курс BYN в PLN
            }
        } else if (fromCurrency.equals("UAH")) {
            if (toCurrency.equals("USD")) {
                rate = 0.037; // Примерный курс UAH в USD
            } else if (toCurrency.equals("EUR")) {
                rate = 0.032; // Примерный курс UAH в EUR
            } else if (toCurrency.equals("RUB")) {
                rate = 2.8; // Примерный курс UAH в RUB
            } else if (toCurrency.equals("BYN")) {
                rate = 0.091; // Примерный курс UAH в BYN
            } else if (toCurrency.equals("PLN")) {
                rate = 0.12; // Примерный курс UAH в PLN
            }
        } else if (fromCurrency.equals("PLN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.27; // Примерный курс PLN в USD
            } else if (toCurrency.equals("EUR")) {
                rate = 0.23; // Примерный курс PLN в EUR
            } else if (toCurrency.equals("RUB")) {
                rate = 20.0; // Примерный курс PLN в RUB
            } else if (toCurrency.equals("BYN")) {
                rate = 0.71; // Примерный курс PLN в BYN
            } else if (toCurrency.equals("UAH")) {
                rate = 8.3; // Примерный курс PLN в UAH
            }
        }

        return amount * rate; // Возвращаем сконвертированную сумму
    }

    static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView textViewGoalName;
        TextView textViewTargetAmount;
        TextView textViewProgress;
        TextView textViewDateEnd;
        TextView textViewNote;
        TextView textViewCurrency;
        ImageView deleteGoal;
        ProgressBar progressBar;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewGoalName = itemView.findViewById(R.id.textViewGoalName);
            textViewTargetAmount = itemView.findViewById(R.id.textViewTargetAmount);
            textViewProgress = itemView.findViewById(R.id.textViewProgress);
            textViewDateEnd = itemView.findViewById(R.id.textViewDateEnd);
            textViewNote = itemView.findViewById(R.id.textViewNote);
            textViewCurrency = itemView.findViewById(R.id.textViewCurrency);
            deleteGoal = itemView.findViewById(R.id.deleteGoal);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
