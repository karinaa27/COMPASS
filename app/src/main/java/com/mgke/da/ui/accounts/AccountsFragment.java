package com.mgke.da.ui.accounts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}