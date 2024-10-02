package com.mgke.da.ui.categories;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.models.Category;
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
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_category, container, false);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        categoryRepository = new CategoryRepository(db);

        categoryNameEditText = view.findViewById(R.id.categoryNameEditText);
        incomeExpenseRadioGroup = view.findViewById(R.id.incomeExpenseRadioGroup);
        addCategoryButton = view.findViewById(R.id.addCategoryButton);
        backButton = view.findViewById(R.id.backButton);
        iconGrid = view.findViewById(R.id.iconGrid);
        colorPicker = view.findViewById(R.id.colorPickerView);

        View selectedColorView = view.findViewById(R.id.selectedColorView);

        colorPicker.setColorListener(new ColorListener() {
            @Override
            public void onColorSelected(int color, boolean fromUser) {
                selectedColor = color; // Сохраняем выбранный цвет
                selectedColorView.setBackgroundColor(selectedColor);
            }
        });

        backButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_settings_category); // Замените на ID вашего фрагмента
        });

        setupIconSelection();

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
                Log.d("AddCategoryFragment", "Selected Icon Tag: " + tag);
                if (tag != null) {
                    selectedImageResId = getResources().getIdentifier(tag, "drawable", getActivity().getPackageName());
                    Log.d("AddCategoryFragment", "Selected Image Res ID: " + selectedImageResId);
                    highlightSelectedIcon(icon);
                } else {
                    Log.d("AddCategoryFragment", "Тег иконки не установлен");
                }
            });
        }
    }

    private void addCategory(String categoryName, boolean isIncome) {
        Log.d("AddCategoryFragment", "Category Name: " + categoryName);
        Log.d("AddCategoryFragment", "Selected Image Res ID: " + selectedImageResId);
        Log.d("AddCategoryFragment", "Selected Color: " + selectedColor);

        if (categoryName.isEmpty() || selectedImageResId == 0) {
            Log.d("AddCategoryFragment", "Ошибка: Название категории или изображение не выбрано.");
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String type = isIncome ? "income" : "expense";

        Category category = new Category();
        category.categoryName = categoryName;
        category.type = type;
        category.categoryImage = selectedImageResId;
        category.categoryColor = selectedColor;
        category.userId = userId;

        categoryRepository.addCategory(category)
                .addOnSuccessListener(aVoid -> {
                    Log.d("AddCategoryFragment", "Категория успешно добавлена: " + categoryName);

                    // Переход к CategoryFragment
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_settings_category); // Замените на ID вашего фрагмента
                })
                .addOnFailureListener(e -> {
                    Log.e("AddCategoryFragment", "Ошибка при добавлении категории", e);
                });
    }

    private void highlightSelectedIcon(ImageView selectedIcon) {
        for (int i = 0; i < iconGrid.getChildCount(); i++) {
            ImageView icon = (ImageView) iconGrid.getChildAt(i);
            icon.setBackgroundResource(R.drawable.category_bg);
        }

        selectedIcon.setBackgroundResource(R.drawable.category_selected_icon);
    }
}