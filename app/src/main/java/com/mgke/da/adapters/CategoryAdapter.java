package com.mgke.da.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Category;
import com.mgke.da.repository.CategoryRepository;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Category> categories;
    private Context context;

    private CategoryRepository categoryRepository; // Добавляем поле для репозитория

    public CategoryAdapter(Context context, List<Category> categories, CategoryRepository categoryRepository) {
        this.context = context;
        this.categories = categories;
        this.categoryRepository = categoryRepository; // Инициализируем репозиторий
    }

    private static final int VIEW_TYPE_CATEGORY = 0;
    private static final int VIEW_TYPE_ADD_BUTTON = 1;

    public CategoryAdapter(Context context, List<Category> categories) {
        this.context = context;
        this.categories = categories;
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
            ((CategoryViewHolder) holder).categoryText.setText(category.categoryName);
            ((CategoryViewHolder) holder).categoryIcon.setImageResource(category.categoryImage);
            ((CategoryViewHolder) holder).categoryIcon.setBackgroundTintList(ColorStateList.valueOf(category.categoryColor));
            Log.d("CategoryAdapter", "Binding category: " + category.categoryName);

            holder.itemView.setOnLongClickListener(v -> {
                if (!category.categoryName.equals("Другое")) {
                    showDeleteConfirmationDialog(category, position);
                } else {
                    Log.d("CategoryAdapter", "Категорию 'Другое' удалить нельзя.");
                }
                return true;
            });
        } else if (holder instanceof AddCategoryViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                // Получаем NavController и выполняем навигацию
                NavController navController = Navigation.findNavController((Activity) context, R.id.nav_host_fragment_activity_main);
                navController.navigate(R.id.addCategoryFragment); // Переход к фрагменту добавления категории
            });
        }
    }
    private void showDeleteConfirmationDialog(Category category, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Подтверждение удаления")
                .setMessage("Вы действительно хотите удалить категорию " + category.categoryName + "?")
                .setPositiveButton("Да", (dialog, which) -> {
                    categoryRepository.removeCategory(category); // Удаляем через репозиторий
                    categories.remove(position);
                    notifyItemRemoved(position);
                })
                .setNegativeButton("Нет", null)
                .show();
    }

    @Override
    public int getItemViewType(int position) {
        return position == categories.size() ? VIEW_TYPE_ADD_BUTTON : VIEW_TYPE_CATEGORY;
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