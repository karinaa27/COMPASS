package com.mgke.da.ui.accounts;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;
import android.text.InputFilter;
import android.text.Spanned;
import com.google.firebase.auth.FirebaseUser;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.models.Account;
import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.AccountRepository;
import com.mgke.da.repository.PersonalDataRepository;
import java.util.Arrays;

public class AddAccountFragment extends Fragment {
    private String selectedBackground = null;
    private GridLayout iconSelectionGrid;
    private Spinner currencySpinner;
    private FirebaseAuth auth;
    private String[] currencies = {"BYN", "USD", "RUB", "UAH", "PLN", "EUR"};
    private boolean isEditingAccount = false;
    private Account accountToEdit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_account, container, false);
        ImageView backButton = view.findViewById(R.id.backButton);
        EditText accountNameEditText = view.findViewById(R.id.accountNameEditText);
        Button saveAccountButton = view.findViewById(R.id.saveAccountButton);
        TextView deleteAccountButton = view.findViewById(R.id.textViewDeleteTransaction);
        currencySpinner = view.findViewById(R.id.currencySpinner);
        iconSelectionGrid = view.findViewById(R.id.iconSelectionGrid);
        EditText accountAmountEditText = view.findViewById(R.id.accountAmountEditText);
        accountAmountEditText.setFilters(new InputFilter[]{new DecimalInputFilter()});
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);

        if (getArguments() != null) {
            accountToEdit = (Account) getArguments().getSerializable("selectedAccount");
            if (accountToEdit != null) {
                isEditingAccount = true;
                accountNameEditText.setText(accountToEdit.accountName);
                accountAmountEditText.setText(String.valueOf(accountToEdit.accountAmount));
                int position = Arrays.asList(currencies).indexOf(accountToEdit.currency);
                currencySpinner.setSelection(position >= 0 ? position : 0);
                selectedBackground = accountToEdit.background;
                currencySpinner.setVisibility(View.GONE);
                accountAmountEditText.setVisibility(View.GONE);
                deleteAccountButton.setVisibility(View.VISIBLE);
                highlightIconFromBackground(selectedBackground);
            }
        }

        for (int i = 0; i < iconSelectionGrid.getChildCount(); i++) {
            ImageView icon = (ImageView) iconSelectionGrid.getChildAt(i);
            icon.setOnClickListener(v -> highlightSelectedIcon(icon));
        }

        backButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_accounts);
        });

        deleteAccountButton.setOnClickListener(v -> {
            if (accountToEdit != null) {
                String accountId = accountToEdit.id;
                if (accountId != null) {
                    AccountRepository accountRepository = new AccountRepository(FirebaseFirestore.getInstance());
                    accountRepository.deleteAccount(accountId)
                            .thenRun(() -> {
                                Toast.makeText(getActivity(), R.string.account_deleted, Toast.LENGTH_SHORT).show();
                                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                                navController.navigate(R.id.navigation_accounts);
                            })
                            .exceptionally(e -> {
                                Toast.makeText(getActivity(), getString(R.string.error_deleting_account) + e.getMessage(), Toast.LENGTH_SHORT).show();
                                return null;
                            });
                } else {
                    Toast.makeText(getActivity(), R.string.error_account_id_not_available, Toast.LENGTH_SHORT).show();
                }
            }
        });

        saveAccountButton.setOnClickListener(v -> {
            String accountName = accountNameEditText.getText().toString().trim();
            String accountAmountString = accountAmountEditText.getText().toString().trim();
            if (accountToEdit != null) {
                boolean nameChanged = !accountName.equals(accountToEdit.accountName);
                boolean backgroundChanged = selectedBackground != null && !selectedBackground.equals(accountToEdit.background);
                if (accountName.isEmpty()) {
                    Toast.makeText(getActivity(), R.string.error_empty_account_name, Toast.LENGTH_SHORT).show();
                } else if (selectedBackground == null) {
                    Toast.makeText(getActivity(), R.string.error_select_background, Toast.LENGTH_SHORT).show();
                } else {
                    double accountAmount = parseAccountAmount(accountAmountString);
                    AccountRepository accountRepository = new AccountRepository(FirebaseFirestore.getInstance());
                    if (nameChanged) {
                        accountToEdit.accountName = accountName;
                    }
                    if (!accountAmountString.isEmpty()) {
                        accountToEdit.accountAmount = accountAmount;
                    }
                    if (backgroundChanged) {
                        accountToEdit.background = selectedBackground;
                    }
                    accountRepository.updateAccount(accountToEdit)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getActivity(), R.string.account_updated, Toast.LENGTH_SHORT).show();
                                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                                navController.navigate(R.id.navigation_accounts);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), getString(R.string.error_updating_account) + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            } else {
                if (accountName.isEmpty()) {
                    Toast.makeText(getActivity(), R.string.error_empty_account_name, Toast.LENGTH_SHORT).show();
                } else if (selectedBackground == null) {
                    Toast.makeText(getActivity(), R.string.error_select_background, Toast.LENGTH_SHORT).show();
                } else {
                    double accountAmount = parseAccountAmount(accountAmountString);
                    Account newAccount = new Account();
                    newAccount.accountName = accountName;
                    newAccount.accountAmount = accountAmount;
                    newAccount.userId = getCurrentUserId();
                    newAccount.currency = currencySpinner.getSelectedItem().toString();
                    newAccount.background = selectedBackground;
                    AccountRepository accountRepository = new AccountRepository(FirebaseFirestore.getInstance());
                    accountRepository.addAccount(newAccount)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getActivity(), R.string.account_added, Toast.LENGTH_SHORT).show();
                                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                                navController.navigate(R.id.navigation_accounts);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getActivity(), getString(R.string.error_adding_account) + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });

        auth = FirebaseAuth.getInstance();
        loadUserCurrency();
        return view;
    }

    private void loadUserCurrency() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
            personalDataRepository.getPersonalDataById(userId).thenAccept(this::loadUserCurrency).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });
        }
    }

    private void loadUserCurrency(PersonalData personalData) {
        String userCurrency = personalData.currency;
        int position = Arrays.asList(currencies).indexOf(userCurrency);
        currencySpinner.setSelection(position >= 0 ? position : 1);
    }

    private double parseAccountAmount(String amountString) {
        if (amountString.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(amountString);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private void highlightSelectedIcon(ImageView selectedIcon) {
        if (iconSelectionGrid != null) {
            for (int i = 0; i < iconSelectionGrid.getChildCount(); i++) {
                ImageView icon = (ImageView) iconSelectionGrid.getChildAt(i);
                if (icon.getId() == R.id.icon1) {
                    icon.setBackgroundResource(R.drawable.account_fon1);
                } else if (icon.getId() == R.id.icon2) {
                    icon.setBackgroundResource(R.drawable.account_fon2);
                } else if (icon.getId() == R.id.icon3) {
                    icon.setBackgroundResource(R.drawable.account_fon3);
                } else if (icon.getId() == R.id.icon4) {
                    icon.setBackgroundResource(R.drawable.account_fon4);
                } else if (icon.getId() == R.id.icon5) {
                    icon.setBackgroundResource(R.drawable.account_fon5);
                }
                icon.setScaleX(1.0f);
                icon.setScaleY(1.0f);
            }
        }
        if (selectedIcon.getId() == R.id.icon1) {
            selectedBackground = "account_fon1";
        } else if (selectedIcon.getId() == R.id.icon2) {
            selectedBackground = "account_fon2";
        } else if (selectedIcon.getId() == R.id.icon3) {
            selectedBackground = "account_fon3";
        } else if (selectedIcon.getId() == R.id.icon4) {
            selectedBackground = "account_fon4";
        } else if (selectedIcon.getId() == R.id.icon5) {
            selectedBackground = "account_fon5";
        }
        selectedIcon.setScaleX(1.2f);
        selectedIcon.setScaleY(1.2f);
    }

    private void highlightIconFromBackground(String background) {
        if (background != null) {
            ImageView iconToHighlight = null;
            switch (background) {
                case "account_fon1":
                    iconToHighlight = iconSelectionGrid.findViewById(R.id.icon1);
                    break;
                case "account_fon2":
                    iconToHighlight = iconSelectionGrid.findViewById(R.id.icon2);
                    break;
                case "account_fon3":
                    iconToHighlight = iconSelectionGrid.findViewById(R.id.icon3);
                    break;
                case "account_fon4":
                    iconToHighlight = iconSelectionGrid.findViewById(R.id.icon4);
                    break;
                case "account_fon5":
                    iconToHighlight = iconSelectionGrid.findViewById(R.id.icon5);
                    break;
            }
            if (iconToHighlight != null) {
                highlightSelectedIcon(iconToHighlight);
            }
        }
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = auth.getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    private static class DecimalInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source.length() == 0) {
                return null;
            }
            String resultingString = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());
            if (resultingString.equals(".")) {
                return "0.";
            }
            if (resultingString.matches("\\d*\\.\\d*")) {
                return null;
            }
            if (!resultingString.matches("^[0-9]*\\.?[0-9]*$")) {
                return "";
            }
            return null;
        }
    }
}