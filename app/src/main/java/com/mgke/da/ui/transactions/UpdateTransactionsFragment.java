package com.mgke.da.ui.transactions;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import com.mgke.da.models.Goal;
import com.mgke.da.repository.GoalRepository;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.SimpleCategoryAdapter;
import com.mgke.da.api.ApiClient;
import com.mgke.da.databinding.FragmentAddTransactionBinding;
import com.mgke.da.databinding.FragmentUpdateTransactionsBinding;
import com.mgke.da.models.Account;
import com.mgke.da.models.ConversionResponse;
import com.mgke.da.models.Goal;
import com.mgke.da.models.PersonalData;
import com.mgke.da.models.Transaction;
import com.mgke.da.repository.AccountRepository;
import com.mgke.da.repository.CategoryRepository;
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

public class UpdateTransactionsFragment extends Fragment {

    private FragmentUpdateTransactionsBinding binding;
    private String defaultCurrency;
    public static final String INCOME = "DOHOD";
    public static final String EXPENSE = "RACHOD";
    private SimpleCategoryAdapter categoryAdapter;
    private CategoryRepository categoryRepository;
    private AccountRepository accountRepository;
    private FirebaseUser currentUser;
    private String userId;
    private String selectedAccountId = null;
    private String currentTransactionType = INCOME;
    private FirebaseAuth auth;
    private String selectedGoalId; // Переменная для хранения ID выбранной цели
    private Transaction transaction;
    private Goal goal;

