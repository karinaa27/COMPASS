package com.mgke.da.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mgke.da.R;
import com.mgke.da.databinding.ActivityMainBinding;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Получаем SharedPreferences для проверки и сохранения языка и флага первого запуска
        SharedPreferences sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);

        // Проверяем, первый ли это запуск
        boolean isFirstRun = sharedPreferences.getBoolean("isFirstRun", true);

        // Если это первый запуск, устанавливаем русский язык и сохраняем флаг
        if (isFirstRun) {
            setLocale("ru"); // Устанавливаем русский язык по умолчанию
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isFirstRun", false); // Ставим флаг, что это не первый запуск
            editor.putString("selectedLanguage", "ru"); // Сохраняем выбранный язык
            editor.apply();
        } else {
            // Если не первый запуск, просто получаем сохраненный язык
            String languageCode = sharedPreferences.getString("selectedLanguage", "ru");
            setLocale(languageCode); // Устанавливаем сохраненный язык
        }

        // Проверка ночного режима
        boolean nightMode = sharedPreferences.getBoolean("nightMode", false); // Получаем сохраненный режим
        AppCompatDelegate.setDefaultNightMode(nightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO); // Применяем режим

        // Проверяем, авторизован ли пользователь
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Если пользователь не авторизован, перенаправляем в LoginActivity
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish(); // Завершаем текущую активность
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = binding.navView;
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_stats)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        NavigationUI.setupWithNavController(navView, navController);
        navView.setOnNavigationItemSelectedListener(item -> {
            View selectedItem = navView.findViewById(item.getItemId());
            animateSelectedItem(selectedItem);

            if (item.getItemId() == R.id.bottom_home) {
                navController.navigate(R.id.navigation_home);
                return true;
            } else if (item.getItemId() == R.id.bottom_stats) {
                navController.navigate(R.id.navigation_stats);
                return true;
            } else if (item.getItemId() == R.id.bottom_accounts) {
                navController.navigate(R.id.navigation_accounts);
                return true;
            } else if (item.getItemId() == R.id.bottom_articles) {
                navController.navigate(R.id.navigation_articles);
                return true;
            } else if (item.getItemId() == R.id.bottom_settings) {
                navController.navigate(R.id.navigation_settings);
                return true;
            } else {
                return false;
            }
        });
    }


    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        Log.d("Locale", "Locale set to: " + Locale.getDefault().getDisplayLanguage());
    }
    
    private void animateSelectedItem(View item) {
        if (item.getTranslationY() == 0) {
            ObjectAnimator raiseAnimator = ObjectAnimator.ofFloat(item, "translationY", -20f);
            raiseAnimator.setDuration(300);
            raiseAnimator.setRepeatCount(1);
            raiseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            raiseAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    item.setTranslationY(0);
                }
            });
            raiseAnimator.start();
        }
    }
}