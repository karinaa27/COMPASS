package com.mgke.da;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private TextView signupRedirectText, forgotPasswordText;
    public static final String PREFS_NAME = "UserPrefs";
    public static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Проверка состояния входа
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);
        Log.d("LoginActivity", "isLoggedIn: " + isLoggedIn); // Лог состояния входа

        if (isLoggedIn) {
            // Если пользователь уже вошел, перенаправьте на главный экран
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        auth = FirebaseAuth.getInstance();
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString();
            String pass = loginPassword.getText().toString();

            if (!email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (!pass.isEmpty()) {
                    auth.signInWithEmailAndPassword(email, pass)
                            .addOnSuccessListener(authResult -> {
                                FirebaseUser user = auth.getCurrentUser();
                                if (user != null) {
                                    Log.d("LoginActivity", "User ID: " + user.getUid());
                                    if (user.isEmailVerified()) {
                                        // Сохраните состояние входа
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putBoolean(KEY_IS_LOGGED_IN, true);
                                        editor.apply();
                                        Log.d("LoginActivity", "User logged in and state saved.");

                                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Please verify your email before logging in", Toast.LENGTH_LONG).show();
                                        auth.signOut(); // Выйти из аккаунта, если почта не подтверждена
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("LoginActivity", "Login Failed: " + e.getMessage());
                                Toast.makeText(LoginActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    loginPassword.setError("Password cannot be empty");
                }
            } else if (email.isEmpty()) {
                loginEmail.setError("Email cannot be empty");
            } else {
                loginEmail.setError("Please enter a valid email");
            }
        });

        signupRedirectText.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        forgotPasswordText.setOnClickListener(v -> {
            String email = loginEmail.getText().toString();
            if (email.isEmpty()) {
                loginEmail.setError("Please enter your email");
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                loginEmail.setError("Please enter a valid email");
            } else {
                auth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(unused -> Toast.makeText(LoginActivity.this, "Reset link sent to your email", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
}