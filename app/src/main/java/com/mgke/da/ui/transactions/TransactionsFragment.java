package com.mgke.da.ui.transactions;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.mgke.da.Constants;
import com.mgke.da.R;
import com.mgke.da.databinding.FragmentTransactionsBinding;
import android.app.DatePickerDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;



public class TransactionsFragment extends Fragment {

    private FragmentTransactionsBinding binding;
    private Calendar calendar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Инициализация привязки
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Считываем язык из SharedPreferences
        setLocaleFromPreferences();
        TransactionsViewModel transactionsViewModel =
                new ViewModelProvider(this).get(TransactionsViewModel.class);

        // Инициализация календаря
        calendar = Calendar.getInstance();

        // Устанавливаем фиксированную текущую дату для dateText
        binding.dateText.setText(formatDateShort(calendar.getTime())); // Устанавливаем текущую дату

        // Установка состояния по умолчанию
        selectDayButton(); // Установка кнопки "День" по умолчанию
        updateDateText(); // Обновляем отображение даты

        // Установка обработчиков нажатий для кнопок
        setupButtonListeners();

        // Настройка кнопки добавления транзакции
        Button addTransactionButton = binding.addTransactionButton; // Используем binding для доступа к кнопке
        addTransactionButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.addTransactionFragment); // Замените на правильный ID вашего фрагмента
        });

        return root; // Возвращаем корневое представление
    }

    private void resetToCurrentDate() {
        calendar = Calendar.getInstance(); // Сбрасываем календарь на текущую дату
    }

    private void selectDayButton() {
        setSelectedButton(binding.dayBtn, binding.monthlyBtn);
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
        });

        binding.nextDate.setOnClickListener(v -> {
            if (Constants.SELECTED_TAB == Constants.DAILY) {
                calendar.add(Calendar.DATE, 1); // Увеличиваем на день
            } else {
                calendar.add(Calendar.MONTH, 1); // Увеличиваем на месяц
            }
            updateDateText(); // Обновляем отображение даты
        });
    }

    private void setSelectedButton(TextView selected, TextView unselected) {
        selected.setBackgroundResource(R.drawable.day_month_selector_active); // Фон для выбранной кнопки
        unselected.setBackgroundResource(R.drawable.day_month_selector); // Исходный фон для невыбранной кнопки
    }

    private void updateDateText() {
        TextView currentDateText = binding.currentDate; // Получаем ссылку на TextView для отображения даты

        if (Constants.SELECTED_TAB == Constants.DAILY) {
            currentDateText.setText(formatDate(calendar.getTime())); // Форматируем дату по дням
        } else {
            currentDateText.setText(formatDateByMonth(calendar.getTime())); // Форматируем дату по месяцам
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
    private void setLocaleFromPreferences() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
        String selectedLanguage = sharedPreferences.getString("selectedLanguage", "English");
        String languageCode = selectedLanguage.equals("Русский") ? "ru" : "en";
        setLocale(languageCode);
    }
    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}