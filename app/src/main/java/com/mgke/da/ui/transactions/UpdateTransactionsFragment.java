package com.mgke.da.ui.transactions;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import com.mgke.da.models.Category;
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
import android.widget.ImageView;
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
import com.mgke.da.databinding.FragmentUpdateTransactionsBinding;
import com.mgke.da.models.Account;
import com.mgke.da.models.ConversionResponse;
import com.mgke.da.models.PersonalData;
import com.mgke.da.models.Transaction;
import com.mgke.da.repository.AccountRepository;
import com.mgke.da.repository.CategoryRepository;
import com.mgke.da.repository.PersonalDataRepository;
import com.mgke.da.repository.TransactionRepository;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdateTransactionsFragment extends Fragment {

    private FragmentUpdateTransactionsBinding binding;
    private String defaultCurrency;
    private TransactionRepository transactionRepository;
    public static final String INCOME = "DOHOD";
    public static final String EXPENSE = "RACHOD";
    private SimpleCategoryAdapter categoryAdapter;
    private CategoryRepository categoryRepository;
    private AccountRepository accountRepository;
    private FirebaseUser currentUser;
    private String userId;
    private String selectedAccountId = null;
    private String selectedCategory;

    private String currentTransactionType = INCOME;
    private FirebaseAuth auth;
    private String selectedGoalId;
    private Transaction transaction;
    private List<Category> categories;
    private String transactionId;
    private static final String TAG = "AddTransactionFragment"; // Тег для логов
    private static boolean areCategoriesCreated = false;

    public UpdateTransactionsFragment() {
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
                } else {
                }
            }).exceptionally(e -> {
                return null;
            });
        } else {
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
        binding = FragmentUpdateTransactionsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());
        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());
        accountRepository = new AccountRepository(FirebaseFirestore.getInstance());
        if (getArguments() != null) {
            transaction = (Transaction) getArguments().getSerializable("transaction");
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        categoryRepository = new CategoryRepository(db);
        GoalRepository goalRepository = new GoalRepository(db);
        categoryAdapter = new SimpleCategoryAdapter(getContext(), categories, categoryRepository, true);
        AccountRepository accountRepository = new AccountRepository(db); // Репозиторий для получения данных счета
        NavController navController = Navigation.findNavController(view);
        binding.close.setOnClickListener(v -> navController.popBackStack());
        if (!areCategoriesCreated) {
            loadCategoriesWithDefaults();
        } else {
        }
        if (getArguments() != null) {
            transaction = (Transaction) getArguments().getSerializable("transaction");
            if (transaction != null) {
                transactionId = transaction.id;
                currentTransactionType = transaction.type;
                binding.nameGoal.setVisibility(View.GONE);
                binding.textViewCurrencyLabel.setVisibility(View.GONE);
                setTransactionType(currentTransactionType);

                // Загрузка accountName на основе accountId и установка его в nameAccount
                if (transaction.accountId != null && !transaction.accountId.isEmpty()) {
                    accountRepository.getAccountById(transaction.accountId).thenAccept(account -> {
                        if (account != null) {
                            binding.nameAccount.setText(account.accountName); // Установка имени счета
                        } else {
                            // Устанавливаем placeholder если аккаунт не найден
                        }
                    }).exceptionally(e -> {
                       // На случай ошибки
                        return null;
                    });
                } else {
                  // Если accountId отсутствует
                }

                binding.editTextCurrency.setText(transaction.currency);
                binding.sum.setText(String.valueOf(Math.abs(transaction.amount)));
                binding.date.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(transaction.date));

            }
        }

        binding.recyclerViewCategories.setLayoutManager(new GridLayoutManager(getContext(), 4));
        loadCategoriesForCurrentTransactionType();

        binding.nameGoal.setOnClickListener(v -> {
            if (transaction != null) {
                showSelectGoalDialog(transaction); // Передаем текущую транзакцию
            } else {
                // Обработка случая, когда transaction равен null
                Toast.makeText(getContext(), "Transaction не доступна", Toast.LENGTH_SHORT).show();
            }
        });
        binding.textViewCurrencyLabel.setOnClickListener(v -> showSelectCurrencyDialog());
        binding.addCategory.setOnClickListener(v -> {
            navController.navigate(R.id.action_updateTransactionFragment_to_addCategoryFragment);
        });
        binding.incomeBtn.setOnClickListener(v -> setTransactionType(INCOME));
        binding.expenseBtn.setOnClickListener(v -> setTransactionType(EXPENSE));
        binding.nameAccount.setOnClickListener(v -> {
            if (transaction != null) {
                showSelectAccountDialog(transaction); // Передаем текущую транзакцию
            } else {
                Toast.makeText(getContext(), "Transaction не доступна", Toast.LENGTH_SHORT).show();
            }
        });
        binding.date.setOnClickListener(v -> showDatePickerDialog());
        binding.calendarBtn.setOnClickListener(v -> showDatePickerDialog());
        binding.textViewDeleteTransaction.setOnClickListener(v -> deleteTransaction(transactionId));
        binding.currencyTextView.setOnClickListener(v -> showSelectCurrencyDialog());

        binding.SaveTransactionBtn.setOnClickListener(v -> updateTransaction());

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

    private void setTransactionType(String type) {
        currentTransactionType = type;

        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (type.equals(INCOME)) {
            // Настроим кнопки для дохода
            binding.incomeBtn.setBackgroundResource(R.drawable.transaction_add_income_selector);
            binding.expenseBtn.setBackgroundResource(R.drawable.transaction_add_default_selector);

            binding.incomeBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.parseColor("#00C853"));
            binding.expenseBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);

            // Показываем поле цели для дохода
            binding.goal.setVisibility(View.VISIBLE);
            binding.nameGoal.setVisibility(View.VISIBLE);
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            GoalRepository goalRepository = new GoalRepository(db);
            // Если у транзакции уже есть цель, то устанавливаем её в поле nameGoal
            if (transaction != null && transaction.goalId != null && !transaction.goalId.isEmpty()) {
                goalRepository.getGoalById(transaction.goalId).thenAccept(goal -> {
                    if (goal != null) {
                        binding.nameGoal.setText(goal.goalName); // Устанавливаем название цели
                    } else {
                        binding.nameGoal.setText(""); // Если цель не найдена, ставим пустое значение
                    }
                }).exceptionally(e -> {
                    binding.nameGoal.setText(""); // В случае ошибки тоже ставим пустое значение
                    return null;
                });
            } else {
                binding.nameGoal.setText(""); // Если нет goalId, ставим пустое значение
            }

            loadIncomeCategories();
        } else if (type.equals(EXPENSE)) {
            // Настроим кнопки для расхода
            binding.incomeBtn.setBackgroundResource(R.drawable.transaction_add_default_selector);
            binding.expenseBtn.setBackgroundResource(R.drawable.transaction_add_expence_selector);

            binding.incomeBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            binding.expenseBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.RED);

            // Скрываем поле цели для расхода
            binding.goal.setVisibility(View.GONE);
            binding.nameGoal.setVisibility(View.GONE);

            loadExpenseCategories();
        }
    }
    private void loadCategoriesForCurrentTransactionType() {
        // Пример загрузки категорий для текущего типа транзакции
        if (currentTransactionType.equals(INCOME)) {
            loadIncomeCategories();
        } else if (currentTransactionType.equals(EXPENSE)) {
            loadExpenseCategories();
        }

        // Убедитесь, что категорию, связанную с транзакцией, правильно передали в адаптер
        if (transaction != null && transaction.category != null) {
            selectedCategory = transaction.category; // Присваиваем выбранную категорию
            categoryAdapter.setSelectedCategory(selectedCategory); // Устанавливаем выбранную категорию
        }
    }

    private void updateTransaction() {
        String accountId = selectedAccountId;
        String currency = binding.currencyTextView.getText() != null ? binding.currencyTextView.getText().toString() : "";
        String amountStr = binding.sum.getText().toString();
        String dateStr = binding.date.getText().toString();
        String category = categoryAdapter.getSelectedCategory();

        // Проверка на обязательные поля
        if (currency.isEmpty() || amountStr.isEmpty() || dateStr.isEmpty() || category == null || category.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_fill_all_fields, Toast.LENGTH_SHORT).show();
            return; // Возврат, если какие-либо обязательные поля пустые
        }

        double amount;
        try {
            amountStr = amountStr.replace(',', '.');
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), getString(R.string.toast_invalid_amount), Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date date;
        try {
            date = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            Toast.makeText(getContext(), getString(R.string.toast_invalid_date), Toast.LENGTH_SHORT).show();
            return;
        }

        int categoryImage = categoryAdapter.getSelectedCategoryImage();
        int categoryColor = categoryAdapter.getSelectedCategoryColor();

        // Устанавливаем accountId, только если он был изменен
        if (accountId != null && !accountId.isEmpty()) {
            transaction.accountId = accountId; // Обновляем accountId только если он не пустой
        }

        transaction.currency = currency;
        transaction.type = currentTransactionType;
        transaction.amount = currentTransactionType.equals(EXPENSE) ? -Math.abs(amount) : Math.abs(amount);
        transaction.date = date;
        transaction.category = category;
        transaction.categoryImage = categoryImage;
        transaction.categoryColor = categoryColor;
        transaction.goalId = selectedGoalId;

        // Создаем и показываем прогресс-диалог
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(getString(R.string.saving_transaction)); // Сообщение для прогресса
        progressDialog.setCancelable(false);
        progressDialog.show();

        TransactionRepository transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());
        transactionRepository.updateTransaction(transaction)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    progressDialog.dismiss(); // Скрываем прогресс-диалог после успешного сохранения
                    NavController navController = Navigation.findNavController(getView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressDialog.dismiss(); // Скрываем прогресс-диалог в случае ошибки
                    Toast.makeText(getContext(), R.string.error_updating_transaction, Toast.LENGTH_SHORT).show();
                });
    }

    private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
            return amount;
        }

        double rate = 1.0;

        if (fromCurrency.equals("USD")) {
            if (toCurrency.equals("EUR")) {
                rate = 0.85;
            } else if (toCurrency.equals("RUB")) {
                rate = 70.0;
            } else if (toCurrency.equals("BYN")) {
                rate = 2.6;
            } else if (toCurrency.equals("UAH")) {
                rate = 27.0;
            } else if (toCurrency.equals("PLN")) {
                rate = 3.7;
            }
        } else if (fromCurrency.equals("EUR")) {
            if (toCurrency.equals("USD")) {
                rate = 1.18;
            } else if (toCurrency.equals("RUB")) {
                rate = 82.0;
            } else if (toCurrency.equals("BYN")) {
                rate = 3.1;
            } else if (toCurrency.equals("UAH")) {
                rate = 31.0;
            } else if (toCurrency.equals("PLN")) {
                rate = 4.3;
            }
        } else if (fromCurrency.equals("RUB")) {
            if (toCurrency.equals("USD")) {
                rate = 0.014;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.012;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.032;
            } else if (toCurrency.equals("UAH")) {
                rate = 0.36;
            } else if (toCurrency.equals("PLN")) {
                rate = 0.05;
            }
        } else if (fromCurrency.equals("BYN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.38;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.32;
            } else if (toCurrency.equals("RUB")) {
                rate = 31.0;
            } else if (toCurrency.equals("UAH")) {
                rate = 11.0;
            } else if (toCurrency.equals("PLN")) {
                rate = 1.4;
            }
        } else if (fromCurrency.equals("UAH")) {
            if (toCurrency.equals("USD")) {
                rate = 0.037;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.032;
            } else if (toCurrency.equals("RUB")) {
                rate = 2.8;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.091;
            } else if (toCurrency.equals("PLN")) {
                rate = 0.12;
            }
        } else if (fromCurrency.equals("PLN")) {
            if (toCurrency.equals("USD")) {
                rate = 0.27;
            } else if (toCurrency.equals("EUR")) {
                rate = 0.23;
            } else if (toCurrency.equals("RUB")) {
                rate = 20.0;
            } else if (toCurrency.equals("BYN")) {
                rate = 0.71;
            } else if (toCurrency.equals("UAH")) {
                rate = 8.3;
            }
        }

        return amount * rate;
    }

    private void deleteTransaction(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        new AlertDialog.Builder(getContext())
                .setMessage(getString(R.string.confirm_delete_transaction)) // Строка для подтверждения
                .setTitle(getString(R.string.confirm_delete)) // Заголовок
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> {
                    // Создаем и показываем ProgressDialog
                    ProgressDialog progressDialog = new ProgressDialog(getContext());
                    progressDialog.setMessage(getString(R.string.deleting_transaction)); // Строка для удаления
                    progressDialog.setCancelable(false); // Диалог нельзя отменить пользователем
                    progressDialog.show();

                    // Выполняем удаление
                    transactionRepository.deleteTransaction(id)
                            .addOnSuccessListener(aVoid -> {
                                // Скрываем ProgressDialog после успешного удаления
                                progressDialog.dismiss();
                                NavController navController = Navigation.findNavController(getView());
                                navController.popBackStack();
                            })
                            .addOnFailureListener(e -> {
                                // Скрываем ProgressDialog в случае ошибки
                                progressDialog.dismiss();
                                // Показать сообщение об ошибке пользователю
                                Toast.makeText(getContext(), getString(R.string.delete_transaction_error), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton(getString(R.string.no), (dialog, which) -> {
                    // Пользователь отказался от удаления, закрываем диалог
                    dialog.dismiss();
                })
                .show();
    }

    private void showSelectGoalDialog(Transaction transaction) { // Передаем объект Transaction
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_goal, null);

        RadioGroup radioGroupGoals = dialogView.findViewById(R.id.radioGroupGoals);
        Button buttonSelectGoal = dialogView.findViewById(R.id.buttonSelectGoal);
        ImageView buttonAddGoal = dialogView.findViewById(R.id.buttonAddGoal);

        GoalRepository goalRepository = new GoalRepository(FirebaseFirestore.getInstance());
        String currentUserId = getCurrentUserId();

        // Получаем goalId напрямую из объекта transaction
        String transactionGoalId = transaction.goalId; // Используем публичное поле

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

                    // Проверяем, совпадает ли goal.id с transactionGoalId
                    if (goal.id.equals(transactionGoalId)) {
                        radioButton.setChecked(true);
                        selectedGoalId = goal.id; // Устанавливаем selectedGoalId
                        binding.nameGoal.setText(goal.goalName); // Заполняем поле с именем цели
                    }

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
        // Логика для получения идентификатора текущего пользователя
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

    private void showSelectAccountDialog(Transaction transaction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_account, null);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupAccounts);
        Button buttonSelectAccount = dialogView.findViewById(R.id.buttonSelectAccount);
        ImageView addButton = dialogView.findViewById(R.id.buttonAddAccount);

        accountRepository.getAccountsByUserId(userId).thenAccept(accounts -> {
            radioGroup.removeAllViews();
            for (Account account : accounts) {
                RadioButton radioButton = new RadioButton(getContext());
                radioButton.setText(account.accountName);
                radioButton.setId(View.generateViewId());
                radioGroup.addView(radioButton);

                // Устанавливаем выбранную радиокнопку, если ID счета совпадает с accountId транзакции
                if (transaction != null && transaction.accountId != null && transaction.accountId.equals(account.id)) {
                    radioButton.setChecked(true); // Устанавливаем флаг для выбранной радиокнопки
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
            } else {
                Toast.makeText(getContext(), "Пожалуйста, выберите счет", Toast.LENGTH_SHORT).show();
            }
        });

        addButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.action_navigation_update_to_addAccountFragment);
            dialog.dismiss();
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

        // Проверка на пустое поле или некорректный ввод
        if (!inputAmountStr.isEmpty()) {
            try {
                double inputAmount = Double.parseDouble(inputAmountStr);

                // Проверка на валидность суммы (неотрицательное число)
                if (inputAmount <= 0) {
                    return; // Выход, если сумма меньше или равна нулю
                }

                // Получаем выбранную валюту (этот элемент должен быть видим)
                String selectedCurrency = binding.textViewCurrencyLabel.getText().toString();

                // Проверка, что валюта действительно выбрана
                if (selectedCurrency.isEmpty()) {
                    binding.sum.setText("Выберите валюту.");
                    return;
                }

                String apiKey = "87986aa7d23ce4bca64d81bbdd909517";

                // Конвертация валюты через API
                ApiClient.convertCurrency(apiKey, selectedCurrency, defaultCurrency, inputAmount)
                        .enqueue(new Callback<ConversionResponse>() {
                            @Override
                            public void onResponse(Call<ConversionResponse> call, Response<ConversionResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    double convertedAmount = response.body().getResult();
                                    DecimalFormat df = new DecimalFormat("#.00");
                                    String formattedAmount = df.format(convertedAmount);
                                    binding.sum.setText(formattedAmount);
                                } else {
                                    binding.sum.setText("Ошибка при получении данных.");
                                }
                            }

                            @Override
                            public void onFailure(Call<ConversionResponse> call, Throwable t) {
                                binding.sum.setText("Ошибка: " + t.getMessage());
                            }
                        });
            } catch (NumberFormatException e) {
                // Если введенная строка не является числом
                binding.sum.setText("Неверный формат суммы.");
            }
        } else {
            binding.sum.setText(""); // Если поле пустое
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
            } else {
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
    private void loadUserCurrencyFromDatabase() {
        if (currentUser != null) {
            PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
            personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
                if (personalData != null) {
                    loadUserCurrency(personalData);
                }
            }).exceptionally(e -> {
                e.printStackTrace(); // Не забываем обрабатывать исключения
                return null;
            });
        }
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
                // Создаем дефолтные категории
                CompletableFuture<Void> incomeCategories = categoryRepository.createDefaultCategories(userId, "income");
                CompletableFuture<Void> expenseCategories = categoryRepository.createDefaultCategories(userId, "expense");

                CompletableFuture.allOf(incomeCategories, expenseCategories).whenComplete((v, ex) -> {
                    if (ex != null) {
                        Log.e(TAG, "Ошибка при создании категорий", ex);
                    } else {
                        Log.d(TAG, "Дефолтные категории успешно созданы");
                        // После создания категорий обновляем флаг
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
    @Override
    public void onResume() {
        super.onResume();
        binding.recyclerViewCategories.setAdapter(categoryAdapter);
        loadUserCurrencyFromDatabase();
    }
}