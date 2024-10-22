package com.mgke.da.ui.goal;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.models.Goal;
import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.GoalRepository;
import com.mgke.da.repository.PersonalDataRepository;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class AddGoalFragment extends Fragment {
    private EditText editTextGoalName;
    private EditText editTextTargetAmount;
    private EditText editTextDateEnd;
    private EditText editTextNote;
    private Button buttonAddGoal;
    private Spinner spinnerCurrency;
    private GoalRepository goalRepository;
    private String userId;
    private Goal goal;
    private ImageView closeIcon;
    private String[] currencies = {"BYN", "USD", "RUB", "UAH", "PLN", "EUR"};

    public AddGoalFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_add_goal, container, false);
        editTextGoalName = root.findViewById(R.id.editTextGoalName);
        editTextTargetAmount = root.findViewById(R.id.editTextTargetAmount);
        editTextDateEnd = root.findViewById(R.id.editTextDateEnd);
        editTextNote = root.findViewById(R.id.editTextNote);
        buttonAddGoal = root.findViewById(R.id.buttonAddGoal);
        spinnerCurrency = root.findViewById(R.id.spinnerCurrency);
        closeIcon = root.findViewById(R.id.close);
        goalRepository = new GoalRepository(FirebaseFirestore.getInstance());
        editTextTargetAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String value = s.toString();
                if (value.contains(".")) {
                    int index = value.indexOf(".");
                    if (value.length() - index - 1 > 2) {
                        editTextTargetAmount.setText(value.substring(0, index + 3));
                        editTextTargetAmount.setSelection(editTextTargetAmount.getText().length());
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(adapter);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            loadUserCurrency();
        } else {
            Toast.makeText(getContext(), R.string.user_not_authenticated, Toast.LENGTH_SHORT).show();
        }
        if (getArguments() != null) {
            goal = (Goal) getArguments().getSerializable("goal");
            if (goal != null) {
                editTextGoalName.setText(goal.goalName);
                editTextTargetAmount.setText(String.valueOf(goal.targetAmount));
                editTextDateEnd.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(goal.dateEnd));
                editTextNote.setText(goal.note);
                buttonAddGoal.setText(R.string.save_changes);
                int currencyPosition = Arrays.asList(currencies).indexOf(goal.currency);
                spinnerCurrency.setSelection(currencyPosition);
            }
        }
        setupDatePicker();
        buttonAddGoal.setOnClickListener(v -> {
            if (goal == null) {
                addGoal();
            } else {
                updateGoal();
            }
        });
        closeIcon.setOnClickListener(v -> closeFragment());
        return root;
    }

    private void loadUserCurrency() {
        PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
        personalDataRepository.getPersonalDataById(userId).thenAccept(this::setUserCurrency).exceptionally(e -> {
            e.printStackTrace();
            return null;
        });
    }

    private void setUserCurrency(PersonalData personalData) {
        if (personalData != null) {
            String userCurrency = personalData.currency;
            int position = Arrays.asList(currencies).indexOf(userCurrency);
            spinnerCurrency.setSelection(position >= 0 ? position : 1);
        } else {
            spinnerCurrency.setSelection(1);
        }
    }

    private void addGoal() {
        String goalName = editTextGoalName.getText().toString().trim();
        String targetAmountStr = editTextTargetAmount.getText().toString().trim();
        String dateEndStr = editTextDateEnd.getText().toString().trim();
        String note = editTextNote.getText().toString().trim();
        String currency = spinnerCurrency.getSelectedItem().toString();
        if (goalName.isEmpty() || targetAmountStr.isEmpty() || dateEndStr.isEmpty()) {
            Toast.makeText(getContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        double targetAmount = Double.parseDouble(targetAmountStr);
        Date dateEnd;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            dateEnd = sdf.parse(dateEndStr);
        } catch (ParseException e) {
            Toast.makeText(getContext(), R.string.invalid_date, Toast.LENGTH_SHORT).show();
            return;
        }
        Goal newGoal = new Goal(UUID.randomUUID().toString(), goalName, targetAmount, 0.0, userId, dateEnd, false, note, currency);
        goalRepository.addGoal(newGoal);
        Toast.makeText(getContext(), R.string.goal_added, Toast.LENGTH_SHORT).show();
        closeFragment();
    }

    private void updateGoal() {
        if (goal == null) return;
        String goalName = editTextGoalName.getText().toString().trim();
        String targetAmountStr = editTextTargetAmount.getText().toString().trim();
        String dateEndStr = editTextDateEnd.getText().toString().trim();
        String note = editTextNote.getText().toString().trim();
        String currency = spinnerCurrency.getSelectedItem().toString();
        if (goalName.isEmpty() || targetAmountStr.isEmpty() || dateEndStr.isEmpty()) {
            Toast.makeText(getContext(), R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        double targetAmount = Double.parseDouble(targetAmountStr);
        Date dateEnd;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            dateEnd = sdf.parse(dateEndStr);
        } catch (ParseException e) {
            Toast.makeText(getContext(), R.string.invalid_date, Toast.LENGTH_SHORT).show();
            return;
        }
        goal.goalName = goalName;
        goal.targetAmount = targetAmount;
        goal.dateEnd = dateEnd;
        goal.note = note;
        goal.currency = currency;
        goalRepository.updateGoal(goal);
        Toast.makeText(getContext(), R.string.goal_updated, Toast.LENGTH_SHORT).show();
        closeFragment();
    }

    private void setupDatePicker() {
        editTextDateEnd.setFocusable(false);
        editTextDateEnd.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, selectedYear, selectedMonth, selectedDay) -> {
                Calendar selectedDate = Calendar.getInstance();
                selectedDate.set(selectedYear, selectedMonth, selectedDay);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                editTextDateEnd.setText(sdf.format(selectedDate.getTime()));
            }, year, month, day);
            datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
            datePickerDialog.show();
        });
    }

    private void closeFragment() {
        FragmentManager fragmentManager = getParentFragmentManager();
        fragmentManager.popBackStack();
    }
}