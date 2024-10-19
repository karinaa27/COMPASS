package com.mgke.da.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Goal;
import java.util.List;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    private List<Goal> goalsList;

    public GoalAdapter(List<Goal> goalsList) {
        this.goalsList = goalsList;
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
        holder.textViewTargetAmount.setText("Целевая сумма: " + goal.targetAmount);
        holder.textViewProgress.setText("Прогресс: " + goal.progress);
        holder.progressBar.setMax((int) goal.targetAmount);
        holder.progressBar.setProgress((int) goal.progress);
        holder.textViewDateEnd.setText("Дата завершения: " + goal.dateEnd);
        holder.textViewNote.setText("Заметки: " + goal.note);
    }

    @Override
    public int getItemCount() {
        return goalsList.size();
    }

    public static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView textViewGoalName;
        TextView textViewTargetAmount;
        TextView textViewProgress;
        ProgressBar progressBar;
        TextView textViewDateEnd;
        TextView textViewNote;

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewGoalName = itemView.findViewById(R.id.textViewGoalName);
            textViewTargetAmount = itemView.findViewById(R.id.textViewTargetAmount);
            textViewProgress = itemView.findViewById(R.id.textViewProgress);
            progressBar = itemView.findViewById(R.id.progressBar);
            textViewDateEnd = itemView.findViewById(R.id.textViewDateEnd);
            textViewNote = itemView.findViewById(R.id.textViewNote);
        }
    }
}