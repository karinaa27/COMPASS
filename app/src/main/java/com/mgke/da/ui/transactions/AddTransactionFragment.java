package com.mgke.da.ui.transactions;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.SimpleCategoryAdapter;
import com.mgke.da.models.Account;
import com.mgke.da.models.ConversionResponse;
import com.mgke.da.models.Goal;
import com.mgke.da.models.Transaction;
import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.CategoryRepository;
import com.mgke.da.repository.AccountRepository;
import com.mgke.da.databinding.FragmentAddTransactionBinding;
import com.mgke.da.api.ApiClient;
import com.mgke.da.repository.GoalRepository;
import com.mgke.da.repository.PersonalDataRepository;
import com.mgke.da.repository.TransactionRepository;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionFragment extends Fragment {

    private FragmentAddTransactionBinding binding;
    private String defaultCurrency;
    public static final String INCOME = "DOHOD";
    public static final String EXPENSE = "RACHOD";
    private String selectedGoalName;
    private SimpleCategoryAdapter categoryAdapter;
    private CategoryRepository categoryRepository;
    private AccountRepository accountRepository;
    private FirebaseUser currentUser;
    private String userId;
    private String selectedAccountId = null;
    private String currentTransactionType = INCOME;
    private FirebaseAuth auth;
    private String selectedGoalId;

    public AddTransactionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
            personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
                if (personalData != null) {
                    loadUserCurrency(personalData);
                }
            }).exceptionally(e -> {
                return null;
            });
        }
    }

    private void loadUserCurrency(PersonalData personalData) {
        if (personalData.currency != null && !personalData.currency.isEmpty()) {
            defaultCurrency = personalData.currency;
            binding.currencyTextView.setText(defaultCurrency);
        } else {
            defaultCurrency = "USD";
            binding.currencyTextView.setText("");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = Navigation.findNavController(view);
        binding.recyclerViewCategories.setLayoutManager(new GridLayoutManager(getContext(), 4));
        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());
        accountRepository = new AccountRepository(FirebaseFirestore.getInstance());

        binding.nameGoal.setVisibility(View.GONE);
        binding.textViewCurrencyLabel.setVisibility(View.GONE);
        setTransactionType(INCOME);

        binding.nameGoal.setOnClickListener(v -> showSelectGoalDialog());
        binding.textViewCurrencyLabel.setOnClickListener(v -> showSelectCurrencyDialog());
        binding.incomeBtn.setOnClickListener(v -> setTransactionType(INCOME));
        binding.expenseBtn.setOnClickListener(v -> setTransactionType(EXPENSE));
        binding.nameAccount.setOnClickListener(v -> showSelectAccountDialog());
        binding.date.setOnClickListener(v -> showDatePickerDialog());
        binding.calendarBtn.setOnClickListener(v -> showDatePickerDialog());
        binding.currencyTextView.setOnClickListener(v -> showSelectCurrencyDialog());
        binding.SaveTransactionBtn.setOnClickListener(v -> saveTransaction());

        binding.close.setOnClickListener(v -> navController.popBackStack());

        binding.editTextCurrency.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                convertCurrencyToRUB();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void showSelectGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_goal, null);

        RadioGroup radioGroupGoals = dialogView.findViewById(R.id.radioGroupGoals);
        Button buttonSelectGoal = dialogView.findViewById(R.id.buttonSelectGoal);

        GoalRepository goalRepository = new GoalRepository(FirebaseFirestore.getInstance());

        goalRepository.getAllGoal().thenAccept(goals -> {
            radioGroupGoals.removeAllViews();
            for (Goal goal : goals) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(goal.goalName);
                radioButton.setId(View.generateViewId());
                radioGroupGoals.addView(radioButton);
                radioButton.setOnClickListener(v -> {
                    selectedGoalId = goal.id;
                    onGoalSelected(goal.goalName);
                });
            }
        }).exceptionally(e -> {
            return null;
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        buttonSelectGoal.setOnClickListener(v -> {
            int selectedId = radioGroupGoals.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                String selectedGoalName = selectedRadioButton.getText().toString();
                onGoalSelected(selectedGoalName);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), getString(R.string.goal_label_select), Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showDatePickerDialog() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    binding.date.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showSelectAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_account, null);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupAccounts);
        Button buttonSelectAccount = dialogView.findViewById(R.id.buttonSelectAccount);

        accountRepository.getAccountsByUserId(userId).thenAccept(accounts -> {
            radioGroup.removeAllViews();
            for (Account account : accounts) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(account.accountName);
                radioButton.setId(View.generateViewId());
                radioGroup.addView(radioButton);

                if (selectedAccountId != null && selectedAccountId.equals(account.id)) {
                    radioButton.setChecked(true);
                }

                radioButton.setOnClickListener(v -> {
                    selectedAccountId = account.id;
                });
            }
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        buttonSelectAccount.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                String selectedAccountName = selectedRadioButton.getText().toString();
                binding.nameAccount.setText(selectedAccountName);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    private void showSelectCurrencyDialog() {
        String[] currencies = {"BYN", "USD", "RUB", "UAH", "PLN", "EUR"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getString(R.string.select_currency))
                .setItems(currencies, (dialog, which) -> {
                    String selectedCurrency = currencies[which];
                    String amountStr = binding.sum.getText().toString();
                    if (!amountStr.isEmpty()) {
                        binding.editTextCurrency.setText(amountStr);
                        binding.sum.setText("");
                    }
                    binding.textViewCurrencyLabel.setText(selectedCurrency);
                    binding.currencyTextView.setText(defaultCurrency);
                    updateCurrencyVisibility(selectedCurrency.equals(defaultCurrency));
                    convertCurrencyToRUB();
                });
        builder.show();
    }

    private void updateCurrencyVisibility(boolean isDefault) {
        if (isDefault) {
            binding.textViewCurrencyLabel.setVisibility(View.GONE);
            binding.textViewEquals.setVisibility(View.GONE);
            binding.editTextCurrency.setVisibility(View.GONE);
            binding.sum.setEnabled(true);
            binding.sum.setFocusable(true);
            binding.sum.setClickable(true);
            binding.currencyTextView.setTextColor(Color.parseColor("#FFBB86FC"));
            binding.currencyTextView.setClickable(true);
        } else {
            binding.textViewCurrencyLabel.setVisibility(View.VISIBLE);
            binding.textViewEquals.setVisibility(View.VISIBLE);
            binding.editTextCurrency.setVisibility(View.VISIBLE);
            binding.sum.setEnabled(false);
            binding.sum.setFocusable(true);
            binding.sum.setClickable(true);
            binding.currencyTextView.setTextColor(Color.BLACK);
            binding.currencyTextView.setClickable(false);
        }
    }

    private void convertCurrencyToRUB() {
        String inputAmountStr = binding.editTextCurrency.getText().toString();
        if (!inputAmountStr.isEmpty()) {
            double inputAmount = Double.parseDouble(inputAmountStr);
            if (inputAmount <= 0) {
                Toast.makeText(getContext(), "", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedCurrency = binding.textViewCurrencyLabel.getText().toString();
            String apiKey = "87986aa7d23ce4bca64d81bbdd909517";
            ApiClient.convertCurrency(apiKey, selectedCurrency, defaultCurrency, inputAmount).enqueue(new Callback<ConversionResponse>() {
                @Override
                public void onResponse(Call<ConversionResponse> call, Response<ConversionResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        double convertedAmount = response.body().getResult();
                        DecimalFormat df = new DecimalFormat("#.00");
                        String formattedAmount = df.format(convertedAmount);
                        binding.sum.setText(formattedAmount);
                    } else {
                        binding.sum.setText("");
                    }
                }

                @Override
                public void onFailure(Call<ConversionResponse> call, Throwable t) {
                    binding.sum.setText("Ошибка: " + t.getMessage());
                }
            });
        } else {
            binding.sum.setText("");
        }
    }

    private void setTransactionType(String type) {
        currentTransactionType = type;
        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (type.equals(INCOME)) {
            binding.incomeBtn.setBackgroundResource(R.drawable.transaction_add_income_selector);
            binding.expenseBtn.setBackgroundResource(R.drawable.transaction_add_default_selector);
            binding.incomeBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.parseColor("#00C853"));
            binding.expenseBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            binding.goal.setVisibility(View.VISIBLE);
            binding.nameGoal.setVisibility(View.VISIBLE);
            loadIncomeCategories();
        } else if (type.equals(EXPENSE)) {
            binding.incomeBtn.setBackgroundResource(R.drawable.transaction_add_default_selector);
            binding.expenseBtn.setBackgroundResource(R.drawable.transaction_add_expence_selector);
            binding.incomeBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            binding.expenseBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.RED);
            binding.goal.setVisibility(View.GONE);
            binding.nameGoal.setVisibility(View.GONE);
            loadExpenseCategories();
        }
    }

    private void loadIncomeCategories() {
        categoryRepository.getAllCategory(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new SimpleCategoryAdapter(getContext(), categories, categoryRepository, true);
                    binding.recyclerViewCategories.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
            }
        });
    }

    private void loadExpenseCategories() {
        categoryRepository.getAllExpenseCategories(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new SimpleCategoryAdapter(getContext(), categories, categoryRepository, true);
                    binding.recyclerViewCategories.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
            }
        });
    }

    private void saveTransaction() {
        if (!isAdded()) return;

        String type = currentTransactionType;
        String category = categoryAdapter.getSelectedCategory();
        String account = binding.nameAccount.getText() != null ? binding.nameAccount.getText().toString().trim() : "";
        String dateStr = binding.date.getText() != null ? binding.date.getText().toString() : "";
        String amountStr = binding.sum.getText() != null ? binding.sum.getText().toString() : "";
        String currency = binding.currencyTextView.getText() != null ? binding.currencyTextView.getText().toString() : "";

        if (account.isEmpty() || account.equals(getString(R.string.select_account))) {
            Toast.makeText(getContext(), R.string.toast_select_account, Toast.LENGTH_SHORT).show();
            return;
        }

        if (category == null || category.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_select_category, Toast.LENGTH_SHORT).show();
            return;
        }

        if (dateStr.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_select_date, Toast.LENGTH_SHORT).show();
            return;
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_enter_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amountStr = amountStr.replace(',', '.');
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.toast_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        amount = type.equals(EXPENSE) ? -Math.abs(amount) : Math.abs(amount);
        Date date = parseDate(dateStr);
        if (date == null) {
            Toast.makeText(getContext(), R.string.toast_invalid_date, Toast.LENGTH_SHORT).show();
            return;
        }

        String nameGoal = binding.nameGoal.getText() != null ? binding.nameGoal.getText().toString().trim() : "";
        Transaction transaction = createTransaction(type, category, account, date, amount, currency, nameGoal);
        TransactionRepository transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());

        transactionRepository.addTransaction(transaction)
                .addOnSuccessListener(transactionId -> {
                    if (!isAdded()) return;
                    transaction.id = transactionId;
                    if (!nameGoal.isEmpty()) {
                    }
                    Toast.makeText(getContext(), getString(R.string.toast_save_success, transactionId), Toast.LENGTH_SHORT).show();
                    clearFields();
                    NavController navController = Navigation.findNavController(getView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), getString(R.string.toast_save_failure, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void onGoalSelected(String goalName) {
        this.selectedGoalName = goalName;
        binding.nameGoal.setText(goalName);
    }

    private Transaction createTransaction(String type, String category, String account, Date date, double amount, String currency, String nameGoal) {
        int categoryImage = categoryAdapter.getSelectedCategoryImage();
        int categoryColor = categoryAdapter.getSelectedCategoryColor();
        String accountBackground = getAccountBackground(account);

        Transaction transaction = new Transaction();
        transaction.type = type;
        transaction.category = category;
        transaction.account = account;
        transaction.date = date;
        transaction.amount = amount;
        transaction.currency = currency;
        transaction.userId = userId;
        transaction.nameGoal = nameGoal;
        transaction.categoryImage = categoryImage;
        transaction.categoryColor = categoryColor;
        transaction.accountBackground = accountBackground;

        return transaction;
    }

    private String getAccountBackground(String accountName) {
        if (accountName == null) return "account_fon1";

        switch (accountName) {
            case "Счет 1":
                return "account_fon1";
            case "Счет 2":
                return "account_fon2";
            case "Счет 3":
                return "account_fon3";
            case "Счет 4":
                return "account_fon4";
            case "Счет 5":
                return "account_fon5";
            default:
                return "account_fon1";
        }
    }

    private Date parseDate(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
    private void clearFields() {
        binding.sum.setText("");
        binding.date.setText("");
        binding.nameAccount.setText("");
        binding.editTextCurrency.setText("");
        binding.currencyTextView.setText(defaultCurrency);
        updateCurrencyVisibility(true);
    }
}