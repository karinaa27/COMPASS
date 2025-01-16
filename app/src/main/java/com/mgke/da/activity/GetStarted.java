package com.mgke.da.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mgke.da.R;

public class GetStarted extends AppCompatActivity {
    Button startButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true);

        // Получаем текущий языковой код
        String currentLanguageCode = getResources().getConfiguration().locale.getLanguage();
        Log.d("GetStarted", "Текущий языковой код: " + currentLanguageCode);  // Логируем текущий языковой код

        if (!isFirstLaunch) {
            // Если это не первый запуск, сразу переходим в MainActivity или LoginActivity
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            Intent intent;
            if (currentUser != null) {
                // Если пользователь авторизован, переходим на MainActivity
                intent = new Intent(GetStarted.this, MainActivity.class);
            } else {
                // Если пользователь не авторизован, переходим на LoginActivity
                intent = new Intent(GetStarted.this, LoginActivity.class);
            }
            startActivity(intent);
            finish(); // Завершаем текущую активность, чтобы не было возвращения на GetStarted
            return; // Прерываем выполнение метода onCreate
        }

        setContentView(R.layout.activity_get_started);
        startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Сохраняем информацию о первом запуске
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isFirstLaunch", false); // Устанавливаем, что это не первый запуск
                editor.apply();

                // Проверяем текущего пользователя
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    // Если пользователь авторизован, принудительно выходим
                    FirebaseAuth.getInstance().signOut();
                }

                // Переходим на LoginActivity после выхода
                Intent loginIntent = new Intent(GetStarted.this, LoginActivity.class);
                startActivity(loginIntent);
                finish(); // Завершаем текущую активность
            }
        });
    }
}
