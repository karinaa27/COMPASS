package com.mgke.da.adapters;

import android.app.AlertDialog;
import android.content.Context;
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

    public void setOnGoalClickListener(OnGoalClickListener listener) {
        this.listener = listener;
    }


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

        transactionRepository.getTransactionsForGoalName(goal.goalName).thenAccept(transactions -> {
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

                holder.textViewTargetAmount.setText(String.format("Цель: %.2f", targetAmountInCurrentCurrency));
                holder.textViewProgress.setText(String.format("Прогресс: %.2f", progressInCurrentCurrency));

                holder.progressBar.setMax((int) goal.targetAmount);
                holder.progressBar.setProgress((int) totalProgress);
            } else {
                holder.textViewTargetAmount.setText("Сумма: N/A");
                holder.textViewProgress.setText("N/A");
                holder.progressBar.setProgress(0);
            }
        });

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = sdf.format(goal.dateEnd);
        holder.textViewDateEnd.setText(formattedDate);

        holder.textViewNote.setText(goal.note);
        holder.textViewCurrency.setText(goal.currency != null ? goal.currency : "N/A");
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGoalClick(goal);  // Передаем цель через интерфейс
            }
        });


        holder.deleteGoal.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.delete_goal_title)
                    .setMessage(R.string.delete_goal_message)
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        goalRepository.deleteGoal(goal.id);
                        goalsList.remove(position);
                        notifyItemRemoved(position);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return goalsList.size();
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
                rate = 8.3;
            }
        }

        return amount * rate;
    }

    public void updateGoals(List<Goal> updatedGoals) {
        this.goalsList = updatedGoals;
        notifyDataSetChanged();
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