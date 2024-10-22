package com.mgke.da.ui.personal_data;

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
import com.mgke.da.R;
import com.mgke.da.models.PersonalData;

public class PersonalDataFragment extends Fragment {
    private EditText etFirstName, etLastName, etCountry, etProfession, etNote;
    private RadioGroup radioGroupGender;
    private Button buttonSave;
    private ImageView closeButton;
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
        closeButton = view.findViewById(R.id.close);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        loadPersonalData();
        closeButton.setOnClickListener(v -> navigateToSettings());
        buttonSave.setOnClickListener(v -> {
            savePersonalData();
            navigateToSettings();
        });
        return view;
    }

    private void loadPersonalData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            DocumentReference docRef = db.collection("personalData").document(currentUser.getUid());
            registration = docRef.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) return;
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    PersonalData data = documentSnapshot.toObject(PersonalData.class);
                    if (data != null) {
                        etFirstName.setText(data.firstName);
                        etLastName.setText(data.lastName);
                        etCountry.setText(data.country);
                        etProfession.setText(data.profession);
                        etNote.setText(data.notes);
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
                    Toast.makeText(getContext(), R.string.data_saved_successfully, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), R.string.data_save_error + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getSelectedGender() {
        int selectedId = radioGroupGender.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMale) {
            return getString(R.string.gender_male);
        } else if (selectedId == R.id.radioFemale) {
            return getString(R.string.gender_female);
        }
        return null;
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