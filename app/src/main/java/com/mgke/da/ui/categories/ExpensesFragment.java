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

public class ExpensesFragment extends Fragment {
    private RecyclerView recyclerView;
    private CategoryAdapter categoryAdapter;
    private CategoryRepository categoryRepository;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private String userId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCategories);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
        }
        loadCategories();
        return view;
    }

    private void loadCategories() {
        categoryRepository.getAllExpenseCategories(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryAdapter(this, categories, categoryRepository, true, "ru"); // Передаем язык
                    recyclerView.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
            } else {
                categoryRepository.createDefaultExpenseCategories(userId);
                loadCategories();
            }
        }).exceptionally(e -> {
            return null;
        });
    }
}
