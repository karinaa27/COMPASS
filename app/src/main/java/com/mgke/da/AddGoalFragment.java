package com.mgke.da;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.models.Goal;
import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.GoalRepository;
import com.mgke.da.repository.PersonalDataRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class AddGoalFragment extends Fragment {

    private EditText editTextGoalName;
    private EditText editTextTargetAmount;
    private EditText editTextDateEnd;
    private EditText editTextNote; // Поле для заметок
    private Button buttonAddGoal;
    private Spinner spinnerCurrency; // Spinner для выбора валюты
    private GoalRepository goalRepository;
    private String userId; // Идентификатор пользователя
    private Goal goal; // Цель, которую нужно редактировать

    private String[] currencies = {"BYN", "USD", "RUB", "UAH", "PLN", "EUR"}; // Массив валют

    public AddGoalFragment() {
        // Пустой конструктор
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add_goal, container, false);

        editTextGoalName = root.findViewById(R.id.editTextGoalName);
        editTextTargetAmount = root.findViewById(R.id.editTextTargetAmount);
        editTextDateEnd = root.findViewById(R.id.editTextDateEnd);
        editTextNote = root.findViewById(R.id.editTextNote); // Инициализация поля заметок
        buttonAddGoal = root.findViewById(R.id.buttonAddGoal);
        spinnerCurrency = root.findViewById(R.id.spinnerCurrency); // Инициализация Spinner

        // Инициализация GoalRepository
        goalRepository = new GoalRepository(FirebaseFirestore.getInstance());

        // Инициализация адаптера для Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(adapter);

        // Получение текущего пользователя и его идентификатора
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid(); // Получаем идентификатор пользователя
            loadUserCurrency(); // Загружаем валюту пользователя
        } else {
            Toast.makeText(getContext(), "Пользователь не аутентифицирован", Toast.LENGTH_SHORT).show();
            // Вы можете перенаправить на экран входа или выполнить другую логику
        }

        // Получение цели из аргументов, если она передана
        if (getArguments() != null) {
            goal = (Goal) getArguments().getSerializable("goal");
            if (goal != null) {
                // Заполнение полей данными цели
                editTextGoalName.setText(goal.goalName);
                editTextTargetAmount.setText(String.valueOf(goal.targetAmount));
                editTextDateEnd.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(goal.dateEnd));
                editTextNote.setText(goal.note);
                buttonAddGoal.setText("Сохранить изменения"); // Изменение текста кнопки

                // Установка выбранной валюты
                int currencyPosition = Arrays.asList(currencies).indexOf(goal.currency);
                spinnerCurrency.setSelection(currencyPosition); // Устанавливаем выбранную валюту
            }
        }

        buttonAddGoal.setOnClickListener(v -> {
            if (goal == null) {
                addGoal(); // Добавление новой цели
            } else {
                updateGoal(); // Обновление существующей цели
            }
        });

        return root;
    }

    // Метод для загрузки валюты пользователя
    private void loadUserCurrency() {
        PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
        personalDataRepository.getPersonalDataById(userId).thenAccept(this::setUserCurrency).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    // Метод для установки валюты пользователя
    private void setUserCurrency(PersonalData personalData) {
        if (personalData != null) {
            String userCurrency = personalData.currency; // Получаем валюту пользователя
            int position = Arrays.asList(currencies).indexOf(userCurrency);
            spinnerCurrency.setSelection(position >= 0 ? position : 1); // USD по умолчанию
        } else {
            spinnerCurrency.setSelection(1); // USD по умолчанию, если данные не найдены
        }
    }

    private void addGoal() {
        String goalName = editTextGoalName.getText().toString().trim();
        String targetAmountStr = editTextTargetAmount.getText().toString().trim();
        String dateEndStr = editTextDateEnd.getText().toString().trim();
        String note = editTextNote.getText().toString().trim(); // Получение заметок
        String currency = spinnerCurrency.getSelectedItem().toString(); // Получаем выбранную валюту

        if (goalName.isEmpty() || targetAmountStr.isEmpty() || dateEndStr.isEmpty()) {
            Toast.makeText(getContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        double targetAmount = Double.parseDouble(targetAmountStr);
        Date dateEnd;

        // Парсинг даты
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            dateEnd = sdf.parse(dateEndStr);
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Некорректная дата", Toast.LENGTH_SHORT).show();
            return;
        }

        Goal newGoal = new Goal(UUID.randomUUID().toString(), goalName, targetAmount, 0.0, userId, dateEnd, false, note, currency);

        // Добавление цели в репозиторий
        goalRepository.addGoal(newGoal);

        Toast.makeText(getContext(), "Цель добавлена", Toast.LENGTH_SHORT).show();
        clearFields();
    }

    private void updateGoal() {
        if (goal == null) return;

        String goalName = editTextGoalName.getText().toString().trim();
        String targetAmountStr = editTextTargetAmount.getText().toString().trim();
        String dateEndStr = editTextDateEnd.getText().toString().trim();
        String note = editTextNote.getText().toString().trim(); // Получение заметок
        String currency = spinnerCurrency.getSelectedItem().toString(); // Получаем выбранную валюту

        if (goalName.isEmpty() || targetAmountStr.isEmpty() || dateEndStr.isEmpty()) {
            Toast.makeText(getContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        double targetAmount = Double.parseDouble(targetAmountStr);
        Date dateEnd;

        // Парсинг даты
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            dateEnd = sdf.parse(dateEndStr);
        } catch (ParseException e) {
            Toast.makeText(getContext(), "Некорректная дата", Toast.LENGTH_SHORT).show();
            return;
        }

        // Обновление данных цели
        goal.goalName = goalName;
        goal.targetAmount = targetAmount;
        goal.dateEnd = dateEnd;
        goal.note = note;
        goal.currency = currency; // Обновление валюты

        // Обновление цели в репозитории
        goalRepository.updateGoal(goal);

        Toast.makeText(getContext(), "Цель обновлена", Toast.LENGTH_SHORT).show();
        clearFields();
    }

    private void clearFields() {
        editTextGoalName.setText("");
        editTextTargetAmount.setText("");
        editTextDateEnd.setText("");
        editTextNote.setText(""); // Очистка поля заметок
    }
}
