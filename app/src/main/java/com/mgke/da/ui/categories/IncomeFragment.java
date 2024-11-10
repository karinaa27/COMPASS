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
import java.util.Locale;

public class IncomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private CategoryRepository categoryRepository;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private String userId = null;
    private boolean isCategoriesLoaded = false; // Флаг, чтобы избежать повторной загрузки

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
        // Можно добавить логику для повторной загрузки категорий, если они не были загружены
        if (!isCategoriesLoaded) {
            loadCategories();
        }
    }

    public void loadCategories() {
        // Получаем текущий язык
        String currentLanguage = Locale.getDefault().getLanguage();

        if (isCategoriesLoaded) {
            return;
        }

        categoryRepository.getAllCategory(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryAdapter(this, categories, categoryRepository, true, currentLanguage);
                    recyclerView.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
                isCategoriesLoaded = true;
            } else {
                categoryRepository.createDefaultCategories(userId, "income").thenRun(() -> {
                    loadCategories();  // Повторная попытка загрузки после создания категорий
                });
            }
        }).exceptionally(e -> {
            Log.e("IncomeFragment", "Error loading categories", e);
            return null;
        });
    }

}


