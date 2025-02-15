package com.mgke.da.ui.transactions;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.activity.LoginActivity;
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTransactionFragment extends Fragment {

    private boolean isRequestInProgress = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable convertCurrencyRunnable;
    private ProgressDialog progressDialog;
    private static final long DELAY_MS = 2000;
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
    private static final String TAG = "AddTransactionFragment";
    private static boolean areCategoriesCreated = false;

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

        if (!areCategoriesCreated) {
            loadCategoriesWithDefaults();
        }

        binding.nameGoal.setOnClickListener(v -> showSelectGoalDialog());
        binding.textViewCurrencyLabel.setOnClickListener(v -> showSelectCurrencyDialog());
        binding.incomeBtn.setOnClickListener(v -> setTransactionType(INCOME));
        binding.expenseBtn.setOnClickListener(v -> setTransactionType(EXPENSE));
        binding.nameAccount.setOnClickListener(v -> showSelectAccountDialog());
        binding.date.setOnClickListener(v -> showDatePickerDialog());
        binding.calendarBtn.setOnClickListener(v -> showDatePickerDialog());
        binding.currencyTextView.setOnClickListener(v -> showSelectCurrencyDialog());
        binding.SaveTransactionBtn.setOnClickListener(v -> saveTransaction());
        binding.addCategory.setOnClickListener(v -> {
            navController.navigate(R.id.action_addTransactionFragment_to_addCategoryFragment);
        });

        binding.close.setOnClickListener(v -> navController.popBackStack());

        binding.editTextCurrency.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                handler.removeCallbacks(convertCurrencyRunnable);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handler.removeCallbacks(convertCurrencyRunnable);
                convertCurrencyRunnable = new Runnable() {
                    @Override
                    public void run() {
                        convertCurrencyToRUB();
                    }
                };
                handler.postDelayed(convertCurrencyRunnable, DELAY_MS);
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
        ImageView buttonAddGoal = dialogView.findViewById(R.id.buttonAddGoal);

        GoalRepository goalRepository = new GoalRepository(FirebaseFirestore.getInstance());
        String currentUserId = getCurrentUserId();

        goalRepository.getAllGoal().thenAccept(goals -> {
            radioGroupGoals.removeAllViews();

            RadioButton resetSelectionButton = new RadioButton(getContext());
            resetSelectionButton.setText(getString(R.string.no_goal_selected));
            resetSelectionButton.setId(View.generateViewId());
            radioGroupGoals.addView(resetSelectionButton);

            resetSelectionButton.setOnClickListener(v -> {
                selectedGoalId = null;
                binding.nameGoal.setText("");
            });

            for (Goal goal : goals) {
                if (goal.userId.equals(currentUserId)) {
                    RadioButton radioButton = new RadioButton(getContext());
                    radioButton.setText(goal.goalName);
                    radioButton.setId(View.generateViewId());
                    radioGroupGoals.addView(radioButton);

                    radioButton.setOnClickListener(v -> {
                        selectedGoalId = goal.id;
                    });
                }
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
                if (!selectedGoalName.equals(getString(R.string.no_goal_selected))) {
                    binding.nameGoal.setText(selectedGoalName);
                } else {
                    selectedGoalId = null;
                }
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), getString(R.string.goal_label_select), Toast.LENGTH_SHORT).show();
            }
        });

        buttonAddGoal.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.AddGoalFragment);
            dialog.dismiss();
        });

        dialog.show();
    }


    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
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
        ImageView addButton = dialogView.findViewById(R.id.buttonAddAccount);
        TextView noAccountsMessage = dialogView.findViewById(R.id.noAccountsMessage); // Ссылка на TextView

        accountRepository.getAccountsByUserId(userId).thenAccept(accounts -> {
            radioGroup.removeAllViews();

            if (accounts.isEmpty()) {
                // Если счетов нет, показываем сообщение
                noAccountsMessage.setVisibility(View.VISIBLE);
            } else {
                noAccountsMessage.setVisibility(View.GONE); // Если счета есть, скрываем сообщение

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

        addButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.action_navigation_home_to_addAccountFragment);
            dialog.dismiss();
        });

        dialog.show();
    }


    private void loadCategoriesWithDefaults() {
        Log.d(TAG, "loadCategoriesWithDefaults: Вызван метод для загрузки категорий с дефолтами");

        categoryRepository.getAllCategories(userId).whenComplete((categories, throwable) -> {
            if (throwable != null) {
                Log.e(TAG, "Ошибка при получении категорий", throwable);
                return;
            }

            if (categories == null || categories.isEmpty()) {
                Log.d(TAG, "Категории не найдены, создаем дефолтные");
                CompletableFuture<Void> incomeCategories = categoryRepository.createDefaultCategories(userId, "income");
                CompletableFuture<Void> expenseCategories = categoryRepository.createDefaultCategories(userId, "expense");

                CompletableFuture.allOf(incomeCategories, expenseCategories).whenComplete((v, ex) -> {
                    if (ex != null) {
                        Log.e(TAG, "Ошибка при создании категорий", ex);
                    } else {
                        Log.d(TAG, "Дефолтные категории успешно созданы");
                        areCategoriesCreated = true;
                        loadCategoriesBasedOnTransactionType();
                    }
                });
            } else {
                Log.d(TAG, "Категории уже существуют, обновляем флаг");
                // Если категории уже существуют, обновляем флаг
                areCategoriesCreated = true;
                loadCategoriesBasedOnTransactionType();
            }
        });
    }

    private void loadCategoriesBasedOnTransactionType() {
        if (currentTransactionType.equals(INCOME)) {
            loadIncomeCategories();
        } else {
            loadExpenseCategories();
        }
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

        if (isRequestInProgress) {
            return;
        }

        String inputAmountStr = binding.editTextCurrency.getText().toString();

        if (!inputAmountStr.isEmpty()) {
            try {
                double inputAmount = Double.parseDouble(inputAmountStr);

                if (inputAmount <= 0) {
                    return;
                }

                String selectedCurrency = binding.textViewCurrencyLabel.getText().toString();

                if (selectedCurrency.isEmpty()) {
                    binding.sum.setText(getString(R.string.currency_error_select));
                    return;
                }

                String apiKey = "87986aa7d23ce4bca64d81bbdd909517";

                isRequestInProgress = true;

                ApiClient.convertCurrency(apiKey, selectedCurrency, defaultCurrency, inputAmount)
                        .enqueue(new Callback<ConversionResponse>() {
                            @Override
                            public void onResponse(Call<ConversionResponse> call, Response<ConversionResponse> response) {

                                isRequestInProgress = false;

                                if (response.isSuccessful() && response.body() != null) {
                                    double convertedAmount = response.body().getResult();

                                    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
                                    numberFormat.setMaximumFractionDigits(2);
                                    numberFormat.setMinimumFractionDigits(2);

                                    String formattedAmount = numberFormat.format(convertedAmount);

                                    binding.sum.setText(formattedAmount);
                                } else {
                                    binding.sum.setText(getString(R.string.currency_error_request));
                                }
                            }

                            @Override
                            public void onFailure(Call<ConversionResponse> call, Throwable t) {

                                isRequestInProgress = false;
                                binding.sum.setText(getString(R.string.currency_error_failure, t.getMessage()));
                            }
                        });
            } catch (NumberFormatException e) {

                binding.sum.setText(getString(R.string.currency_error_invalid_amount));
            }
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
        Log.d(TAG, "loadIncomeCategories: Вызван метод загрузки категорий доходов");
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
        Log.d(TAG, "loadExpenseCategories: Вызван метод загрузки категорий расходов");
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
        showLoadingDialog();
        String type = currentTransactionType;
        String category = categoryAdapter.getSelectedCategory();
        String dateStr = binding.date.getText() != null ? binding.date.getText().toString() : "";
        String amountStr = binding.sum.getText() != null ? binding.sum.getText().toString() : "";
        String currency = binding.currencyTextView.getText() != null ? binding.currencyTextView.getText().toString() : "";

        if (selectedAccountId == null || selectedAccountId.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_select_account, Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        if (category == null || category.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_select_category, Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        if (dateStr.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_select_date, Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        if (amountStr.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_enter_amount, Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        double amount;
        try {
            amountStr = amountStr.replace(',', '.');
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), R.string.toast_invalid_amount, Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        amount = type.equals(EXPENSE) ? -Math.abs(amount) : Math.abs(amount);
        Date date = parseDate(dateStr);
        if (date == null) {
            Toast.makeText(getContext(), R.string.toast_invalid_date, Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        Transaction transaction = createTransaction(type, category, selectedAccountId, date, amount, currency, selectedGoalId);
        TransactionRepository transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());

        transactionRepository.addTransaction(transaction)
                .addOnSuccessListener(transactionId -> {
                    if (!isAdded()) return;
                    transaction.id = transactionId;

                    if (selectedGoalId != null && !selectedGoalId.isEmpty()) {
                        GoalRepository goalRepository = new GoalRepository(FirebaseFirestore.getInstance());
                        goalRepository.getGoalById(selectedGoalId).thenAccept(goal -> {
                            if (goal != null) {
                                transactionRepository.getTransactionsForGoalId(goal.id).thenAccept(transactions -> {
                                    double totalProgress = 0.0;
                                    for (Transaction t : transactions) {
                                        if (t.currency.equals(goal.currency)) {
                                            totalProgress += t.amount;
                                        } else {
                                            double convertedAmount = convertCurrency(t.amount, t.currency, goal.currency);
                                            totalProgress += convertedAmount;
                                        }
                                    }

                                    goal.progress = totalProgress;
                                    goal.isCompleted = totalProgress >= goal.targetAmount;

                                    goalRepository.updateGoalProgress(goal.id, totalProgress).addOnSuccessListener(aVoid -> {
                                        goalRepository.updateGoalCompletionStatus(goal.id, goal.isCompleted);
                                        Toast.makeText(getContext(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();
                                        clearFields();
                                        NavController navController = Navigation.findNavController(getView());
                                        navController.popBackStack();
                                    }).addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "R.string.toast_update_goal_failure", Toast.LENGTH_SHORT).show();
                                    });
                                });
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), getString(R.string.toast_save_success), Toast.LENGTH_SHORT).show();
                        clearFields();
                        NavController navController = Navigation.findNavController(getView());
                        navController.popBackStack();
                    }

                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), getString(R.string.toast_save_failure), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });
    }

    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            return amount;
        }

        double rate = 1.0;

        if (fromCurrency.equals("USD")) {
            if (toCurrency.equals("EUR")) {
                rate = 0.97;
            } else if (toCurrency.equals("RUB")) {
                rate = 97.83;
            } else if (toCurrency.equals("BYN")) {
                rate = 3.38;
            } else if (toCurrency.equals("UAH")) {
                rate = 41.69;
            } else if (toCurrency.equals("PLN")) {
                rate = 4.0;
            }
        } else if (fromCurrency.equals("EUR")) {
            if (toCurrency.equals("USD")) {
                rate = 1.03;
            } else if (toCurrency.equals("RUB")) {
                rate = 101.02;
            } else if (toCurrency.equals("BYN")) {
                rate = 3.49;
            } else if (toCurrency.equals("UAH")) {
                rate = 42.95;
            } else if (toCurrency.equals("PLN")) {
                rate = 4.2;
            }
        } else if (fromCurrency.equals("RUB")) {
            if (toCurrency.equals("USD")) {
                rate = 0.01;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.01;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.035;
            } else if (toCurrency.equals("UAH")) {
                rate = 0.43;
            } else if (toCurrency.equals("PLN")) {
                rate = 0.04;
            }
        } else if (fromCurrency.equals("BYN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.3;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.3;
            } else if (toCurrency.equals("RUB")) {
                rate = 28.93;
            } else if (toCurrency.equals("UAH")) {
                rate = 12.34;
            } else if (toCurrency.equals("PLN")) {
                rate = 1.2;
            }
        } else if (fromCurrency.equals("UAH")) {
            if (toCurrency.equals("USD")) {
                rate = 0.024;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.023;
            } else if (toCurrency.equals("RUB")) {
                rate = 2.33;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.08;
            } else if (toCurrency.equals("PLN")) {
                rate = 0.1;
            }
        } else if (fromCurrency.equals("PLN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.25;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.24;
            } else if (toCurrency.equals("RUB")) {
                rate = 24.1;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.83;
            } else if (toCurrency.equals("UAH")) {
                rate = 10.26;
            }
        }

        return amount * rate;
    }

    private Transaction createTransaction(String type, String category, String accountId, Date date, double amount, String currency, String goalId) {
        int categoryImage = categoryAdapter.getSelectedCategoryImage();
        int categoryColor = categoryAdapter.getSelectedCategoryColor();
        String accountBackground = getAccountBackground(accountId);

        Transaction transaction = new Transaction();
        transaction.type = type;
        transaction.category = category;
        transaction.accountId = accountId;
        transaction.date = date;
        transaction.amount = amount;
        transaction.currency = currency;
        transaction.userId = userId;
        transaction.goalId = goalId;
        transaction.categoryImage = categoryImage;
        transaction.categoryColor = categoryColor;
        transaction.accountBackground = accountBackground;

        return transaction;
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(getString(R.string.load));
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
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
    @Override
    public void onResume() {
        super.onResume();
        binding.recyclerViewCategories.setAdapter(categoryAdapter);
        loadUserCurrencyFromDatabase();

    }

    private void loadUserCurrencyFromDatabase() {
        if (currentUser != null) {
            PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
            personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
                if (personalData != null) {
                    loadUserCurrency(personalData);
                }
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });
        }
    }
}