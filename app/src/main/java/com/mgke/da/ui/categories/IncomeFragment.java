package com.mgke.da.ui.categories;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.CategoryAdapter;
import com.mgke.da.models.Category;
import com.mgke.da.repository.CategoryRepository;

import java.util.List;

public class IncomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private CategoryRepository categoryRepository;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private String userId = null;
    private boolean isCategoriesLoaded = false; // Флаг, чтобы избежать повторной загрузки
    private List<Category> cachedCategories = null; // Для хранения загруженных категорий

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCategories);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));

        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());

        if (currentUser != null) {
            userId = currentUser.getUid();
        }

        loadCategories();  // Загружаем категории при первом создании фрагмента

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCategories();
            Log.d("CategoryFragment", "onResume called income");
    }

    @Override
    public void onPause() {
        super.onPause();
        // Сохраняем загруженные категории в кэш
        if (categoryAdapter != null) {
                cachedCategories = categoryAdapter.getCategories(); // Предполагается, что в адаптере есть метод для получения категорий
        }
        Log.d("CategoryFragment", "onPause called");
    }

    public void loadCategories() {
        categoryRepository.getAllCategory(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryAdapter(this, categories, categoryRepository, true, "ru");
                    recyclerView.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
                isCategoriesLoaded = true; // Устанавливаем флаг, что категории загружены
                cachedCategories = categories; // Сохраняем категории в кэш
            } else {
                categoryRepository.createDefaultCategories(userId);
                loadCategories();  // Повторная попытка загрузки, если категории пусты
            }
        }).exceptionally(e -> {
            Log.e("IncomeFragment", "Error loading categories", e);
            return null;
        });
    }
}
