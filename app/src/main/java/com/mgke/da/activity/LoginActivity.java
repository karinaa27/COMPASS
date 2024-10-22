package com.mgke.da.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.mgke.da.R;
import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.PersonalDataRepository;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private EditText loginEmail, loginPassword;
    private com.google.android.gms.common.SignInButton googleSignInButton;
    private TextView signupRedirectText, forgotPasswordText;
    public static final String PREFS_NAME = "UserPrefs";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private PersonalDataRepository personalDataRepository;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        initializeGoogleSignIn();
        checkLoginStatus();
        setupClickListeners();
    }

    private void initializeViews() {
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        Button loginButton = findViewById(R.id.login_button);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeGoogleSignIn() {
        personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void checkLoginStatus() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);

        if (isLoggedIn) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void setupClickListeners() {
        Button loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> loginUser());

        googleSignInButton.setOnClickListener(v -> signInWithGoogle());

        signupRedirectText.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        forgotPasswordText.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {
        String email = loginEmail.getText().toString();
        String password = loginPassword.getText().toString();

        if (validateInput(email, password)) {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null && user.isEmailVerified()) {
                            navigateToMain();
                        } else {
                            Toast.makeText(LoginActivity.this, getString(R.string.verify_email), Toast.LENGTH_LONG).show();
                            auth.signOut();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, getString(R.string.login_failed, e.getMessage()), Toast.LENGTH_SHORT).show());
        }
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            loginEmail.setError(getString(R.string.email_empty));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError(getString(R.string.email_invalid));
            return false;
        } else if (password.isEmpty()) {
            loginPassword.setError(getString(R.string.password_empty));
            return false;
        }
        return true;
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
            if (account != null) {
                checkUserPassword(account.getEmail(), account.getIdToken());
            }
        } catch (ApiException e) {
            Toast.makeText(this, getString(R.string.login_error, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkUserPassword(String email, String idToken) {
        personalDataRepository.getUserByEmail(email).thenAccept(personalData -> {
            if (personalData != null && personalData.password != null) {
                // Если пароль существует, заполняем поля и переводим фокус на пароль
                loginEmail.setText(email);
                loginPassword.requestFocus(); // Переводим фокус на поле пароля
            } else {
                // Если пароля нет, выполняем вход через Google
                firebaseAuthWithGoogle(idToken);
            }
        }).exceptionally(e -> {
            // В случае ошибки, выполняем вход через Google
            firebaseAuthWithGoogle(idToken);
            return null; // Возвращаем null, чтобы соответствовать типу
        });
    }

    private void promptForPassword(String email, String idToken) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.enter_password));

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton(getString(R.string.login), (dialog, which) -> {
            String password = input.getText().toString();
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            navigateToMain();
                        } else {
                            Toast.makeText(LoginActivity.this, getString(R.string.login_failed, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    saveUserData(user);
                    Toast.makeText(LoginActivity.this, getString(R.string.google_signin_success), Toast.LENGTH_SHORT).show();
                    navigateToMain();
                }
            } else {
                Toast.makeText(this, getString(R.string.google_auth_error, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserData(FirebaseUser user) {
        String id = user.getUid();
        PersonalData personalData = new PersonalData(
                id,
                user.getDisplayName() != null ? user.getDisplayName() : "",
                "",
                user.getEmail() != null ? user.getEmail() : "",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "USD"
        );

        personalDataRepository.addOrUpdatePersonalData(personalData);
    }

    private void navigateToMain() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void resetPassword() {
        String email = loginEmail.getText().toString();
        if (email.isEmpty()) {
            loginEmail.setError(getString(R.string.email_empty));
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError(getString(R.string.email_invalid));
        } else {
            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> Toast.makeText(LoginActivity.this, getString(R.string.reset_link_sent), Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, getString(R.string.error, e.getMessage()), Toast.LENGTH_SHORT).show());
        }
    }
}