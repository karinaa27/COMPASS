package com.mgke.da.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mgke.da.R;
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
import java.util.Calendar;
import java.util.Date;
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

    private boolean isGoogleSignUp = false;

    public static final String PREFS_NAME = "UserPrefs";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        Log.d("ActivityLifecycle", "SignUpActivity onCreate called");
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        personalDataRepository = new PersonalDataRepository(firestore);

        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupPassword2 = findViewById(R.id.signup_password2);
        signupUsername = findViewById(R.id.signup_username);
        signupFirstName = findViewById(R.id.signup_first_name);
        signupLastName = findViewById(R.id.signup_last_name);
        signupBirthDate = findViewById(R.id.signup_birth_date);
        signupCountry = findViewById(R.id.signup_country);
        signupCurrency = findViewById(R.id.signup_currency);
        signupButton = findViewById(R.id.sign_up_button);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        // Проверка на уже авторизованного пользователя
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Подсветка пустых обязательных полей при создании активности
        highlightEmptyFields();

        // Убираем подсветку, когда пользователь вводит данные
        setupTextChangedListeners();

        signupBirthDate.setOnClickListener(view -> showDatePickerDialog());
        signupCountry.setOnClickListener(view -> showCountryPickerDialog());

        InputFilter nameFilter = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char character = source.charAt(i);
                if (!Character.isLetter(character) && !Character.isWhitespace(character)) {
                    return "";
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
        loginRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    // Метод для подсветки пустых обязательных полей
    private void highlightEmptyFields() {
        if (signupEmail.getText().toString().trim().isEmpty()) {
            signupEmail.setError(getString(R.string.email_error));
        }
        if (signupPassword.getText().toString().trim().isEmpty()) {
            signupPassword.setError(getString(R.string.password_error));
        }
        if (signupPassword2.getText().toString().trim().isEmpty()) {
            signupPassword2.setError("Пароли не совпадают");
        }
        if (signupUsername.getText().toString().trim().isEmpty()) {
            signupUsername.setError(getString(R.string.username_error));
        }
    }

    // Метод для удаления подсветки ошибок, когда пользователь вводит данные
    private void setupTextChangedListeners() {
        TextWatcher fieldTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!signupEmail.getText().toString().trim().isEmpty()) {
                    signupEmail.setError(null);
                }
                if (!signupPassword.getText().toString().trim().isEmpty()) {
                    signupPassword.setError(null);
                }
                if (!signupPassword2.getText().toString().trim().isEmpty()) {
                    signupPassword2.setError(null);
                }
                if (!signupUsername.getText().toString().trim().isEmpty()) {
                    signupUsername.setError(null);
                }
            }
        };

        signupEmail.addTextChangedListener(fieldTextWatcher);
        signupPassword.addTextChangedListener(fieldTextWatcher);
        signupPassword2.addTextChangedListener(fieldTextWatcher);
        signupUsername.addTextChangedListener(fieldTextWatcher);
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

        Date birthDate = getBirthDateFromInput();

        // Валидация полей
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

        // Проверка уникальности имени пользователя
        personalDataRepository.isUsernameUnique(username).thenAccept(isUnique -> {
            if (!isUnique) {
                signupUsername.setError(getString(R.string.username_taken_error));
                return;
            }

            // Если имя пользователя уникально, продолжаем регистрацию
            auth.createUserWithEmailAndPassword(user, pass).addOnCompleteListener(innerTask -> {
                if (innerTask.isSuccessful()) {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser != null) {
                        sendVerificationEmail(); // Отправка письма для подтверждения
                        String userId = firebaseUser.getUid();
                        PersonalData personalData = new PersonalData(userId, username, pass, user, firstName, lastName, gender, birthDate, country, null, null, null, currency);
                        personalDataRepository.addOrUpdatePersonalData(personalData);

                        // Уведомляем пользователя о том, что письмо отправлено
                        Toast.makeText(SignUpActivity.this, getString(R.string.verification_email_sent), Toast.LENGTH_SHORT).show();

                        // Перенаправляем пользователя на LoginActivity
                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish(); // Завершаем текущую активность, чтобы предотвратить возврат к ней
                    }
                } else {
                    Toast.makeText(SignUpActivity.this, getString(R.string.registration_error, innerTask.getException().getMessage()), Toast.LENGTH_SHORT).show();
                }
            });
        }).exceptionally(e -> {
            Toast.makeText(SignUpActivity.this, getString(R.string.error_checking_username), Toast.LENGTH_SHORT).show();
            return null;
        });
    }



    private Date getBirthDateFromInput() {
        String dateString = signupBirthDate.getText().toString().trim();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean isFirstLetterUppercase(String name) {
        return name.length() > 0 && Character.isUpperCase(name.charAt(0));
    }
    private Task<Boolean> isUsernameTaken(String username) {
        return firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .continueWith(task -> !task.getResult().isEmpty());
    }
    private String getSelectedGender() {
        RadioGroup radioGroup = findViewById(R.id.radioGroupGender);
        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMale) {
            return "Male";
        } else if (selectedId == R.id.radioFemale) {
            return "Female";
        }
        return null;
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
                    String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    signupBirthDate.setText(date);
                }, year, month, day);
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());

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
                        checkUserInFirestore(email).addOnCompleteListener(checkTask -> {
                            if (checkTask.isSuccessful()) {
                                PersonalData personalData = checkTask.getResult();
                                if (personalData != null && personalData.getPassword() != null) {
                                    Toast.makeText(SignUpActivity.this, getString(R.string.account_exists_with_password), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                    finish();
                                } else {
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
                        return task.getResult().getDocuments().get(0).toObject(PersonalData.class);
                    }
                    return null;
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