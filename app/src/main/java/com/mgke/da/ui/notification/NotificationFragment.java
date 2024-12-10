package com.mgke.da.ui.notification;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.models.Notification;
import com.mgke.da.repository.NotificationRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class NotificationFragment extends Fragment {
    private EditText titleEditText, amountEditText;
    private TextView dateTextView, timeTextView;
    private RadioGroup repeatTypeGroup;
    private Spinner repeatIntervalSpinner;
    private Button saveButton;
    private NotificationRepository notificationRepository;

    private String selectedInterval = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository(db);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        // Инициализация UI
        titleEditText = view.findViewById(R.id.notification_title);
        amountEditText = view.findViewById(R.id.notification_amount);
        dateTextView = view.findViewById(R.id.notification_date);
        timeTextView = view.findViewById(R.id.notification_time);
        repeatTypeGroup = view.findViewById(R.id.repeat_type_group);
        repeatIntervalSpinner = view.findViewById(R.id.repeat_interval_spinner);
        saveButton = view.findViewById(R.id.save_notification_btn);

        // Установка даты
        dateTextView.setOnClickListener(v -> showDatePicker());

        // Установка времени
        timeTextView.setOnClickListener(v -> showTimePicker());

        // Логика для переключения "Один раз" и "Повторяется"
        repeatTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.one_time) {
                repeatIntervalSpinner.setVisibility(View.GONE);  // Скрыть интервал, если выбран "Один раз"
            } else if (checkedId == R.id.repeat) {
                repeatIntervalSpinner.setVisibility(View.VISIBLE);  // Показать интервал, если выбран "Повторяется"
            }
        });

        // Установка доступных интервалов для повторяющихся уведомлений
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.repeat_intervals, // Это должен быть массив интервалов в res/values/strings.xml
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatIntervalSpinner.setAdapter(adapter);

        // Логика выбора интервала
        repeatIntervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedInterval = parentView.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                selectedInterval = ""; // Если не выбран интервал
            }
        });

        // Сохранение уведомления
        saveButton.setOnClickListener(v -> saveNotification());

        return view;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                    dateTextView.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    String selectedTime = String.format("%02d:%02d", hourOfDay, minute);
                    timeTextView.setText(selectedTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void saveNotification() {
        String title = titleEditText.getText().toString().trim();
        String amountStr = amountEditText.getText().toString().trim();
        String date = dateTextView.getText().toString().trim();
        String time = timeTextView.getText().toString().trim();
        String repeatType = repeatTypeGroup.getCheckedRadioButtonId() == R.id.one_time ? "Один раз" : "Повторяется";

        if (title.isEmpty() || amountStr.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(requireContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show();
            return;
        }

        Date dateTime;
        try {
            dateTime = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(date + " " + time);
        } catch (ParseException e) {
            Toast.makeText(requireContext(), "Некорректный формат даты/времени", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Notification notification = new Notification(null, title, amount, dateTime, repeatType, selectedInterval, userId);

        notificationRepository.addNotification(notification);
        Toast.makeText(requireContext(), "Уведомление сохранено", Toast.LENGTH_SHORT).show();
    }
}