package com.mgke.da.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Goal;
import com.mgke.da.models.Transaction;
import com.mgke.da.repository.GoalRepository;
import com.mgke.da.repository.TransactionRepository;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    private List<Goal> goalsList;
    private Context context;
    private GoalRepository goalRepository;
    private TransactionRepository transactionRepository;
    private OnGoalClickListener listener;
    private String currentCurrency;
    private Fragment fragment;

    public interface OnGoalClickListener {
        void onGoalClick(Goal goal);
    }

    public GoalAdapter(List<Goal> goalsList, Context context, GoalRepository goalRepository,
                       TransactionRepository transactionRepository, String currentCurrency,
                       OnGoalClickListener listener, Fragment fragment) {
        this.goalsList = goalsList;
        this.context = context;
        this.goalRepository = goalRepository;
        this.transactionRepository = transactionRepository;
        this.currentCurrency = currentCurrency;
        this.listener = listener;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.goal_item, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        Goal goal = goalsList.get(position);
        holder.textViewGoalName.setText(goal.goalName);
        Log.d("GoalAdapter", "GoalAdapter call");
        transactionRepository.getTransactionsForGoalId(goal.id).thenAccept(transactions -> {
            double totalProgress = 0.0;
            for (Transaction transaction : transactions) {
                if (transaction.currency.equals(goal.currency)) {
                    totalProgress += transaction.amount;
                } else {
                    double convertedAmount = convertCurrency(transaction.amount, transaction.currency, goal.currency);
                    totalProgress += convertedAmount;
                }
            }

            if (goal.currency != null) {
                double targetAmountInCurrentCurrency = convertCurrency(goal.targetAmount, goal.currency, currentCurrency);
                double progressInCurrentCurrency = convertCurrency(totalProgress, goal.currency, currentCurrency);

                goal.progress = progressInCurrentCurrency;

                if (goal.progress >= targetAmountInCurrentCurrency) {
                    goal.isCompleted = true;
                } else {
                    goal.isCompleted = false;
                }

                goalRepository.updateGoalProgress(goal.id, goal.progress).addOnSuccessListener(aVoid -> {
                    goalRepository.updateGoalCompletionStatus(goal.id, goal.isCompleted);
                });

                holder.textViewTargetAmount.setText(String.format(context.getString(R.string.goal_target_amount), targetAmountInCurrentCurrency));
                holder.textViewProgress.setText(String.format(context.getString(R.string.goal_progress), progressInCurrentCurrency));

                holder.progressBar.setMax((int) goal.targetAmount);
                holder.progressBar.setProgress((int) totalProgress);
            } else {
                holder.textViewTargetAmount.setText("Сумма: N/A");
                holder.textViewProgress.setText("N/A");
                holder.progressBar.setProgress(0);
            }
        });

        // Здесь добавим дополнительную проверку для завершенной цели
        if (goal.isOverdue()) {
            holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.fon_setting_red));
        } else if (goal.isCompleted) {
            int backgroundResource = isDarkMode ? R.drawable.fon_setting_night : R.drawable.fon_setting;
            holder.itemView.setBackground(ContextCompat.getDrawable(context, backgroundResource));
        }
            else {
            // Убираем фон или оставляем стандартный для активных целей
            holder.itemView.setBackground(null);  // или какой-то стандартный фон
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String formattedDate = sdf.format(goal.dateEnd);
        holder.textViewDateEnd.setText(formattedDate);

        holder.textViewNote.setText(goal.note);
        holder.textViewCurrency.setText(goal.currency != null ? goal.currency : "N/A");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGoalClick(goal);  // Передаем цель через интерфейс
            }
        });

        // В методе удаления
        holder.deleteGoal.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.delete_goal_title)
                    .setMessage(R.string.delete_goal_message)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        ProgressDialog progressDialog = new ProgressDialog(context);
                        progressDialog.setMessage(context.getString(R.string.deleting_goal));
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        int currentPosition = holder.getAdapterPosition();
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            // Сначала удаляем цель из списка
                            goalsList.remove(currentPosition);
                            notifyItemRemoved(currentPosition);

                            // Затем удаляем её из базы данных
                            goalRepository.deleteGoal(goal.id)
                                    .thenRun(() -> {
                                        progressDialog.dismiss(); // Скрываем прогресс
                                    })
                                    .exceptionally(e -> {
                                        progressDialog.dismiss();
                                        Toast.makeText(context, context.getString(R.string.delete_goal_error), Toast.LENGTH_SHORT).show();
                                        return null;
                                    });
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return goalsList.size();
    }

    public void setGoals(List<Goal> goalsList) {
        this.goalsList = goalsList;
        notifyDataSetChanged();  // Уведомляет адаптер о том, что данные изменились, и нужно обновить UI
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
