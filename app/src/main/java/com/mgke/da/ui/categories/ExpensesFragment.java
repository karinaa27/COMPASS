package com.mgke.da.ui.categories;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.CategoryAdapter;
import com.mgke.da.repository.CategoryRepository;
import java.util.Locale;

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

        loadCategories();

        return view;
    }

    public void loadCategories() {
        String currentLanguage = Locale.getDefault().getLanguage();
        if (categoryAdapter != null && categoryAdapter.getItemCount() > 0) {
            return;
        }

        categoryRepository.getAllExpenseCategories(userId).thenAccept(categories -> {
            if (categories != null && !categories.isEmpty()) {
                if (categoryAdapter == null) {
                    categoryAdapter = new CategoryAdapter(this, categories, categoryRepository, true, currentLanguage);
                    recyclerView.setAdapter(categoryAdapter);
                } else {
                    categoryAdapter.updateCategories(categories);
                }
            } else {
                categoryRepository.createDefaultCategories(userId, "expense").thenRun(() -> {
                    loadCategories();
                });
                Log.w("ExpensesFragment", "No categories found, default categories created.");
            }
        }).exceptionally(e -> {
            Log.e("ExpensesFragment", "Error loading categories", e);
            return null;
        });
    }

    private void navigateToAddCategoryFragment() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
        Bundle bundle = new Bundle();
        bundle.putString("category_type", "expense"); // Указываем, что тип категории - расход
        navController.navigate(R.id.action_expensesFragment_to_addCategoryFragment, bundle);
    }

}


