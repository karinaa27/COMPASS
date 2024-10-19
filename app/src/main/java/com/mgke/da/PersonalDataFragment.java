package com.mgke.da;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.mgke.da.models.PersonalData;

public class PersonalDataFragment extends Fragment {

    private EditText etFirstName, etLastName, etCountry, etProfession, etNote;
    private RadioGroup radioGroupGender;
    private Button buttonSave;
    private ImageView closeButton; // Кнопка закрытия
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration registration;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_personal_data, container, false);

        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etCountry = view.findViewById(R.id.etCountry);
        etProfession = view.findViewById(R.id.etProfession);
        etNote = view.findViewById(R.id.etNote);
        radioGroupGender = view.findViewById(R.id.radioGroupGender);
        buttonSave = view.findViewById(R.id.buttonSave);
        closeButton = view.findViewById(R.id.close); // Инициализация кнопки закрытия

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadPersonalData();

        // Обработчик клика для кнопки закрытия
        closeButton.setOnClickListener(v -> {
            navigateToSettings();
        });

        // Обработчик клика для кнопки сохранения
        buttonSave.setOnClickListener(v -> {
            savePersonalData();
            navigateToSettings(); // Переход после сохранения данных
        });

        return view;
    }

    private void loadPersonalData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            DocumentReference docRef = db.collection("personalData").document(currentUser.getUid());
            registration = docRef.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) {
                    // Обработка ошибки
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    // Получение данных
                    PersonalData data = documentSnapshot.toObject(PersonalData.class);
                    if (data != null) {
                        // Прямой доступ к полям класса
                        etFirstName.setText(data.firstName);
                        etLastName.setText(data.lastName);
                        etCountry.setText(data.country);
                        etProfession.setText(data.profession);
                        etNote.setText(data.notes);

                        // Установка значения радиокнопки
                        if ("Male".equals(data.gender)) {
                            radioGroupGender.check(R.id.radioMale);
                        } else if ("Female".equals(data.gender)) {
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
            String userId = currentUser.getUid();
            String firstName = etFirstName.getText().toString().trim();
            String lastName = etLastName.getText().toString().trim();
            String country = etCountry.getText().toString().trim();
            String profession = etProfession.getText().toString().trim();
            String notes = etNote.getText().toString().trim();
            String gender = getSelectedGender();

            PersonalData personalData = new PersonalData(userId, null, null, null, firstName, lastName, gender, null, country, profession, notes, null, null);

            db.collection("personalData").document(userId).set(personalData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Успешно сохранено
                    Toast.makeText(getContext(), "Данные успешно сохранены", Toast.LENGTH_SHORT).show();
                } else {
                    // Ошибка при сохранении
                    Toast.makeText(getContext(), "Ошибка сохранения данных: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getSelectedGender() {
        int selectedId = radioGroupGender.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMale) {
            return "Male";
        } else if (selectedId == R.id.radioFemale) {
            return "Female";
        }
        return null; // Если ничего не выбрано
    }

    private void navigateToSettings() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        navController.navigate(R.id.navigation_settings); // Убедитесь, что ID фрагмента настроек правильный
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }
}