package com.mgke.da.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
    private PersonalDataRepository personalDataRepository;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Проверяем, если пользователь уже аутентифицирован
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Если пользователь уже вошел, переходим в MainActivity
            navigateToMain();
            return;  // Завершаем работу текущей Activity, чтобы не показывать LoginActivity
        }

        setContentView(R.layout.activity_login);
        initializeViews();
        initializeGoogleSignIn();
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
        // Инициализация Firestore
        firestore = FirebaseFirestore.getInstance(); // Добавьте эту строку
        personalDataRepository = new PersonalDataRepository(firestore);
        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
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
            showLoadingDialog();

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            if (user.isEmailVerified()) {
                                navigateToMain();
                            } else {
                                Toast.makeText(LoginActivity.this, getString(R.string.verify_email), Toast.LENGTH_LONG).show();
                                auth.signOut();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this, getString(R.string.login_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                        hideLoadingDialog();
                    });
        }
    }

    private void showLoadingDialog() {
        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.setMessage(getString(R.string.load));
        progressDialog.setCancelable(false);  // Prevent canceling the dialog
        progressDialog.setIndeterminate(true); // Set circular progress style
        progressDialog.show();

        // Disable login button and google sign-in button to prevent multiple clicks
        findViewById(R.id.login_button).setEnabled(false);
        googleSignInButton.setEnabled(false);
    }
    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        // Re-enable the buttons
        findViewById(R.id.login_button).setEnabled(true);
        googleSignInButton.setEnabled(true);
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            loginEmail.setError(getString(R.string.email_empty));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError(getString(R.string.email_invalid));
            return false;
        }
        if (password.isEmpty()) {
            loginPassword.setError(getString(R.string.password_empty)); // Добавьте ошибку для пустого пароля
            return false;
        }
        return true;
    }

    private void signInWithGoogle() {
        showLoadingDialog();
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
                firebaseAuthWithGoogle(account.getIdToken());
            }
        } catch (ApiException e) {
            Toast.makeText(this, getString(R.string.login_error, e.getMessage()), Toast.LENGTH_SHORT).show();
            hideLoadingDialog();  // Hide the progress dialog on error
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

                                // Проверка, есть ли у пользователя пароль
                                String password = null;
                                if (personalData != null && personalData.getPassword() != null) {
                                    password = personalData.getPassword();  // Если пароль есть, сохраняем его
                                }

                                // Устанавливаем isAdmin на основе email
                                boolean isAdmin = email.equals("markinakarina1122@gmail.com");

                                // Создаем новый объект PersonalData
                                String id = user.getUid();
                                PersonalData newPersonalData = new PersonalData(
                                        id,
                                        user.getDisplayName() != null ? user.getDisplayName() : "",
                                        password != null ? password : "",  // Если пароль существует, сохраняем его
                                        email,
                                        null,  // First Name
                                        null,  // Last Name
                                        null,  // Gender
                                        null,  // Birth Date
                                        null,  // Country
                                        null,  // Profession
                                        null,  // Notes
                                        null,
                                        "USD",
                                        isAdmin
                                );

                                personalDataRepository.addOrUpdatePersonalData(newPersonalData);

                                Toast.makeText(LoginActivity.this, getString(R.string.google_sign_in_success), Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
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

    private void navigateToMain() {
        Log.d("LoginActivity", "Переход в MainActivity");
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
