package com.mgke.da.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.mgke.da.R;
import com.mgke.da.databinding.FragmentSettingsBinding;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        setupLanguageSpinner();
        setupNightModeSwitch();

        setupCategoriesSettingsClick();
        return view;
    }

    private void setupCategoriesSettingsClick() {
        RelativeLayout categoriesSettings = binding.CategoriesSettings; // Получаем ссылку на CategoriesSettings
        categoriesSettings.setOnClickListener(v -> {
            // Получаем NavController
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            // Переход к новому фрагменту
            navController.navigate(R.id.navigation_settings_category); // Замените на ID вашего фрагмента
        });
    }

    private void setupLanguageSpinner() {
        String[] languages = {"Русский", "English"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.languageSpinner.setAdapter(adapter);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
        String selectedLanguage = sharedPreferences.getString("selectedLanguage", "English");
        int spinnerPosition = selectedLanguage.equals("Русский") ? 0 : 1;
        binding.languageSpinner.setSelection(spinnerPosition);

        binding.languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String languageCode = position == 0 ? "ru" : "en";
                setLocale(languageCode);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("selectedLanguage", position == 0 ? "Русский" : "English");
                editor.apply();
                updateUI();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupNightModeSwitch() {

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
        boolean nightMode = sharedPreferences.getBoolean("nightMode", false);
        AppCompatDelegate.setDefaultNightMode(nightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        binding.switchNightMode.setChecked(nightMode);

        binding.switchNightMode.setOnCheckedChangeListener((buttonView, isChecked) -> {

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("nightMode", isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        if (!languageCode.equals(getCurrentLanguage())) {
            if (languageCode.equals("ru")) {
                requireActivity().recreate();
            }
        }
    }
    private void updateUI() {
        binding.textSettings.setText(getString(R.string.settings));
        binding.textUser.setText(getString(R.string.user_name_settings));
        binding.textUserEmail.setText(getString(R.string.email_settings));
        binding.textNightMode.setText(getString(R.string.night_mode));
        binding.textNotifications.setText(getString(R.string.notifications));
        binding.textLanguage.setText(getString(R.string.language));
        binding.textCategories.setText(getString(R.string.categories_string_settings));

        String selectedLanguage = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE)
                .getString("selectedLanguage", "English");
        int spinnerPosition = selectedLanguage.equals("Русский") ? 0 : 1;
        binding.languageSpinner.setSelection(spinnerPosition);
    }

    private String getCurrentLanguage() {
        return getResources().getConfiguration().locale.getLanguage();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}