package com.mgke.da.ui.categories;

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
    private GridLayout iconGrid;
    private int selectedColor;
    private int selectedImageResId;
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
            navController.navigate(R.id.navigation_settings_category);
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
                    selectedImageResId = getResources().getIdentifier(tag, "drawable", getActivity().getPackageName());
                    highlightSelectedIcon(icon);
                }
            });
        }
    }

    private void addCategory(String categoryName, boolean isIncome) {
        if (categoryName.isEmpty() || selectedImageResId == 0) {
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            return;
        }

        String type = isIncome ? "income" : "expense";

        categoryRepository.addCategory(categoryName, type, selectedImageResId, selectedColor, userId)
                .thenAccept(aVoid -> {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_settings_category);
                })
                .exceptionally(e -> {
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
        int backgroundResource = isDarkTheme() ? R.drawable.category_bg_night : R.drawable.category_bg;

        for (int i = 0; i < iconGrid.getChildCount(); i++) {
            ImageView icon = (ImageView) iconGrid.getChildAt(i);
            icon.setScaleX(1f);
            icon.setScaleY(1f);
            icon.setBackgroundResource(backgroundResource);
        }
        selectedIcon.setBackgroundResource(R.drawable.category_selected_icon);
        selectedIcon.setScaleX(1.2f);
        selectedIcon.setScaleY(1.2f);
    }
}
