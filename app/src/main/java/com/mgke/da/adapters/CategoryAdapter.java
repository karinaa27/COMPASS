package com.mgke.da.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Category;
import com.mgke.da.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Category> categories;
    private Context context;
    private String selectedCategory;
    private int selectedCategoryImage;
    private int selectedCategoryColor;
    private CategoryRepository categoryRepository;
    private boolean isSelectionEnabled;
    private String language;
    private Fragment fragment;

    public CategoryAdapter(Fragment fragment, List<Category> categories, CategoryRepository categoryRepository, boolean isSelectionEnabled, String language) {
        this.fragment = fragment;
        this.categories = categories;
        this.categoryRepository = categoryRepository;
        this.isSelectionEnabled = isSelectionEnabled;
        this.language = language;
    }

    private static final int VIEW_TYPE_CATEGORY = 0;
    private static final int VIEW_TYPE_ADD_BUTTON = 1;

    public CategoryAdapter(ArrayList<Object> objects) {
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CATEGORY) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sample_category_item, parent, false);
            return new CategoryViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_category, parent, false);
            return new AddCategoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CategoryViewHolder) {
            Category category = categories.get(position);
            String categoryName = category.getNameLan(language);
            ((CategoryViewHolder) holder).categoryText.setText(categoryName);
            ((CategoryViewHolder) holder).categoryIcon.setImageResource(category.categoryImage);
            ((CategoryViewHolder) holder).categoryIcon.setBackgroundTintList(ColorStateList.valueOf(category.categoryColor));

            if (selectedCategory != null && selectedCategory.equals(categoryName)) {
                holder.itemView.setScaleX(1.1f);
                holder.itemView.setScaleY(1.1f);
                selectedCategoryImage = category.categoryImage;
                selectedCategoryColor = category.categoryColor;
            } else {
                holder.itemView.setScaleX(1.0f);
                holder.itemView.setScaleY(1.0f);
            }

            holder.itemView.setOnClickListener(v -> {
                if (isSelectionEnabled) {
                    selectedCategory = categoryName;
                    notifyDataSetChanged();
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                if (!categoryName.equals("Другое")) {
                    showDeleteConfirmationDialog(category, position);
                }
                return true;
            });
        } else if (holder instanceof AddCategoryViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(v);
                Bundle bundle = new Bundle();
                bundle.putString("category_type", "income");
                navController.navigate(R.id.addCategoryFragment, bundle);
            });
        }
    }

    private void showDeleteConfirmationDialog(Category category, int position) {
        String categoryName = categoryRepository.getCategoryName(category);
        String currentLanguage = Locale.getDefault().getLanguage();

        new AlertDialog.Builder(context)
                .setTitle(R.string.confirmation_title)
                .setMessage(String.format(context.getString(R.string.confirmation_message), categoryName))
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    categoryRepository.removeCategory(category, currentLanguage);
                    categories.remove(position);
                    notifyItemRemoved(position);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public int getItemViewType(int position) {
        return position < categories.size() ? VIEW_TYPE_CATEGORY : VIEW_TYPE_ADD_BUTTON;
    }

    @Override
    public int getItemCount() {
        return categories.size() + 1;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryText;
        ImageView categoryIcon;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryText = itemView.findViewById(R.id.categoryText);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
        }
    }

    static class AddCategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView addCategoryIcon;

        public AddCategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            addCategoryIcon = itemView.findViewById(R.id.addCategoryIcon);
        }
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }
}