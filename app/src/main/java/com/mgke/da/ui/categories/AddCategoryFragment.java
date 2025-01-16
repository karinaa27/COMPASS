package com.mgke.da.ui.categories;

import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.google.firebase.auth.FirebaseAuth;
import com.mgke.da.R;
import com.mgke.da.repository.CategoryRepository;

public class AddCategoryFragment extends Fragment {
    private EditText categoryNameEditText;
    private RadioGroup incomeExpenseRadioGroup;
    private Button addCategoryButton;
    private ImageView backButton;
    private CategoryRepository categoryRepository;
    private String selectedImageName;
    private GridLayout iconGrid;
    private int selectedColor;
    private ColorPickerView colorPicker;

    public AddCategoryFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_category, container, false);

        iconGrid = view.findViewById(R.id.iconGrid);
        categoryNameEditText = view.findViewById(R.id.categoryNameEditText);
        incomeExpenseRadioGroup = view.findViewById(R.id.incomeExpenseRadioGroup);
        addCategoryButton = view.findViewById(R.id.addCategoryButton);
        backButton = view.findViewById(R.id.backButton);
        colorPicker = view.findViewById(R.id.colorPickerView);
        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());

        View selectedColorView = view.findViewById(R.id.selectedColorView);

        colorPicker.setColorListener(new ColorListener() {
            @Override
            public void onColorSelected(int color, boolean fromUser) {
                selectedColor = color;
                selectedColorView.setBackgroundColor(selectedColor);
            }
        });

        setupIconSelection();
        updateIconBackgrounds();

        backButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.popBackStack();
        });

        if (getArguments() != null) {
            String categoryType = getArguments().getString("category_type", "");
            if ("income".equals(categoryType)) {
                incomeExpenseRadioGroup.check(R.id.radioIncome);
            } else if ("expense".equals(categoryType)) {
                incomeExpenseRadioGroup.check(R.id.radioExpense);
            }
        }

        addCategoryButton.setOnClickListener(v -> {
            String categoryName = categoryNameEditText.getText().toString();
            boolean isIncome = incomeExpenseRadioGroup.getCheckedRadioButtonId() == R.id.radioIncome;
            addCategory(categoryName, isIncome);
        });

        return view;
    }

    private void setupIconSelection() {
        for (int i = 0; i < iconGrid.getChildCount(); i++) {
            ImageView icon = (ImageView) iconGrid.getChildAt(i);
            icon.setOnClickListener(v -> {
                String tag = icon.getTag() != null ? icon.getTag().toString() : null;
                if (tag != null) {
                    selectedImageName = tag;
                    highlightSelectedIcon(icon);
                }
            });
        }
    }

    private void addCategory(String categoryName, boolean isIncome) {
        // Проверка, что поле для имени категории не пустое
        if (categoryName.isEmpty()) {
            Toast.makeText(getContext(), R.string.enter_category_name, Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверка, что иконка была выбрана
        if (selectedImageName == null) {
            Toast.makeText(getContext(), R.string.select_category_icon, Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверка, что тип категории выбран (должен быть "income" или "expense")
        if (incomeExpenseRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), R.string.select_category_type, Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            return;
        }

        String type = isIncome ? "income" : "expense";

        // Создаем и показываем прогресс-диалог
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(getString(R.string.save)); // Строка для прогресса
        progressDialog.setCancelable(false);
        progressDialog.show();

        categoryRepository.addCategory(categoryName, type, selectedImageName, selectedColor, userId)
                .thenAccept(aVoid -> {
                    // После успешного добавления категории, вернемся на предыдущий экран
                    progressDialog.dismiss(); // Скрытие прогресс-диалога после успешного сохранения
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.popBackStack();
                })
                .exceptionally(throwable -> {
                    // Обработка ошибок
                    progressDialog.dismiss(); // Скрытие прогресс-диалога в случае ошибки
                    Toast.makeText(getContext(), R.string.error_adding_category, Toast.LENGTH_SHORT).show();
                    return null;
                });
    }


    private void updateIconBackgrounds() {
        if (iconGrid == null) return;

        int backgroundResource = isDarkTheme() ? R.drawable.category_bg_night : R.drawable.category_bg;

        for (int i = 0; i < iconGrid.getChildCount(); i++) {
            ImageView icon = (ImageView) iconGrid.getChildAt(i);
            icon.setBackgroundResource(backgroundResource);
        }
    }

    private boolean isDarkTheme() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void highlightSelectedIcon(ImageView selectedIcon) {
        // Фон для обычных иконок (не выделенных)
        int backgroundResource = isDarkTheme() ? R.drawable.category_bg_night : R.drawable.category_bg;

        // Проходим по всем иконкам и сбрасываем их масштаб и фон
        for (int i = 0; i < iconGrid.getChildCount(); i++) {
            ImageView icon = (ImageView) iconGrid.getChildAt(i);

            // Сбрасываем масштаб для всех иконок
            icon.setScaleX(1f);
            icon.setScaleY(1f);

            // Возвращаем обычный фон
            icon.setBackgroundResource(backgroundResource);
        }

        // Применяем новый фон и увеличиваем выбранную иконку
        selectedIcon.setBackgroundResource(R.drawable.category_selected_icon);
        selectedIcon.setScaleX(1.1f);  // Увеличиваем только выбранную иконку
        selectedIcon.setScaleY(1.1f);  // Увеличиваем только выбранную иконку
    }

}
