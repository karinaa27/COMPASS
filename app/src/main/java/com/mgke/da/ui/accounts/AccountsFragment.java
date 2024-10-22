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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.AccountsAdapter;
import com.mgke.da.models.Account;
import com.mgke.da.repository.AccountRepository;
import com.mgke.da.repository.TransactionRepository;
import java.util.ArrayList;
import java.util.List;

public class AccountsFragment extends Fragment {
    private RecyclerView recyclerView;
    private AccountsAdapter accountsAdapter;
    private List<Account> accountList;
    private AccountRepository accountRepository;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_accounts, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewAccounts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        accountList = new ArrayList<>();
        TransactionRepository transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());
        accountRepository = new AccountRepository(FirebaseFirestore.getInstance());
        accountsAdapter = new AccountsAdapter(accountList, getContext(), transactionRepository, accountRepository, account -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("selectedAccount", account);
            bundle.putBoolean("isEditing", true);
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_add_account, bundle);
        });
        recyclerView.setAdapter(accountsAdapter);
        loadAccounts();
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
                accountList.clear();
                accountList.addAll(accounts);
                accountsAdapter.notifyDataSetChanged();
            }).exceptionally(e -> {
                return null;
            });
        }
    }

    private String getCurrentUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}