    public UpdateTransactionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());

        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d(TAG, "Attempting to load user data for userId: " + userId);

            personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
                if (personalData != null) {
                    Log.d(TAG, "Loaded personal data: " + personalData);
                    loadUserCurrency(personalData);
                } else {
                    Log.e(TAG, "User not found or error occurred");
                }
            }).exceptionally(e -> {
                Log.e(TAG, "Error fetching user data: ", e);
                return null;
            });
        } else {
            Log.e(TAG, "Current user is null");
        }
    }

    private void loadUserCurrency(PersonalData personalData) {
        if (personalData.currency != null && !personalData.currency.isEmpty()) {
            defaultCurrency = personalData.currency;
            binding.currencyTextView.setText(defaultCurrency);
            Log.d(TAG, "Loaded currency: " + defaultCurrency);
        } else {
            defaultCurrency = "USD";
            binding.currencyTextView.setText("");
            Log.d(TAG, "Currency is not set, defaulting to USD");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUpdateTransactionsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        // Получаем переданные данные
        if (getArguments() != null) {
            transaction = (Transaction) getArguments().getSerializable("transaction");
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Теперь вы можете безопасно получить NavController
        NavController navController = Navigation.findNavController(view);

        // Остальная часть вашего кода
        binding.recyclerViewCategories.setLayoutManager(new GridLayoutManager(getContext(), 4));
        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());
        accountRepository = new AccountRepository(FirebaseFirestore.getInstance());
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

        // Заполнение полей данными транзакции через binding
        if (transaction != null) {
            binding.nameAccount.setText(transaction.account);
//            binding.nameGoal.setText(goal.goalName);
            binding.sum.setText(String.valueOf(transaction.amount));
            binding.editTextCurrency.setText(transaction.currency);
            // Здесь можно добавить логику для отображения других данных, если нужно
        }

        binding.editTextCurrency.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                convertCurrencyToRUB();
            }

            @Override
            public void afterTextChanged(Editable s) {}
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
            radioGroupGoals.removeAllViews(); // Очищаем предыдущие радиокнопки
            for (Goal goal : goals) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(goal.goalName);
                radioButton.setId(View.generateViewId());
                radioGroupGoals.addView(radioButton);

                radioButton.setOnClickListener(v -> {
                    selectedGoalId = goal.id; // Сохраняем ID выбранной цели
                });
            }
        }).exceptionally(e -> {
            Toast.makeText(getContext(), "Ошибка загрузки целей: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        });

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        buttonSelectGoal.setOnClickListener(v -> {
            int selectedId = radioGroupGoals.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedRadioButton = dialogView.findViewById(selectedId);
                String selectedGoalName = selectedRadioButton.getText().toString();
                binding.nameGoal.setText(selectedGoalName);
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Пожалуйста, выберите цель", Toast.LENGTH_SHORT).show();
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

        // Изменяем на получение счетов по userId
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
        builder.setTitle("Выберите валюту")
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
                Toast.makeText(getContext(), "Введите положительное значение", Toast.LENGTH_SHORT).show();
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
                        binding.sum.setText("Ошибка конвертации");
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

        if (type.equals(INCOME)) {
            binding.incomeBtn.setBackgroundResource(R.drawable.transaction_add_income_selector);
            binding.expenseBtn.setBackgroundResource(R.drawable.transaction_add_default_selector);
            binding.incomeBtn.setTextColor(Color.parseColor("#00C853"));
            binding.expenseBtn.setTextColor(Color.BLACK);
            loadIncomeCategories();
        } else if (type.equals(EXPENSE)) {
            binding.incomeBtn.setBackgroundResource(R.drawable.transaction_add_default_selector);
            binding.expenseBtn.setBackgroundResource(R.drawable.transaction_add_expence_selector);
            binding.incomeBtn.setTextColor(Color.BLACK);
            binding.expenseBtn.setTextColor(Color.RED);
            loadExpenseCategories();
        }
    }

    private void loadIncomeCategories() {
        categoryRepository.getAllCategory(userId).thenAccept(categories -> {
            Log.d(TAG, "Loaded income categories: " + categories.size());
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new SimpleCategoryAdapter(getContext(), categories, categoryRepository, true);
                    binding.recyclerViewCategories.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
            } else {
                Log.d(TAG, "No income categories found for userId: " + userId);
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
        if (!isAdded()) {
            Log.e(TAG, "Fragment is not attached to the activity.");
            return;
        }

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

        Transaction transaction = createTransaction(type, category, account, date, amount, currency);
        TransactionRepository transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());

        // Получение выбранной цели
        final String goalIdToUse = selectedGoalId;  // Объявляем как final
        final double finalAmount = amount;  // Создаем локальную копию

        transactionRepository.addTransaction(transaction)
                .addOnSuccessListener(transactionId -> {
                    if (!isAdded()) {
                        return;
                    }
                    transaction.id = transactionId;

                    // Если выбрана цель, обновляем сумму цели
                    if (goalIdToUse != null) {
                        updateGoalAmount(goalIdToUse, finalAmount);  // Используем локальную копию
                    }

                    Toast.makeText(getContext(), getString(R.string.toast_save_success, transactionId), Toast.LENGTH_SHORT).show();
                    clearFields();
                    NavController navController = Navigation.findNavController(getView());
                    navController.popBackStack(); // Возврат на предыдущий фрагмент
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(getContext(), getString(R.string.toast_save_failure, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateGoalAmount(String goalId, double amount) {
        GoalRepository goalRepository = new GoalRepository(FirebaseFirestore.getInstance());

        goalRepository.getGoalById(goalId).thenAccept(goal -> {
            if (goal != null) {
                // Обновляем текущую сумму цели
                goal.progress += amount;  // Добавляем сумму к текущему прогрессу

                // Обновляем процент выполнения
                double percentage = (goal.progress / goal.targetAmount) * 100;

                // Сохраняем обновлённую цель в базе данных
                goalRepository.updateGoal(goal).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Цель обновлена: " + String.format("%.2f", percentage) + "% достигнуто", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Ошибка обновления цели: " + (task.getException() != null ? task.getException().getMessage() : "неизвестная ошибка"), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Цель не найдена", Toast.LENGTH_SHORT).show();
                }
            }
        }).exceptionally(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Ошибка получения цели: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return null;
        });
    }

    private String getSelectedGoalId() {
        // Реализуйте логику для получения ID выбранной цели
        // Например, вы можете сохранить ID выбранной цели в переменной класса при выборе в диалоговом окне
        return selectedGoalId; // Замените на вашу логику
    }

    private Transaction createTransaction(String type, String category, String account, Date date, double amount, String currency) {
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
        transaction.categoryImage = categoryImage;
        transaction.categoryColor = categoryColor;
        transaction.accountBackground = accountBackground;

        return transaction;
    }

    private String getAccountBackground(String accountName) {
        if (accountName == null) {
            return "account_fon1";
        }

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
            e.printStackTrace();
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