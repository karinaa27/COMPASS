package com.mgke.da;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private EditText signupEmail, signupPassword, signupPassword2; // Поле для подтверждения пароля
    private Button signupButton;
    private com.google.android.gms.common.SignInButton googleSignInButton;
    private TextView loginRedirectText;
    private static final int RC_SIGN_IN = 9001;
    private PersonalDataRepository personalDataRepository;

    public static final String PREFS_NAME = "UserPrefs"; // Имя SharedPreferences
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn"; // Ключ состояния входа

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            // Если уже вошёл, перенаправляем на главный экран
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
            finish();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        signupPassword2 = findViewById(R.id.signup_password2); // Поле для подтверждения пароля
        signupButton = findViewById(R.id.sign_up_button);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        // Установка фильтров для ввода
        setupInputFilters();

        signupButton.setOnClickListener(view -> {
            String user = signupEmail.getText().toString().trim();
            String pass = signupPassword.getText().toString().trim();
            String confirmPass = signupPassword2.getText().toString().trim(); // Получаем текст из поля подтверждения

            if (user.isEmpty() || !isValidEmail(user)) {
                signupEmail.setError("Введите корректный email");
                return;
            }

            if (pass.isEmpty()) {
                signupPassword.setError("Введите пароль");
                return;
            }

            if (!isValidPassword(pass)) {
                signupPassword.setError("Пароль должен содержать минимум 6 символов, включая заглавные, строчные буквы, цифры и специальный символ.");
                return;
            }

            if (!pass.equals(confirmPass)) { // Проверка совпадения паролей
                signupPassword2.setError("Пароли не совпадают");
                return;
            }

            auth.createUserWithEmailAndPassword(user, pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    sendVerificationEmail();
                    saveLoginState(); // Сохранение состояния входа
                    Toast.makeText(SignUpActivity.this, "Регистрация успешна", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                } else {
                    Toast.makeText(SignUpActivity.this, "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        googleSignInButton.setOnClickListener(view -> signInWithGoogle());

        loginRedirectText.setOnClickListener(v -> startActivity(new Intent(SignUpActivity.this, LoginActivity.class)));
    }

    private void saveLoginState() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true); // Установите состояние входа
        editor.apply();
    }

    private void setupInputFilters() {
        // Фильтры для email
        signupEmail.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, android.text.Spanned dest, int dstart, int dend) {
                return source.toString().replaceAll("[^\\p{L}\\p{N}_.@-]", ""); // Разрешаем только латиницу, цифры, точку, @ и -
            }
        }});

        // Фильтры для пароля
        signupPassword.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, android.text.Spanned dest, int dstart, int dend) {
                return source.toString().replaceAll("[^\\p{L}\\p{N}!" +
                        "@#$%^&*()\\-_=+\\[\\]{};:'\",.<>?/]", ""); // Разрешаем буквы, цифры и специальные символы
            }
        }});
        signupEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains(" ")) {
                    signupEmail.setText(s.toString().replace(" ", ""));
                    signupEmail.setSelection(signupEmail.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        signupPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains(" ")) {
                    signupPassword.setText(s.toString().replace(" ", ""));
                    signupPassword.setSelection(signupPassword.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        signupPassword2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains(" ")) {
                    signupPassword2.setText(s.toString().replace(" ", ""));
                    signupPassword2.setSelection(signupPassword2.getText().length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
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
            String idToken = account.getIdToken();
            firebaseAuthWithGoogle(idToken);
        } catch (ApiException e) {
            Toast.makeText(this, "Ошибка входа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendVerificationEmail() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SignUpActivity.this, "Письмо для подтверждения отправлено на " + user.getEmail(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Не удалось отправить письмо для подтверждения.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                PersonalData personalData = new PersonalData();
                personalDataRepository.addPersonalData(personalData);
                Toast.makeText(SignUpActivity.this, "Вход через Google успешен", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Ошибка аутентификации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 6) return false;

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
        return Patterns.EMAIL_ADDRESS.matcher(email).matches(); // Используется встроенная проверка email
    }
}