package com.mgke.da;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mgke.da.models.Goal;
import com.mgke.da.repository.GoalRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class AddGoalFragment extends Fragment {

    private EditText editTextGoalName;
    private EditText editTextTargetAmount;
    private EditText editTextDateEnd;
    private EditText editTextNote; // Поле для заметок
    private Button buttonAddGoal;
    private GoalRepository goalRepository;
    private String userId; // Идентификатор пользователя

    public AddGoalFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add_goal, container, false);

        editTextGoalName = root.findViewById(R.id.editTextGoalName);
        editTextTargetAmount = root.findViewById(R.id.editTextTargetAmount);
        editTextDateEnd = root.findViewById(R.id.editTextDateEnd);
        editTextNote = root.findViewById(R.id.editTextNote); // Инициализация поля заметок
        buttonAddGoal = root.findViewById(R.id.buttonAddGoal);

        // Инициализация GoalRepository
        goalRepository = new GoalRepository(FirebaseFirestore.getInstance());

        // Получение текущего пользователя и его идентификатора
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid(); // Получаем идентификатор пользователя
        } else {
            Toast.makeText(getContext(), "Пользователь не аутентифицирован", Toast.LENGTH_SHORT).show();
            // Вы можете перенаправить на экран входа или выполнить другую логику
        }

        buttonAddGoal.setOnClickListener(v -> addGoal());

        return root;
    }

    private void addGoal() {
        String goalName = editTextGoalName.getText().toString().trim();
        String targetAmountStr = editTextTargetAmount.getText().toString().trim();
        String dateEndStr = editTextDateEnd.getText().toString().trim();
        String note = editTextNote.getText().toString().trim(); // Получение заметок

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

        Goal newGoal = new Goal(UUID.randomUUID().toString(), goalName, targetAmount, 0.0, userId, dateEnd, false, note);

        // Добавление цели в репозиторий
        goalRepository.addGoal(newGoal);

        Toast.makeText(getContext(), "Цель добавлена", Toast.LENGTH_SHORT).show();
        clearFields();
    }

    private void clearFields() {
        editTextGoalName.setText("");
        editTextTargetAmount.setText("");
        editTextDateEnd.setText("");
        editTextNote.setText(""); // Очистка поля заметок
    }
}