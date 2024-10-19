package com.mgke.da.ui.transactions;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
<<<<<<< HEAD
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.mgke.da.Constants;
import com.mgke.da.R;
import com.mgke.da.adapters.TransactionAdapter;
import com.mgke.da.databinding.FragmentTransactionsBinding;
import com.mgke.da.models.PersonalData;
import com.mgke.da.models.Transaction;
import com.mgke.da.repository.AccountRepository;
import com.mgke.da.repository.PersonalDataRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
=======
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.mgke.da.Constants;
import com.mgke.da.R;
import com.mgke.da.databinding.FragmentTransactionsBinding;
import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private FragmentTransactionsBinding binding;
    private Calendar calendar;
<<<<<<< HEAD
    private TransactionAdapter adapter;
    private List<Transaction> transactionList;
    private ImageView emptyStateImageView;
    private PersonalData personalData;
    private TextView emptyStateTextView;
    private PersonalDataRepository personalDataRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        transactionList = new ArrayList<>();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AccountRepository accountRepository = new AccountRepository(db);
        personalDataRepository = new PersonalDataRepository(db);

        adapter = new TransactionAdapter(getContext(), transactionList, accountRepository);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emptyStateImageView = binding.emptyStateImageView;
        emptyStateTextView = binding.emptyStateTextView;

        setLocaleFromPreferences();
        calendar = Calendar.getInstance();
        binding.dateText.setText(formatDateShort(calendar.getTime()));

        selectDayButton();
        updateDateText();
        setupButtonListeners();

        Button addTransactionButton = binding.addTransactionButton;
        addTransactionButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.addTransactionFragment);
        });

        loadUserData(); // Загружаем данные пользователя

