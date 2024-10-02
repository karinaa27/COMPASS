package com.mgke.da.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Account;

import java.util.List;

public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.AccountViewHolder> {

    private List<Account> accounts;
    private Context context;

    public AccountsAdapter(List<Account> accounts, Context context) {
        this.accounts = accounts;
        this.context = context; // Сохраняем контекст для дальнейшего использования
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_account, parent, false); // Убедитесь, что этот файл разметки существует
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        Account account = accounts.get(position);
        holder.accountNameTextView.setText(account.accountName);
        holder.accountAmountTextView.setText(String.format("%s %.2f", account.currency, account.accountAmount));
        holder.incomeAmount.setText(String.format("%s 0,00", account.currency)); // Начальная сумма
        holder.expensesAmount.setText(String.format("%s 0,00", account.currency)); // Начальная сумма

        // Установка фона
        String backgroundName = account.background; // Получаем имя файла фона

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
            holder.itemView.setBackgroundResource(R.drawable.account_fon1); // Установите фон по умолчанию
        }
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
            accountNameTextView = itemView.findViewById(R.id.account_name); // Убедитесь, что ID совпадает с вашим
            accountAmountTextView = itemView.findViewById(R.id.account_amount); // Убедитесь, что ID совпадает с вашим
            incomeAmount = itemView.findViewById(R.id.incomeAmount); // Убедитесь, что ID совпадает с вашим
            expensesAmount = itemView.findViewById(R.id.expensesAmount); // Убедитесь, что ID совпадает с вашим
        }
    }

    public void updateAccounts(List<Account> newAccounts) {
        this.accounts.clear();
        this.accounts.addAll(newAccounts);
        notifyDataSetChanged();
    }
}