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
import com.mgke.da.repository.CategoryRepository;

public class IncomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private CategoryRepository categoryRepository;
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String userId = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d("IncomeFragment", "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_income, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCategories);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));

        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());

        if (currentUser != null) {
            userId = currentUser.getUid(); // Получаем уникальный идентификатор пользователя
        } else {
            Log.d("YourTag", "User is not logged in");
        }

        loadCategories(); // Загружаем категории

        return view;
    }

    private void loadCategories() {
        categoryRepository.getAllCategory(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryAdapter(getContext(), categories, categoryRepository);
                    recyclerView.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
            } else {
                Log.d("IncomeFragment", "No categories found. Creating default categories.");
                categoryRepository.createDefaultCategories(userId); // Создаем базовые категории
                loadCategories(); // Затем снова загружаем категории
            }
        }).exceptionally(e -> {
            Log.e("IncomeFragment", "Error loading categories", e);
            return null;
        });
    }
}