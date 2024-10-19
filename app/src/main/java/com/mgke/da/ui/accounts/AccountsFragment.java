package com.mgke.da.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
<<<<<<< HEAD
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
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

        // Загрузка счетов для текущего пользователя
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
        String userId = getCurrentUserId();
        if (userId != null) {
            accountRepository.getAccountsByUserId(userId).thenAccept(accounts -> {
                accountList.clear(); // Очистка списка перед добавлением новых данных
                accountList.addAll(accounts);
                accountsAdapter.notifyDataSetChanged(); // Уведомляем адаптер об изменениях
            }).exceptionally(e -> {
                e.printStackTrace(); // Логируем ошибку
                return null;
            });
        }
    }

    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null; // Возвращает ID пользователя или null, если не аутентифицирован
=======
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.mgke.da.databinding.FragmentAccountsBinding;

public class AccountsFragment extends Fragment {

    private FragmentAccountsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AccountsViewModel statsViewModel =
                new ViewModelProvider(this).get(AccountsViewModel.class);

        binding = FragmentAccountsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textAccounts;
        statsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
<<<<<<< HEAD
        // Обнуляем ссылки на элементы, если они были использованы
        recyclerView.setAdapter(null);
=======
        binding = null;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    }
}