=======

    public View onCreateView(@NonNull LayoutInflater inflater,
                              ViewGroup container, Bundle savedInstanceState) {
        // Считываем язык из SharedPreferences
        setLocaleFromPreferences();
        TransactionsViewModel transactionsViewModel =
                new ViewModelProvider(this).get(TransactionsViewModel.class);

        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Инициализация календаря
        calendar = Calendar.getInstance();

        // Устанавливаем фиксированную текущую дату для dateText
        binding.dateText.setText(formatDateShort(calendar.getTime())); // Устанавливаем текущую дату

        // Установка состояния по умолчанию
        selectDayButton(); // Установка кнопки "День" по умолчанию
        updateDateText(); // Обновляем отображение даты

        // Установка обработчиков нажатий для кнопок
        setupButtonListeners();

>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
        return root;
    }

    private void resetToCurrentDate() {
<<<<<<< HEAD
        calendar = Calendar.getInstance();
=======
        calendar = Calendar.getInstance(); // Сбрасываем календарь на текущую дату
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    }

    private void selectDayButton() {
        setSelectedButton(binding.dayBtn, binding.monthlyBtn);
<<<<<<< HEAD
        Constants.SELECTED_TAB = Constants.DAILY;
        resetToCurrentDate();
        updateDateText();
    }

    private void setupButtonListeners() {
        binding.dayBtn.setOnClickListener(v -> {
            setSelectedButton(binding.dayBtn, binding.monthlyBtn);
            Constants.SELECTED_TAB = Constants.DAILY;
            resetToCurrentDate();
            updateDateText();
            loadTransactions();
        });

        binding.monthlyBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthlyBtn, binding.dayBtn);
            Constants.SELECTED_TAB = Constants.MONTHLY;
            resetToCurrentDate();
            updateDateText();
            loadTransactions();
        });

        binding.previousDate.setOnClickListener(v -> {
            if (Constants.SELECTED_TAB == Constants.DAILY) {
                calendar.add(Calendar.DATE, -1);
            } else {
                calendar.add(Calendar.MONTH, -1);
            }
            updateDateText();
            loadTransactions();
=======
        Constants.SELECTED_TAB = Constants.DAILY; // Устанавливаем выбранный таб
        resetToCurrentDate(); // Сбрасываем дату на текущую
        updateDateText(); // Обновляем отображение даты
    }

    private void setupButtonListeners() {
        // Обработчик нажатия для кнопки "День"
        binding.dayBtn.setOnClickListener(v -> {
            setSelectedButton(binding.dayBtn, binding.monthlyBtn);
            Constants.SELECTED_TAB = Constants.DAILY; // Установите выбранный таб
            resetToCurrentDate(); // Сбрасываем дату на текущую
            updateDateText(); // Обновляем дату при выборе "День"
        });

        // Обработчик нажатия для кнопки "Месяц"
        binding.monthlyBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthlyBtn, binding.dayBtn);
            Constants.SELECTED_TAB = Constants.MONTHLY; // Установите выбранный таб
            resetToCurrentDate(); // Сбрасываем дату на текущую
            updateDateText(); // Обновляем дату при выборе "Месяц"
        });

        // Обработчики для кнопок "Предыдущая дата" и "Следующая дата"
        binding.previousDate.setOnClickListener(v -> {
            if (Constants.SELECTED_TAB == Constants.DAILY) {
                calendar.add(Calendar.DATE, -1); // Уменьшаем на день
            } else {
                calendar.add(Calendar.MONTH, -1); // Уменьшаем на месяц
            }
            updateDateText(); // Обновляем отображение даты
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
        });

        binding.nextDate.setOnClickListener(v -> {
            if (Constants.SELECTED_TAB == Constants.DAILY) {
<<<<<<< HEAD
                calendar.add(Calendar.DATE, 1);
            } else {
                calendar.add(Calendar.MONTH, 1);
            }
            updateDateText();
            loadTransactions();
=======
                calendar.add(Calendar.DATE, 1); // Увеличиваем на день
            } else {
                calendar.add(Calendar.MONTH, 1); // Увеличиваем на месяц
            }
            updateDateText(); // Обновляем отображение даты
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
        });
    }

    private void setSelectedButton(TextView selected, TextView unselected) {
<<<<<<< HEAD
        // Устанавливаем фон и цвет текста для выбранной кнопки
        selected.setBackgroundResource(R.drawable.day_month_selector_active);
        selected.setTextColor(getResources().getColor(android.R.color.white)); // Белый цвет текста

        // Устанавливаем фон и цвет текста для невыбранной кнопки
        unselected.setBackgroundResource(R.drawable.day_month_selector);
        unselected.setTextColor(getResources().getColor(android.R.color.black)); // Черный цвет текста
    }

    private void updateDateText() {
        TextView currentDateText = binding.currentDate;
        if (Constants.SELECTED_TAB == Constants.DAILY) {
            currentDateText.setText(formatDate(calendar.getTime()));
        } else {
            currentDateText.setText(formatDateByMonth(calendar.getTime()));
=======
        selected.setBackgroundResource(R.drawable.day_month_selector_active); // Фон для выбранной кнопки
        unselected.setBackgroundResource(R.drawable.day_month_selector); // Исходный фон для невыбранной кнопки
    }

    private void updateDateText() {
        TextView currentDateText = binding.currentDate; // Получаем ссылку на TextView для отображения даты

        if (Constants.SELECTED_TAB == Constants.DAILY) {
            currentDateText.setText(formatDate(calendar.getTime())); // Форматируем дату по дням
        } else {
            currentDateText.setText(formatDateByMonth(calendar.getTime())); // Форматируем дату по месяцам
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
        }
    }

    public static String formatDateShort(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE dd, MMM", Locale.getDefault());
        return dateFormat.format(date);
    }

    public static String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }

    public static String formatDateByMonth(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }
