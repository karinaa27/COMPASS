package com.mgke.da.ui.transactions;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.CategoryAdapter;
import com.mgke.da.models.Account;
import com.mgke.da.repository.CategoryRepository;
import com.mgke.da.repository.AccountRepository;
import com.mgke.da.databinding.FragmentAddTransactionBinding;

import java.util.List;

public class AddTransactionFragment extends Fragment {

    private FragmentAddTransactionBinding binding;
    public static final String INCOME = "DOHOD";
    public static final String EXPENSE = "RACHOD";

    private CategoryAdapter categoryAdapter;
    private CategoryRepository categoryRepository;
    private AccountRepository accountRepository; // Для работы со счетами
    private FirebaseUser currentUser;
    private String userId;

    public AddTransactionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Инициализация привязки
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        // Настройка обработчика для кнопки закрытия
        ImageView closeButton = binding.close;
        closeButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(R.id.navigation_home);
        });

        // Инициализация RecyclerView
        binding.recyclerViewCategories.setLayoutManager(new GridLayoutManager(getContext(), 4));
        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());
        accountRepository = new AccountRepository(FirebaseFirestore.getInstance()); // Инициализация репозитория счетов

        // Настройка обработчиков для кнопок дохода и расхода
        binding.incomeBtn.setOnClickListener(v -> setTransactionType(INCOME));
        binding.expenseBtn.setOnClickListener(v -> setTransactionType(EXPENSE));

        // Обработчик для выбора счета
        binding.nameAccount.setOnClickListener(v -> showSelectAccountDialog());

        return view;
    }

    private void showSelectAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_account, null);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupAccounts);
        Button buttonSelectAccount = dialogView.findViewById(R.id.buttonSelectAccount);

        // Загрузка счетов и добавление их в RadioGroup
        accountRepository.getAllAccount().thenAccept(accounts -> {
            radioGroup.removeAllViews(); // Очистка предыдущих записей
            for (Account account : accounts) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(account.accountName); // Используем accountName из вашего класса Account
                radioButton.setId(View.generateViewId());
                radioGroup.addView(radioButton);
            }
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        buttonSelectAccount.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                String selectedAccountName = selectedRadioButton.getText().toString();
                binding.nameAccount.setText(selectedAccountName); // Обновление TextView с выбранным счетом
                dialog.dismiss();
            } else {
                // Обработка случая, если ничего не выбрано
            }
        });

        dialog.show();
    }

    private void setTransactionType(String type) {
        if (type.equals(INCOME)) {
            binding.incomeBtn.setBackground(getContext().getDrawable(R.drawable.transaction_add_income_selector));
            binding.expenseBtn.setBackground(getContext().getDrawable(R.drawable.transaction_add_default_selector));
            binding.expenseBtn.setTextColor(getContext().getColor(R.color.black));
            binding.incomeBtn.setTextColor(getContext().getColor(R.color.black));
            loadIncomeCategories();
        } else {
            binding.incomeBtn.setBackground(getContext().getDrawable(R.drawable.transaction_add_default_selector));
            binding.expenseBtn.setBackground(getContext().getDrawable(R.drawable.transaction_add_expence_selector));
            binding.incomeBtn.setTextColor(getContext().getColor(R.color.black));
            binding.expenseBtn.setTextColor(getContext().getColor(R.color.black));
            loadExpenseCategories();
        }
    }

    private void loadIncomeCategories() {
        categoryRepository.getAllCategory(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryAdapter(getContext(), categories, categoryRepository);
                    binding.recyclerViewCategories.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
            } else {
                // Обработка случая, если категории не найдены
            }
        }).exceptionally(e -> {
            // Обработка ошибок
            return null;
        });
    }

    private void loadExpenseCategories() {
        categoryRepository.getAllExpenseCategories(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryAdapter(getContext(), categories, categoryRepository);
                    binding.recyclerViewCategories.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
            } else {
                // Обработка случая, если категории не найдены
            }
        }).exceptionally(e -> {
            // Обработка ошибок
            return null;
        });
    }
}