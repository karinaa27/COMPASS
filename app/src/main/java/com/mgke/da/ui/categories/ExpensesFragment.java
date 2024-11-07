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
        }

        loadCategories();  // Загружаем категории при первом создании фрагмента

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
            loadCategories();  // Загружаем категории, если они еще не были загружены
            Log.d("CategoryFragment", "onResume called");
    }

    public void loadCategories() {
        // Проверяем, были ли категории загружены ранее
        if (categoryAdapter != null && categoryAdapter.getItemCount() > 0) {
            return; // Категории уже загружены, не загружаем их снова
        }

        categoryRepository.getAllExpenseCategories(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryAdapter(this, categories, categoryRepository, true, "ru");
                    recyclerView.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
            } else {
                // Создаем категории по умолчанию, если данных нет
                categoryRepository.createDefaultExpenseCategories(userId);
                // Не вызываем loadCategories() снова, а просто выводим сообщение об ошибке
                Log.w("ExpensesFragment", "No categories found, default categories created.");
            }
        }).exceptionally(e -> {
            Log.e("ExpensesFragment", "Error loading categories", e);
            return null;
        });
    }

}

