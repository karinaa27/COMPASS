package com.mgke.da.ui.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mgke.da.activity.LoginActivity;
import com.mgke.da.R;
import com.mgke.da.activity.SignUpActivity;
import com.mgke.da.databinding.FragmentSettingsBinding;
import com.mgke.da.models.Account;
import com.mgke.da.models.Category;
import com.mgke.da.models.Comment;
import com.mgke.da.models.Goal;
import com.mgke.da.models.PersonalData;
import com.mgke.da.models.Transaction;
import com.mgke.da.repository.AccountRepository;
import com.mgke.da.repository.CategoryRepository;
import com.mgke.da.repository.CommentRepository;
import com.mgke.da.repository.GoalRepository;
import com.mgke.da.repository.LikeRepository;
import com.mgke.da.repository.PersonalDataRepository;
import com.mgke.da.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class SettingsFragment extends Fragment {

    private ProgressDialog loadingDialog;
    private FragmentSettingsBinding binding;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private static final int GALLERY_REQUEST_CODE = 1000;
    private final String[] currencies = {"BYN", "USD", "RUB", "UAH", "PLN", "EUR"};
    private FirebaseAuth.AuthStateListener authStateListener;
    private static final int RC_GOOGLE_SIGN_IN = 9001;
    private Button googleSignInButton;
    private TextView googleAuthMessage;
    private EditText newEmailInput, currentEmail;
    private Button saveButton;
    private GoogleSignInClient googleSignInClient;
PersonalDataRepository personalDataRepository;
GoalRepository goalRepository;
CategoryRepository categoryRepository;
AccountRepository accountRepository;
TransactionRepository transactionRepository;
LikeRepository likeRepository;
CommentRepository commentRepository;

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
        setupEmailSettingsClick();
        setupPersonalDataClick();
        loadUserEmail();
        setupImagePickerLauncher();
        setupCurrencySpinner();
        setupPasswordSettingsClick();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id)) // Убедитесь, что у вас есть правильный ID клиента
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
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
                            showLoadingDialog(); // Показываем диалог загрузки
                            uploadImageToFirebase(imageUri); // Начинаем загрузку
                        } else {
                            showToast(R.string.error_image_retrieval);
                        }
                    }
                });
    }

    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showToast(getString(R.string.error_user_not_authenticated));
            hideLoadingDialog(); // Закрываем диалог при ошибке
            return;
        }

        String userId = currentUser.getUid();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("avatars/" + userId + ".jpg");

        storageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String imageUrl = uri.toString();
                            saveImageUrlToFirestore(imageUrl);
                            hideLoadingDialog(); // Закрываем диалог после успешной загрузки
                            showToast(R.string.avatar_updated);
                        })
                        .addOnFailureListener(e -> {
                            showToast(getString(R.string.error_url_retrieval) + ": " + e.getMessage());
                            hideLoadingDialog(); // Закрываем диалог при ошибке
                        }))
                .addOnFailureListener(e -> {
                    showToast(getString(R.string.error_image_upload) + ": " + e.getMessage());
                    hideLoadingDialog(); // Закрываем диалог при ошибке
                });
    }

    private void showLoadingDialog() {
        loadingDialog = new ProgressDialog(getContext());
        loadingDialog.setMessage(getString(R.string.image_upload_progress));
        loadingDialog.setCancelable(false); // Диалог нельзя закрыть кнопкой "Назад"
        loadingDialog.show();
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
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

    private void setupEmailSettingsClick() {
        RelativeLayout emailSettings = binding.EmailSettings;
        emailSettings.setOnClickListener(v -> showChangeEmailDialog());
    }

    private void showChangeEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_email, null);
        builder.setView(dialogView);

        googleSignInButton = dialogView.findViewById(R.id.googleSignInButton);
        googleAuthMessage = dialogView.findViewById(R.id.googleAuthMessage);
        newEmailInput = dialogView.findViewById(R.id.newEmail);
        currentEmail = dialogView.findViewById(R.id.currentEmail);
        saveButton = dialogView.findViewById(R.id.saveButton);

        newEmailInput.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            currentEmail.setText(currentUser.getEmail());

            boolean isGoogleSignIn = false;
            for (UserInfo info : currentUser.getProviderData()) {
                if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                    isGoogleSignIn = true;
                    break;
                }
            }

            if (isGoogleSignIn) {
                googleSignInButton.setVisibility(View.VISIBLE);
                googleAuthMessage.setVisibility(View.VISIBLE);
                googleSignInButton.setOnClickListener(v -> requestNewGoogleSignInToken());
            } else {
                newEmailInput.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);
            }
        }

        AlertDialog dialog = builder.create();

        saveButton.setOnClickListener(v -> {
            String newEmail = newEmailInput.getText().toString().trim();
            if (TextUtils.isEmpty(newEmail)) {
                showToast("Введите новую почту");
                return;
            }
            // Используем уже созданный dialog
            reauthenticateAndChangeEmail(newEmail, dialog); // передаем диалог как параметр
        });

        dialog.show();
    }


    private void reauthenticateAndChangeEmail(String newEmail, AlertDialog dialog) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Пользователь не авторизован");
            return;
        }

        boolean isGoogleSignIn = false;
        for (UserInfo info : currentUser.getProviderData()) {
            if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                isGoogleSignIn = true;
                break;
            }
        }

        if (isGoogleSignIn) {
            // Повторная аутентификация через Google
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getContext());
            if (account != null) {
                AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                reauthenticateWithCredential(credential, newEmail, dialog);
            } else {
                showToast("Ошибка: учетная запись Google недоступна.");
            }
        } else {
            // Повторная аутентификация через email и пароль
            fetchPasswordFromFirestoreAndReauthenticate(newEmail, dialog);
        }
    }

    private void reauthenticateWithCredential(AuthCredential credential, String newEmail, AlertDialog dialog) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                updateEmail(newEmail, dialog);
            } else {
                showToast("Ошибка повторной аутентификации: " + task.getException().getMessage());
            }
        });
    }
    private void updateEmail(String newEmail, final AlertDialog dialog) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Пользователь не авторизован.");
            return;
        }

        // Отправка письма для подтверждения нового email
        currentUser.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                showToast("На почту " + newEmail + " отправлено письмо для подтверждения.");

                // Закрытие диалога перед выходом из аккаунта и переходом в LoginActivity
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss(); // Закрываем диалог
                }

                // После успешной отправки письма выход из аккаунта и переход в LoginActivity
                FirebaseAuth.getInstance().signOut(); // Осуществляем выход
                navigateToLoginActivity(); // Переход в LoginActivity
            } else {
                showToast("Ошибка при отправке письма для подтверждения: " + task.getException().getMessage());
            }
        });
    }


    private void checkIfEmailConfirmed(final AlertDialog dialog, final String newEmail) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        currentUser.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean isVerified = currentUser.isEmailVerified();
                String currentEmail = currentUser.getEmail();
                if (isVerified && newEmail.equals(currentEmail)) {
                    // Email подтвержден и совпадает с новым email
                    updateEmailInFirestore(newEmail);
                    showToast("Почта подтверждена и обновлена.");
                    dialog.dismiss();  // Закрываем диалог после подтверждения почты
                } else {
                    showToast("Почта еще не подтверждена или не совпадает. Пожалуйста, проверьте свою почту.");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> checkIfEmailConfirmed(dialog, newEmail), 5000);
                }
            } else {
                showToast("Ошибка при проверке статуса почты.");
            }
        });
    }

    private void updateEmailInFirestore(String newEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DocumentReference userRef = db.collection("personalData").document(currentUser.getUid());
            userRef.update("email", newEmail)
                    .addOnSuccessListener(aVoid -> showToast("Email успешно обновлен в базе данных"))
                    .addOnFailureListener(e -> showToast("Ошибка при обновлении email в базе данных"));
        }
    }

    private void fetchPasswordFromFirestoreAndReauthenticate(final String newEmail, final AlertDialog dialog) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Пользователь не авторизован");
            return;
        }

        FirebaseFirestore.getInstance().collection("personalData")
                .document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null) {
                            String password = document.getString("password"); // убедитесь, что пароль существует
                            if (password == null || password.isEmpty()) {
                                showToast("Пароль не найден.");
                                return;
                            }

                            AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
                            reauthenticateWithCredential(credential, newEmail, dialog);
                        } else {
                            showToast("Ошибка при получении данных пользователя.");
                        }
                    } else {
                        showToast("Ошибка при обращении к базе данных.");
                    }
                });
    }

    private void requestNewGoogleSignInToken() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
                    reauthenticateWithCredential(credential, () -> {
                        // Если повторная аутентификация успешна, показываем поля для изменения почты
                        googleSignInButton.setVisibility(View.GONE);
                        googleAuthMessage.setVisibility(View.GONE);
                        newEmailInput.setVisibility(View.VISIBLE);
                        saveButton.setVisibility(View.VISIBLE);
                    });
                }
            } catch (ApiException e) {
                showToast("Ошибка повторного входа через Google: " + e.getMessage());
            }
        }
    }

    private void reauthenticateWithCredential(AuthCredential credential, Runnable onSuccess) {
        FirebaseAuth.getInstance().getCurrentUser().reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        onSuccess.run();
                    } else {
                        showToast("Ошибка повторной аутентификации: " + task.getException().getMessage());
                    }
                });
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

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void setupExitButtonClick() {
        binding.ExitUser.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.logout_title)
                    .setMessage(R.string.logout_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        auth.signOut(); // Выход из аккаунта
                        showToast(R.string.logged_out); // Сообщение об успешном выходе
                        navigateToLoginActivity(); // Переход к LoginActivity
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
                            String userId = currentUser.getUid();
                            TransactionRepository transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());
                            CategoryRepository categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());
                            GoalRepository goalRepository = new GoalRepository(FirebaseFirestore.getInstance());
                            AccountRepository accountRepository = new AccountRepository(FirebaseFirestore.getInstance());

                            // Получаем все транзакции пользователя
                            transactionRepository.getTransactionsForUserId(userId).thenAccept(transactions -> {
                                // Удаляем каждую транзакцию с использованием Task
                                List<Task<Void>> deleteTransactionTasks = new ArrayList<>();
                                for (Transaction transaction : transactions) {
                                    deleteTransactionTasks.add(transactionRepository.deleteTransaction(transaction.id));
                                }

                                // Ждем завершения всех операций удаления транзакций
                                Tasks.whenAllSuccess(deleteTransactionTasks).addOnCompleteListener(task -> {
                                    // Получаем все категории пользователя
                                    categoryRepository.getAllCategories(userId).thenAccept(categories -> {
                                        // Удаляем каждую категорию с использованием Task
                                        List<Task<Void>> deleteCategoryTasks = new ArrayList<>();
                                        for (Category category : categories) {
                                            deleteCategoryTasks.add(categoryRepository.deleteCategory(category.id));
                                        }

                                        // Ждем завершения всех операций удаления категорий
                                        Tasks.whenAllSuccess(deleteCategoryTasks).addOnCompleteListener(taskDeleteCategories -> {
                                            // Удаляем все цели пользователя
                                            goalRepository.getUserGoals(userId).thenAccept(goals -> {
                                                // Удаляем каждую цель
                                                List<CompletableFuture<Void>> deleteGoalFutures = new ArrayList<>();
                                                for (Goal goal : goals) {
                                                    deleteGoalFutures.add(goalRepository.deleteGoal(goal.id));
                                                }

                                                // Ждем завершения всех операций удаления целей
                                                CompletableFuture.allOf(deleteGoalFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
                                                    // Получаем все счета пользователя
                                                    accountRepository.getAccountsByUserId(userId).thenAccept(accounts -> {
                                                        // Удаляем каждый счет
                                                        List<CompletableFuture<Void>> deleteAccountFutures = new ArrayList<>();
                                                        for (Account account : accounts) {
                                                            deleteAccountFutures.add(accountRepository.deleteAccount(account.id));
                                                        }

                                                        // Ждем завершения всех операций удаления счетов
                                                        CompletableFuture.allOf(deleteAccountFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
                                                            // Теперь удаляем пользователя
                                                            currentUser.delete().addOnCompleteListener(taskDelete -> {
                                                                if (taskDelete.isSuccessful()) {
                                                                    PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
                                                                    personalDataRepository.deletePersonalData(userId);
                                                                    showToast(R.string.account_deleted2);
                                                                    navigateToLoginActivity();
                                                                } else {
                                                                    showToast(R.string.error_account_deletion);
                                                                }
                                                            });
                                                        });
                                                    });
                                                });
                                            });
                                        });
                                    });
                                });
                            });
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });
    }


    private void navigateToLoginActivity() {
        Intent intent = new Intent(getActivity(), SignUpActivity.class);
        startActivity(intent);
        requireActivity().finish(); // Закрываем текущую активность
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
        binding.textCurrency.setText(getString(R.string.Currency));
        binding.textPersonalData.setText(getString(R.string.personaldata));

        binding.textPassword.setText(getString(R.string.password));
        binding.textSettings.setText(getString(R.string.settings));
        binding.textExit.setText(getString(R.string.exit));
        binding.textDeleteUser.setText(getString(R.string.DeleteUser));
        String[] languages = {getString(R.string.russian), getString(R.string.english)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.languageSpinner.setAdapter(adapter);

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

            if (!isValidPassword(newPassword)) {
                showToast(R.string.invalid_password);
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

    // Метод для проверки сложности пароля
    private boolean isValidPassword(String password) {
        // Минимум 8 символов, одна строчная, одна заглавная буква, одна цифра и один специальный символ
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$";
        return password.matches(passwordPattern);
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