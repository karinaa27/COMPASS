package com.mgke.da.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.mgke.da.R;
import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.PersonalDataRepository;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private EditText loginEmail, loginPassword;
    private Button googleSignInButton;
    private TextView signupRedirectText, forgotPasswordText;
    private PersonalDataRepository personalDataRepository;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseFirestore firestore;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String currentLocaleCode = getResources().getConfiguration().locale.getLanguage();
        Log.d("LoginActivity", "Текущий языковой код LA: " + currentLocaleCode);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish(); // Закрываем текущую активность, чтобы не возвращаться назад
            return; // Прекращаем выполнение метода
        }
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                });

        SharedPreferences sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        boolean nightMode = sharedPreferences.getBoolean("nightMode", false);
        AppCompatDelegate.setDefaultNightMode(nightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

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
        firestore = FirebaseFirestore.getInstance();
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
                            user.reload().addOnCompleteListener(task -> {
                                if (task.isSuccessful() && user.isEmailVerified()) {
                                    // Проверяем, если у пользователя пустая валюта, устанавливаем по умолчанию
                                    checkUserInFirestore(email).addOnCompleteListener(checkTask -> {
                                        if (checkTask.isSuccessful()) {
                                            PersonalData personalData = checkTask.getResult();
                                            if (personalData != null) {
                                                // Если у пользователя нет валюты, устанавливаем валюту по умолчанию
                                                if (personalData.currency == null || personalData.currency.isEmpty()) {
                                                    personalData.currency = "USD"; // или любая другая валюта по умолчанию
                                                    personalDataRepository.addOrUpdatePersonalData(personalData); // обновляем данные в базе
                                                }
                                                navigateToMain();
                                            } else {
                                            }
                                        } else {
                                            // Обработка ошибки при проверке пользователя
                                            Toast.makeText(LoginActivity.this, getString(R.string.error, checkTask.getException().getMessage()), Toast.LENGTH_SHORT).show();
                                        }
                                        hideLoadingDialog();
                                    });
                                } else {
                                    Toast.makeText(LoginActivity.this, getString(R.string.verify_email), Toast.LENGTH_LONG).show();
                                    hideLoadingDialog();
                                    auth.signOut();
                                }
                            });
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
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
        findViewById(R.id.login_button).setEnabled(false);
        googleSignInButton.setEnabled(false);
    }
    private void hideLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
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
            loginPassword.setError(getString(R.string.password_empty));
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
            Toast.makeText(this, getString(R.string.login_error), Toast.LENGTH_SHORT).show();
            hideLoadingDialog();
        }
    }

    private Task<PersonalData> checkUserInFirestore(String email) {
        // Получаем ссылку на коллекцию "PersonalData" в Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Выполняем запрос для поиска пользователя с указанным email
        CollectionReference usersRef = db.collection("personalData");

        // Запрос для нахождения документа с таким email
        Query query = usersRef.whereEqualTo("email", email);

        // Выполняем запрос и возвращаем Task
        return query.get().continueWithTask(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Если нашли хотя бы один документ, создаем объект PersonalData
                    DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                    PersonalData personalData = document.toObject(PersonalData.class);
                    return Tasks.forResult(personalData);
                } else {
                    // Если документ не найден, возвращаем null
                    return Tasks.forResult(null);
                }
            } else {
                // В случае ошибки выполнения запроса
                throw task.getException();
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = auth.getCurrentUser();
                if (user != null) {
                    String email = user.getEmail();
                    if (email != null) {
                        // Проверяем, есть ли пользователь в Firestore
                        checkUserInFirestore(email).addOnCompleteListener(checkTask -> {
                            if (checkTask.isSuccessful()) {
                                PersonalData personalData = checkTask.getResult();

                                if (personalData != null) {
                                    // Если пользователь уже существует, ничего не меняем, просто переходим в главный экран
                                    Toast.makeText(LoginActivity.this, getString(R.string.google_sign_in_success), Toast.LENGTH_SHORT).show();
                                } else {
                                    // Если пользователя нет в базе, создаем нового
                                    String id = user.getUid();
                                    String password = null; // Пароль не хранится для пользователей Google
                                    boolean isAdmin = email.equals("markinakarina1122@gmail.com");

                                    PersonalData newPersonalData = new PersonalData(
                                            id,
                                            user.getDisplayName() != null ? user.getDisplayName() : "",
                                            password != null ? password : "",
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
                                }

                                // Переход в главный экран
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                String currentLocaleCode = getResources().getConfiguration().locale.getLanguage();
                                Log.d("LoginActivity", "Текущий языковой код LA: " + currentLocaleCode);
                                finish();
                            } else {
                                // Обработка ошибок при проверке данных в Firestore
                                Toast.makeText(LoginActivity.this, "Ошибка при проверке данных в базе.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } else {
                Toast.makeText(this, getString(R.string.auth_error, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMain() {
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
            showLoadingDialog();
            // Проверка наличия email в базе данных Firestore
            checkUserInFirestore(email).addOnCompleteListener(task -> {
                hideLoadingDialog();
                if (task.isSuccessful()) {
                    PersonalData personalData = task.getResult();
                    if (personalData != null) {
                        // Если email найден в базе данных, отправляем письмо для сброса пароля
                        auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(unused -> Toast.makeText(LoginActivity.this, getString(R.string.reset_link_sent), Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, getString(R.string.error), Toast.LENGTH_SHORT).show());

                    } else {
                        // Если email не найден в базе данных
                        Toast.makeText(LoginActivity.this, getString(R.string.email_not_found), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Обработка ошибки при выполнении запроса
                    Toast.makeText(LoginActivity.this, getString(R.string.error, task.getException().getMessage()), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

}
