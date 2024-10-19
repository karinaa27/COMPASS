package com.mgke.da;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.PersonalDataRepository;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private EditText signupEmail, signupPassword, signupPassword2, signupUsername, signupFirstName, signupLastName, signupBirthDate, signupCountry, signupCurrency;
    private Button signupButton;
    private com.google.android.gms.common.SignInButton googleSignInButton;
    private TextView loginRedirectText;
    private static final int RC_SIGN_IN = 9001;
    private PersonalDataRepository personalDataRepository;
    private FirebaseFirestore firestore;

    public static final String PREFS_NAME = "UserPrefs";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private boolean isGoogleSignUp = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance(); // Инициализация Firestore
        personalDataRepository = new PersonalDataRepository(firestore); // Передача Firestore в репозиторий

        FirebaseUser currentUser = auth.getCurrentUser();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        if (currentUser != null) {
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        }

        // Инициализация полей ввода
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupPassword2 = findViewById(R.id.signup_password2);
        signupUsername = findViewById(R.id.signup_username);
        signupButton = findViewById(R.id.sign_up_button);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        signupFirstName = findViewById(R.id.signup_first_name);
        signupLastName = findViewById(R.id.signup_last_name);
        signupBirthDate = findViewById(R.id.signup_birth_date);
        signupCountry = findViewById(R.id.signup_country);
        signupCurrency = findViewById(R.id.signup_currency);

        signupBirthDate.setOnClickListener(view -> showDatePickerDialog());
        signupCountry.setOnClickListener(view -> showCountryPickerDialog());

        // Установите фильтры для имени и фамилии
        InputFilter nameFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char character = source.charAt(i);
                if (!Character.isLetter(character) && !Character.isWhitespace(character)) {
                    return ""; // Запретить ввод небуквенных символов
                }
            }
            return null;
        };

        signupFirstName.setFilters(new InputFilter[]{nameFilter, new InputFilter.LengthFilter(30)});
        signupLastName.setFilters(new InputFilter[]{nameFilter, new InputFilter.LengthFilter(30)});

        setupInputFilters();

        signupButton.setOnClickListener(view -> {
            if (isGoogleSignUp) {
                signInWithGoogle();
            } else {
                registerUser();
            }
        });

        googleSignInButton.setOnClickListener(view -> {
            isGoogleSignUp = true;
            signInWithGoogle();
        });
        signupCurrency.setOnClickListener(view -> showCurrencyPickerDialog());
        loginRedirectText.setOnClickListener(v -> startActivity(new Intent(SignUpActivity.this, LoginActivity.class)));
    }

    private void registerUser() {
        String user = signupEmail.getText().toString().trim();
        String pass = signupPassword.getText().toString().trim();
        String confirmPass = signupPassword2.getText().toString().trim();
        String username = signupUsername.getText().toString().trim();
        String firstName = signupFirstName.getText().toString().trim();
        String lastName = signupLastName.getText().toString().trim();
        String country = signupCountry.getText().toString().trim();
        String gender = getSelectedGender();
        String currency = signupCurrency.getText().toString().trim();

        // Получение даты рождения
        Date birthDate = getBirthDateFromInput();
        if (birthDate == null) {
            Toast.makeText(this, "Введите корректную дату рождения", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверка обязательных полей
        if (user.isEmpty() || !isValidEmail(user)) {
            signupEmail.setError(getString(R.string.email_error));
            return;
        }

        if (pass.isEmpty() || !isValidPassword(pass)) {
            signupPassword.setError(getString(R.string.password_error));
            return;
        }

        if (!pass.equals(confirmPass)) {
            signupPassword2.setError(getString(R.string.password_mismatch));
            return;
        }

        if (username.isEmpty()) {
            signupUsername.setError(getString(R.string.username_error));
            return;
        }

        if (!isFirstLetterUppercase(firstName)) {
            signupFirstName.setError(getString(R.string.first_name_error));
            return;
        }

        if (!isFirstLetterUppercase(lastName)) {
            signupLastName.setError(getString(R.string.last_name_error));
            return;
        }

        isUsernameTaken(username).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (task.getResult()) {
                    signupUsername.setError(getString(R.string.username_taken_error));
                    return;
                }

                // Создание пользователя
                auth.createUserWithEmailAndPassword(user, pass).addOnCompleteListener(innerTask -> {
                    if (innerTask.isSuccessful()) {
                        sendVerificationEmail(); // Отправка письма с подтверждением
                        saveLoginState();

                        String userId = auth.getCurrentUser().getUid();
                        PersonalData personalData = new PersonalData(userId, username, pass, user, firstName, lastName, gender, birthDate, country, null, null, null, currency);
                        personalDataRepository.addOrUpdatePersonalData(personalData);

                        Toast.makeText(SignUpActivity.this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show();

                        // Перенаправление на экран входа
                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish(); // Закрываем текущую активити
                    } else {
                        Toast.makeText(SignUpActivity.this, getString(R.string.registration_error, innerTask.getException().getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(SignUpActivity.this, getString(R.string.error_checking_username), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private Date getBirthDateFromInput() {
        String dateString = signupBirthDate.getText().toString().trim();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try {
            return sdf.parse(dateString); // Возвращаем Date
        } catch (ParseException e) {
            e.printStackTrace();
            // Обработка ошибки
            return null; // Возвращаем null или обрабатываем иначе
        }
    }
    private boolean isFirstLetterUppercase(String name) {
        return name.length() > 0 && Character.isUpperCase(name.charAt(0));
    }

    // Проверка уникальности имени пользователя
    private Task<Boolean> isUsernameTaken(String username) {
        return firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .continueWith(task -> !task.getResult().isEmpty());
    }

    // Метод для получения выбранного пола
    private String getSelectedGender() {
        RadioGroup radioGroup = findViewById(R.id.radioGroupGender);
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMale) {
            return "Male";
        } else if (selectedId == R.id.radioFemale) {
            return "Female";
        }
        return null; // Если ничего не выбрано
    }

    private void saveLoginState() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    private void setupInputFilters() {
        signupEmail.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) ->
                source.toString().replaceAll("[^\\p{L}\\p{N}_.@-]", "")
        });

        signupPassword.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) ->
                source.toString().replaceAll("[^\\p{L}\\p{N}!" +
                        "@#$%^&*()\\-_=+\\[\\]{};:'\",.<>?/]", "")
        });

        setupTextChangedListeners();
    }

    private void setupTextChangedListeners() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains(" ")) {
                    ((EditText) getCurrentFocus()).setText(s.toString().replace(" ", ""));
                    ((EditText) getCurrentFocus()).setSelection(((EditText) getCurrentFocus()).getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        signupEmail.addTextChangedListener(textWatcher);
        signupPassword.addTextChangedListener(textWatcher);
        signupPassword2.addTextChangedListener(textWatcher);
    }

    private void signInWithGoogle() {
        if (googleSignInClient != null) {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        } else {
            Toast.makeText(this, getString(R.string.google_sign_in_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String idToken = account.getIdToken();
                firebaseAuthWithGoogle(idToken);
            }
        } catch (ApiException e) {
            Toast.makeText(this, getString(R.string.sign_in_error, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear; // Формат: ДД/ММ/ГГГГ
                    signupBirthDate.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showCountryPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_country)
                .setItems(R.array.countries_array, (dialog, which) -> {
                    String[] countries = getResources().getStringArray(R.array.countries_array);
                    signupCountry.setText(countries[which]);
                })
                .setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void sendVerificationEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(SignUpActivity.this, getString(R.string.verification_email_sent, user.getEmail()), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SignUpActivity.this, getString(R.string.verification_email_failed), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    String email = user.getEmail();
                    if (email != null) {
                        // Проверяем, существует ли аккаунт с таким email в Firestore
                        checkUserInFirestore(email).addOnCompleteListener(checkTask -> {
                            if (checkTask.isSuccessful()) {
                                PersonalData personalData = checkTask.getResult();
                                if (personalData != null && personalData.getPassword() != null) {
                                    // Если существует аккаунт с паролем, переходим на экран входа
                                    Toast.makeText(SignUpActivity.this, getString(R.string.account_exists_with_password), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                    finish();
                                } else {
                                    // Новый пользователь, продолжаем регистрацию
                                    String id = user.getUid();
                                    PersonalData newPersonalData = new PersonalData(
                                            id,
                                            user.getDisplayName() != null ? user.getDisplayName() : "",
                                            "",  // Пароль не нужен
                                            email,
                                            null,  // First Name
                                            null,  // Last Name
                                            null,  // Gender
                                            null,  // Birth Date
                                            null,  // Country
                                            null,  // Profession
                                            null,  // Notes
                                            null,
                                            "USD"
                                    );
                                    personalDataRepository.addOrUpdatePersonalData(newPersonalData);

                                    Toast.makeText(SignUpActivity.this, getString(R.string.google_sign_in_success), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                    finish();
                                }
                            }
                        });
                    }
                }
            } else {
                Toast.makeText(this, getString(R.string.auth_error, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Task<PersonalData> checkUserInFirestore(String email) {
        return firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Предполагается, что у вас только один пользователь с таким email
                        return task.getResult().getDocuments().get(0).toObject(PersonalData.class);
                    }
                    return null; // Пользователь не найден
                });
    }
    private void checkUserPassword(String email, String idToken) {
        // Получаем данные пользователя по электронной почте
        personalDataRepository.getUserByEmail(email).thenAccept(personalData -> {
            // Проверяем, существует ли пользователь и есть ли у него пароль
            if (personalData != null && personalData.password != null) {
                // Если пароль существует, переходим на экран логина
                Toast.makeText(SignUpActivity.this, getString(R.string.account_exists_with_password), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                finish(); // Закрываем текущую активити
            } else {
                // Если пароля нет, выполняем вход через Google
                firebaseAuthWithGoogle(idToken);
            }
        }).exceptionally(e -> {
            // В случае ошибки, выполняем вход через Google
            // Можно добавить логирование ошибки здесь
            System.err.println("Error fetching user data: " + e.getMessage());
            firebaseAuthWithGoogle(idToken);
            return null; // Возвращаем null, чтобы соответствовать типу
        });
    }
    private void showCurrencyPickerDialog() {
        String[] currencies = {"USD", "EUR", "RUB", "UAH", "PLN"}; // Ваши валюты

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_currency)
                .setItems(currencies, (dialog, which) -> {
                    signupCurrency.setText(currencies[which]);
                })
                .setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    private boolean isValidPassword(String password) {
        int minLength = 8;
        int maxLength = 20;

        if (password.length() < minLength || password.length() > maxLength) {
            return false;
        }

        Pattern upperCase = Pattern.compile("[A-Z]");
        Pattern lowerCase = Pattern.compile("[a-z]");
        Pattern digit = Pattern.compile("[0-9]");
        Pattern specialChar = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");

        Matcher hasUpperCase = upperCase.matcher(password);
        Matcher hasLowerCase = lowerCase.matcher(password);
        Matcher hasDigit = digit.matcher(password);
        Matcher hasSpecialChar = specialChar.matcher(password);

        return hasUpperCase.find() && hasLowerCase.find() && hasDigit.find() && hasSpecialChar.find();
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}