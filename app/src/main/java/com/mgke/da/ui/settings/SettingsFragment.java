package com.mgke.da.ui.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mgke.da.activity.LoginActivity;
import com.mgke.da.R;
import com.mgke.da.databinding.FragmentSettingsBinding;
import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.PersonalDataRepository;
import java.util.Arrays;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private static final int GALLERY_REQUEST_CODE = 1000;
    private final String[] currencies = {"BYN", "USD", "RUB", "UAH", "PLN", "EUR"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        auth = FirebaseAuth.getInstance();
        setupLanguageSpinner();
        setupNightModeSwitch();
        setupCategoriesSettingsClick();
        setupExitButtonClick();
        setupDeleteAccountButtonClick();
        setupAvatarClick();
        setupPersonalDataClick();
        loadUserEmail();
        setupImagePickerLauncher();
        setupCurrencySpinner();
        setupPasswordSettingsClick();
        return view;
    }

    private void setupPasswordSettingsClick() {
        RelativeLayout passwordSettings = binding.PasswordSettings;
        passwordSettings.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void setupImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Glide.with(this).load(imageUri).into(binding.photoUser);
                            uploadImageToFirebase(imageUri);
                        } else {
                            showToast(R.string.error_image_retrieval);
                        }
                    }
                });
    }

    private void setupPersonalDataClick() {
        binding.PersonalData.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.PersonalDataFragment);
        });
    }

    private void setupAvatarClick() {
        binding.photoUser.setOnClickListener(v -> showChangePhotoDialog());
    }

    private void showChangePhotoDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.change_photo_title)
                .setMessage(R.string.change_photo_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> openGallery())
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void openGallery() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    GALLERY_REQUEST_CODE);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showToast(getString(R.string.error_user_not_authenticated));
            return;
        }
        String userId = currentUser.getUid();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("avatars/" + userId + ".jpg");
        storageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageReference.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                saveImageUrlToFirestore(imageUrl);
                            })
                            .addOnFailureListener(e -> {
                                showToast(getString(R.string.error_url_retrieval) + ": " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    showToast(getString(R.string.error_image_upload) + ": " + e.getMessage());
                });
    }
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void saveImageUrlToFirestore(String imageUrl) {
        String userId = auth.getCurrentUser().getUid();
        PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());

        personalDataRepository.getPersonalDataById(userId).whenComplete((personalData, throwable) -> {
            if (throwable != null) {
                return;
            }

            if (personalData != null) {
                personalData.avatarUrl = imageUrl;
                personalDataRepository.addOrUpdatePersonalData(personalData).whenComplete((aVoid, updateThrowable) -> {
                    if (updateThrowable == null) {
                        showToast(R.string.avatar_updated);
                    }
                });
            }
        });
    }

    private void setupExitButtonClick() {
        binding.exit.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.logout_title)
                    .setMessage(R.string.logout_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        auth.signOut();
                        showToast(R.string.logged_out);
                        navigateToLoginActivity();
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });
    }

    private void setupDeleteAccountButtonClick() {
        binding.DeleteUser.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.delete_account_title)
                    .setMessage(R.string.delete_account_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        FirebaseUser currentUser = auth.getCurrentUser();
                        if (currentUser != null) {
                            currentUser.delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    String userId = currentUser.getUid();
                                    PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
                                    personalDataRepository.deletePersonalData(userId);
                                    showToast(R.string.account_deleted2);
                                    navigateToLoginActivity();
                                } else {
                                    showToast(R.string.error_account_deletion);
                                }
                            });
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });
    }

    private void navigateToLoginActivity() {
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void setupCategoriesSettingsClick() {
        RelativeLayout categoriesSettings = binding.CategoriesSettings;
        categoriesSettings.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_settings_category);
        });
    }

    private void setupLanguageSpinner() {
        String[] languages = {getString(R.string.russian), getString(R.string.english)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.languageSpinner.setAdapter(adapter);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
        String selectedLanguage = sharedPreferences.getString("selectedLanguage", "en");
        int spinnerPosition = selectedLanguage.equals("ru") ? 0 : 1;
        binding.languageSpinner.setSelection(spinnerPosition);
        binding.languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String languageCode = position == 0 ? "ru" : "en";
                setLocale(languageCode);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("selectedLanguage", languageCode);
                editor.apply();
                updateUI();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupNightModeSwitch() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
        boolean nightMode = sharedPreferences.getBoolean("nightMode", false);
        AppCompatDelegate.setDefaultNightMode(nightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        binding.switchNightMode.setChecked(nightMode);

        binding.switchNightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("nightMode", isChecked);
            editor.apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void updateUI() {
        binding.textNightMode.setText(getString(R.string.night_mode));
        binding.textLanguage.setText(getString(R.string.language));
        binding.textCategories.setText(getString(R.string.categories_string_settings));


        String selectedLanguage = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE)
                .getString("selectedLanguage", "en");
        int spinnerPosition = selectedLanguage.equals("ru") ? 0 : 1;
        binding.languageSpinner.setSelection(spinnerPosition);
    }

    private void loadUserEmail() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
            personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
                if (personalData != null) {
                    binding.textUserEmail.setText(personalData.email);
                    if (personalData.username != null && !personalData.username.isEmpty()) {
                        binding.textUser.setText(personalData.username);
                    }
                    if (personalData.avatarUrl != null) {
                        Glide.with(this).load(personalData.avatarUrl).into(binding.photoUser);
                    }
                    loadUserCurrency(personalData);
                }
            }).exceptionally(e -> {
                return null;
            });
        }
    }

    private void setupCurrencySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.currencySpinner.setAdapter(adapter);

        binding.currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveCurrency();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadUserCurrency(PersonalData personalData) {
        String userCurrency = personalData.currency;
        int position = Arrays.asList(currencies).indexOf(userCurrency);
        binding.currencySpinner.setSelection(position >= 0 ? position : 1);
    }

    private void saveCurrency() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String selectedCurrency = binding.currencySpinner.getSelectedItem().toString();
            PersonalDataRepository repository = new PersonalDataRepository(FirebaseFirestore.getInstance());
            repository.getPersonalDataById(userId).thenAccept(personalData -> {
                if (personalData != null) {
                    personalData.currency = selectedCurrency;
                    repository.addOrUpdatePersonalData(personalData).thenRun(() -> {
                        showToast(R.string.currency_saved);
                    });
                }
            });
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        EditText currentPasswordInput = dialogView.findViewById(R.id.currentPassword);
        EditText newPasswordInput = dialogView.findViewById(R.id.newPassword);
        EditText confirmNewPasswordInput = dialogView.findViewById(R.id.confirmNewPassword);
        Button saveButton = dialogView.findViewById(R.id.saveButton);

        FirebaseUser currentUser = auth.getCurrentUser();
        final String userId;
        if (currentUser != null) {
            userId = currentUser.getUid();
            PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
            personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
                if (personalData != null && personalData.password != null && !personalData.password.isEmpty()) {
                    currentPasswordInput.setVisibility(View.VISIBLE);
                } else {
                    currentPasswordInput.setVisibility(View.GONE);
                }
            });
        } else {
            return;
        }
        AlertDialog dialog = builder.create();
        saveButton.setOnClickListener(v -> {
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmNewPassword = confirmNewPasswordInput.getText().toString().trim();
            if (!newPassword.equals(confirmNewPassword)) {
                showToast(R.string.passwords_do_not_match);
                return;
            }
            if (currentUser != null && currentPasswordInput.getVisibility() == View.VISIBLE) {
                String currentPassword = currentPasswordInput.getText().toString().trim();
                currentUser.updatePassword(newPassword).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveNewPasswordToFirestore(userId, newPassword);
                        showToast(R.string.password_changed_successfully);
                        dialog.dismiss();
                    } else {
                        showToast(R.string.error_password_change);
                    }
                });
            } else {
                currentUser.updatePassword(newPassword).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveNewPasswordToFirestore(userId, newPassword);
                        showToast(R.string.password_changed_successfully);
                        dialog.dismiss();
                    } else {
                        showToast(R.string.error_password_change);
                    }
                });
            }
        });

        dialog.show();
    }

    private void saveNewPasswordToFirestore(String userId, String newPassword) {
        PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
        personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
            if (personalData != null) {
                personalData.password = newPassword;
                personalDataRepository.addOrUpdatePersonalData(personalData).thenRun(() -> {
                    showToast(R.string.new_password_saved);
                }).exceptionally(e -> {
                    return null;
                });
            }
        });
    }

    private void showToast(int messageId) {
        Toast.makeText(getContext(), messageId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}