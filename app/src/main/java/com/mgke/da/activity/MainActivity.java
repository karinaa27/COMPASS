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

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, SignUpActivity.class));
            finish();
        }
        setLocale();

        SharedPreferences sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        boolean nightMode = sharedPreferences.getBoolean("nightMode", false);
        AppCompatDelegate.setDefaultNightMode(nightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

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

    private void setLocale() {
        SharedPreferences sharedPreferences = getSharedPreferences("MODE", Context.MODE_PRIVATE);
        String languageCode = sharedPreferences.getString("selectedLanguage", "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        Log.d("Locale", "Locale set to: " + Locale.getDefault().getDisplayLanguage());
    }
}