<<<<<<< HEAD

    private void loadUserData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("Transactions", "Пользователь не аутентифицирован");
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("Transactions", "ID пользователя: " + userId); // Логируем ID пользователя

        personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
            if (personalData != null) {
                this.personalData = personalData;
                Log.d("Transactions", "Данные пользователя успешно загружены: " + personalData.toString());
                loadTransactions(); // Загружаем транзакции после загрузки данных пользователя
                loadAllTransactions(); // Загружаем все транзакции для подсчета итогов
            } else {
                Log.e("Transactions", "Личные данные пустые");
            }
        }).exceptionally(e -> {
            Log.e("Transactions", "Ошибка при загрузке данных пользователя: " + e.getMessage());
            return null;
        });
    }

    private void loadTransactions() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("Transactions", "Пользователь не аутентифицирован");
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Получаем начало и конец выбранного дня или месяца
        Calendar startCalendar = (Calendar) calendar.clone();
        Calendar endCalendar = (Calendar) calendar.clone();

        if (Constants.SELECTED_TAB == Constants.DAILY) {
            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);

            endCalendar.set(Calendar.HOUR_OF_DAY, 23);
            endCalendar.set(Calendar.MINUTE, 59);
            endCalendar.set(Calendar.SECOND, 59);
            endCalendar.set(Calendar.MILLISECOND, 999);
        } else {
            startCalendar.set(Calendar.DAY_OF_MONTH, 1);
            startCalendar.set(Calendar.HOUR_OF_DAY, 0);
            startCalendar.set(Calendar.MINUTE, 0);
            startCalendar.set(Calendar.SECOND, 0);
            startCalendar.set(Calendar.MILLISECOND, 0);

            endCalendar.set(Calendar.DAY_OF_MONTH, startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            endCalendar.set(Calendar.HOUR_OF_DAY, 23);
            endCalendar.set(Calendar.MINUTE, 59);
            endCalendar.set(Calendar.SECOND, 59);
            endCalendar.set(Calendar.MILLISECOND, 999);
        }

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startCalendar.getTime())
                .whereLessThanOrEqualTo("date", endCalendar.getTime())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Transactions", "Ошибка при получении документов: " + error.getMessage());
                        return;
                    }

                    List<Transaction> newTransactions = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            newTransactions.add(transaction);
                        }
                    }

                    // Обновите адаптер
                    if (adapter != null) {
                        adapter.updateTransactions(newTransactions);
                    }

                    // Проверка наличия транзакций
                    if (newTransactions.isEmpty()) {
                        emptyStateImageView.setVisibility(View.VISIBLE);
                        emptyStateTextView.setVisibility(View.VISIBLE);
                        Glide.with(this)
                                .asGif()
                                .load(R.drawable.document_search)
                                .into(emptyStateImageView);
                    } else {
                        emptyStateImageView.setVisibility(View.GONE);
                        emptyStateTextView.setVisibility(View.GONE);
                    }
                });
    }

    private void loadAllTransactions() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("Transactions", "Пользователь не аутентифицирован");
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Transactions", "Ошибка при получении документов: " + error.getMessage());
                        return;
                    }

                    List<Transaction> allTransactions = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = document.toObject(Transaction.class);
                            allTransactions.add(transaction);
                        }
                    }

                    updateAmounts(allTransactions); // Обновляем суммы по всем транзакциям
                });
    }

    private void updateAmounts(List<Transaction> transactions) {
        double totalBalance = 0;
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction transaction : transactions) {
            totalBalance += transaction.getAmount();
            if (transaction.getType().equals("DOHOD")) {
                totalIncome += transaction.getAmount();
            } else if (transaction.getType().equals("RACHOD")) {
                totalExpense += transaction.getAmount();
            }
        }

        String currency = personalData != null ? personalData.currency : "₩"; // Знак валюты по умолчанию
        binding.balanceAmount.setText(String.format("%s %.2f", currency, totalBalance));
        binding.incomeAmount.setText(String.format("%s %.2f", currency, totalIncome));
        binding.expenseAmount.setText(String.format("%s %.2f", currency, totalExpense));
    }

=======
    private void showDatePickerDialog() {
        // Получаем текущую дату
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Создаем DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getActivity(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Устанавливаем выбранную дату в календарь
                    calendar.set(Calendar.YEAR, selectedYear);
                    calendar.set(Calendar.MONTH, selectedMonth);
                    calendar.set(Calendar.DAY_OF_MONTH, selectedDay);
                    updateDateText(); // Обновляем отображение даты
                },
                year, month, day
        );

        datePickerDialog.show();
    }
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    private void setLocaleFromPreferences() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
        String selectedLanguage = sharedPreferences.getString("selectedLanguage", "English");
        String languageCode = selectedLanguage.equals("Русский") ? "ru" : "en";
        setLocale(languageCode);
    }
<<<<<<< HEAD

=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
<<<<<<< HEAD

=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}