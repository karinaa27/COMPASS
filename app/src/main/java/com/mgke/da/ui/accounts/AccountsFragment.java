package com.mgke.da.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.AccountsAdapter;
import com.mgke.da.models.Account;
import com.mgke.da.repository.AccountRepository;
import java.util.ArrayList;
import java.util.List;

public class AccountsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AccountsAdapter accountsAdapter;
    private List<Account> accountList; // Список для хранения аккаунтов
    private AccountRepository accountRepository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts, container, false);

        // Инициализация RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewAccounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Инициализация списка и адаптера с контекстом
        accountList = new ArrayList<>();
        accountsAdapter = new AccountsAdapter(accountList, getContext()); // Передаем контекст
        recyclerView.setAdapter(accountsAdapter);

        // Инициализация репозитория
        accountRepository = new AccountRepository(FirebaseFirestore.getInstance());

        // Загрузка счетов
        loadAccounts();

        // Настройка кнопки добавления счета
        Button addAccountButton = view.findViewById(R.id.add_account_button);
        addAccountButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_add_account);
        });

        return view;
    }

    private void loadAccounts() {
        accountRepository.getAllAccount().thenAccept(accounts -> {
            accountList.clear(); // Очистка списка перед добавлением новых данных
            accountList.addAll(accounts);
            accountsAdapter.notifyDataSetChanged(); // Уведомляем адаптер об изменениях
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Обнуляем ссылки на элементы, если они были использованы
        recyclerView.setAdapter(null);
    }
}