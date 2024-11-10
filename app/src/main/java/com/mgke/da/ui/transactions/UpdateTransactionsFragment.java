package com.mgke.da.ui.transactions;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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

                // Если сохранён goalId, то загружаем название цели
                if (transaction.goalId != null && !transaction.goalId.isEmpty()) {
                    goalRepository.getGoalById(transaction.goalId).thenAccept(goal -> {
                        if (goal != null) {
                            binding.nameGoal.setText(goal.goalName); // Устанавливаем название цели
                            binding.nameGoal.setVisibility(View.VISIBLE);
                        } else {
                            binding.nameGoal.setVisibility(View.GONE);
                        }
                    });
                } else {
                    binding.nameGoal.setVisibility(View.GONE);
                }
            }
        }

        binding.recyclerViewCategories.setLayoutManager(new GridLayoutManager(getContext(), 4));
        loadCategoriesForCurrentTransactionType();

        binding.nameGoal.setOnClickListener(v -> showSelectGoalDialog());
        binding.textViewCurrencyLabel.setOnClickListener(v -> showSelectCurrencyDialog());
        binding.addCategory.setOnClickListener(v -> {
            navController.navigate(R.id.action_updateTransactionFragment_to_addCategoryFragment);
        });
        binding.incomeBtn.setOnClickListener(v -> setTransactionType(INCOME));
        binding.expenseBtn.setOnClickListener(v -> setTransactionType(EXPENSE));
        binding.nameAccount.setOnClickListener(v -> showSelectAccountDialog());
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


    private void loadCategoriesForCurrentTransactionType() {
        if (currentTransactionType.equals(INCOME)) {
            loadIncomeCategories();
        } else if (currentTransactionType.equals(EXPENSE)) {
            loadExpenseCategories();
        }
    }
    private void updateTransaction() {
        // Проверяем наличие accountId, привязанного к счету
        String accountId = transaction.accountId; // Используем сохранённый accountId
        String currency = binding.editTextCurrency.getText().toString();
        String amountStr = binding.sum.getText().toString();
        String dateStr = binding.date.getText().toString();

        // Проверка обязательных полей
        if (accountId == null || accountId.isEmpty() || currency.isEmpty() || amountStr.isEmpty() || dateStr.isEmpty()) {
            return;
        }

        // Преобразование суммы
        double amount;
        try {
            amountStr = amountStr.replace(',', '.');
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), getString(R.string.toast_invalid_amount), Toast.LENGTH_SHORT).show();
            return;
        }

        // Преобразование даты
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date date;
        try {
            date = dateFormat.parse(dateStr);
        } catch (ParseException e) {
            Toast.makeText(getContext(), getString(R.string.toast_invalid_date), Toast.LENGTH_SHORT).show();
            return;
        }

        // Обновление полей транзакции
        transaction.accountId = selectedAccountId; // Используйте selectedAccountId, а не transaction.accountId
        transaction.currency = currency;
        transaction.amount = currentTransactionType.equals(EXPENSE) ? -Math.abs(amount) : Math.abs(amount);
        transaction.date = date;

        // Сохранение выбранного goalId вместо названия
        transaction.goalId = selectedGoalId != null && !selectedGoalId.isEmpty() ? selectedGoalId : null;

        // Сохранение транзакции в базе данных
        transactionRepository.updateTransaction(transaction)
                .addOnSuccessListener(aVoid -> {
                    NavController navController = Navigation.findNavController(getView());
                    navController.popBackStack();
                })
                .addOnFailureListener(e -> {

                });
    }


    private void deleteTransaction(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }
        transactionRepository.deleteTransaction(id).addOnSuccessListener(aVoid -> {
            NavController navController = Navigation.findNavController(getView());
            navController.popBackStack();

        }).addOnFailureListener(e -> {
        });
    }
    private void showSelectGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_goal, null);

        RadioGroup radioGroupGoals = dialogView.findViewById(R.id.radioGroupGoals);
        Button buttonSelectGoal = dialogView.findViewById(R.id.buttonSelectGoal);
        ImageView buttonAddGoal = dialogView.findViewById(R.id.buttonAddGoal); // Кнопка для добавления новой цели

        GoalRepository goalRepository = new GoalRepository(FirebaseFirestore.getInstance());
        String currentUserId = getCurrentUserId();

        goalRepository.getAllGoal().thenAccept(goals -> {
            radioGroupGoals.removeAllViews();

            // Добавляем опцию "Сбросить выбор"
            RadioButton resetSelectionButton = new RadioButton(getContext());
            resetSelectionButton.setText(getString(R.string.no_goal_selected));
            resetSelectionButton.setId(View.generateViewId());
            radioGroupGoals.addView(resetSelectionButton);

            resetSelectionButton.setOnClickListener(v -> {
                selectedGoalId = null;
                binding.nameGoal.setText("");
            });

            // Добавляем цели пользователя
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

    // Предположим, что у вас есть метод для получения идентификатора текущего пользователя
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

    private void showSelectAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_account, null);

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupAccounts);
        Button buttonSelectAccount = dialogView.findViewById(R.id.buttonSelectAccount);
        ImageView addButton = dialogView.findViewById(R.id.buttonAddAccount);  // добавляем обработку для кнопки "+"

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