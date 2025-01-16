package com.mgke.da.ui.personal_data;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.mgke.da.R;
import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.PersonalDataRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PersonalDataFragment extends Fragment {
    private EditText etFirstName, etLastName, etProfession, etNote, etBirthday;
    private RadioGroup radioGroupGender;
    private Button buttonSave;
    private ImageView closeButton;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUsername = "";
    private PersonalDataRepository personalDataRepository;
    private ListenerRegistration registration;
    private AutoCompleteTextView etCountry;
    private EditText etUsername; // Поле для имени пользователя

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_data, container, false);
        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etCountry = view.findViewById(R.id.etCountry);
        etProfession = view.findViewById(R.id.etProfession);
        etNote = view.findViewById(R.id.etNote);
        etBirthday = view.findViewById(R.id.etBirthday);
        radioGroupGender = view.findViewById(R.id.radioGroupGender);
        buttonSave = view.findViewById(R.id.buttonSave);
        closeButton = view.findViewById(R.id.close);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        personalDataRepository = new PersonalDataRepository(db); // инициализация репозитория
        etUsername = view.findViewById(R.id.etUsername);
        loadPersonalData();

        // Ограничение только на выбор из выпадающего списка
        etCountry.setKeyListener(null);
        etCountry.setOnClickListener(v -> showCountrySelectionDialog());

        // Установка слушателя для выбора даты рождения
        etBirthday.setOnClickListener(v -> showDatePicker());

        closeButton.setOnClickListener(v -> navigateToSettings());
        buttonSave.setOnClickListener(v -> {
            savePersonalData();
        });

        return view;
    }

    private void showCountrySelectionDialog() {
        String[] countries = getResources().getStringArray(R.array.countries_array);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.select_country);
        builder.setItems(countries, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                etCountry.setText(countries[which]); // Устанавливаем выбранную страну
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Закрываем диалог
            }
        });

        builder.show(); // Отображаем диалог
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    etBirthday.setText(selectedDate);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void loadPersonalData() {
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            registration = db.collection("personalData").document(userId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (snapshot != null && snapshot.exists()) {
                            PersonalData data = snapshot.toObject(PersonalData.class);
                            if (data != null) {
                                currentUsername = data.username;  // Сохраняем текущий никнейм
                                etUsername.setText(data.username);
                                etFirstName.setText(data.firstName);
                                etLastName.setText(data.lastName);
                                etCountry.setText(data.country);
                                etProfession.setText(data.profession);
                                etNote.setText(data.notes);

                                // Преобразуем Date в строку для отображения
                                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                if (data.birthDate != null) {
                                    etBirthday.setText(sdf.format(data.birthDate));
                                }

                                // Установка выбранного пола
                                if ("male".equals(data.gender)) {
                                    radioGroupGender.check(R.id.radioMale);
                                } else if ("female".equals(data.gender)) {
                                    radioGroupGender.check(R.id.radioFemale);
                                }
                            }
                        }
                    });
        }
    }

    private void savePersonalData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String username = etUsername.getText().toString().trim();
            String userId = currentUser.getUid();
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String country = etCountry.getText().toString().trim();
            String profession = etProfession.getText().toString().trim();
            String notes = etNote.getText().toString().trim();
            String selectedBirthday = etBirthday.getText().toString().trim();
            String gender = radioGroupGender.getCheckedRadioButtonId() == R.id.radioMale ? "male" : "female";

            // Проверка, изменился ли ник
            if (username.equals(currentUsername)) {
                // Если ник не изменился, пропускаем проверку уникальности
                updatePersonalData(userId, username, firstName, lastName, country, profession, notes, selectedBirthday, gender);
            } else {
                // Если ник изменился, проверяем его уникальность
                personalDataRepository.isUsernameUnique(username).thenAccept(isUnique -> {
                    if (!isUnique) {
                        etUsername.setError(getString(R.string.username_taken_error));
                        return; // Прерываем выполнение, если имя пользователя уже занято
                    }
                    // Если ник уникален, обновляем данные
                    updatePersonalData(userId, username, firstName, lastName, country, profession, notes, selectedBirthday, gender);
                }).exceptionally(e -> {
                    Toast.makeText(getContext(), getString(R.string.error_checking_username), Toast.LENGTH_SHORT).show();
                    return null;
                });
            }
        }
    }

    private void updatePersonalData(String userId, String username, String firstName, String lastName, String country,
                                    String profession, String notes, String selectedBirthday, String gender) {
        DocumentReference docRef = db.collection("personalData").document(userId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                PersonalData existingData = task.getResult().toObject(PersonalData.class);
                if (existingData != null) {
                    boolean isChanged = false;

                    // Проверка и обновление имени пользователя
                    if (!username.equals(existingData.username)) {
                        existingData.username = username;
                        isChanged = true;
                    }
                    if (!firstName.equals(existingData.firstName)) {
                        existingData.firstName = firstName;
                        isChanged = true;
                    }
                    if (!lastName.equals(existingData.lastName)) {
                        existingData.lastName = lastName;
                        isChanged = true;
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    if (!selectedBirthday.isEmpty()) { // Проверка на пустое значение
                        try {
                            Date newBirthday = sdf.parse(selectedBirthday);
                            if (newBirthday != null && !newBirthday.equals(existingData.birthDate)) {
                                existingData.birthDate = newBirthday;
                                isChanged = true;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                            return;
                        }
                    } else {
                        existingData.birthDate = null; // Установка null для пустой даты
                        isChanged = true;
                    }

                    if (!country.equals(existingData.country)) {
                        existingData.country = country;
                        isChanged = true;
                    }
                    if (!profession.equals(existingData.profession)) {
                        existingData.profession = profession;
                        isChanged = true;
                    }
                    if (!notes.equals(existingData.notes)) {
                        existingData.notes = notes;
                        isChanged = true;
                    }
                    if (!gender.equals(existingData.gender)) {
                        existingData.gender = gender;
                        isChanged = true;
                    }

                    if (isChanged) {
                        docRef.set(existingData).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(getContext(), R.string.data_saved_successfully, Toast.LENGTH_SHORT).show();
                                navigateToSettings(); // Переход после успешного сохранения
                            } else {
                                Toast.makeText(getContext(), getString(R.string.data_save_error) + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }



    private void navigateToSettings() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_settings);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }
}
