package com.mgke.da.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Category;
import com.mgke.da.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SimpleCategoryAdapter extends RecyclerView.Adapter<SimpleCategoryAdapter.CategoryViewHolder> {

    private List<Category> categories;
    private Context context;
    private String selectedCategory;
    private int selectedCategoryImage;
    private int selectedCategoryColor;
    private CategoryRepository categoryRepository;
    private boolean isSelectionEnabled;


    public SimpleCategoryAdapter(Context context, List<Category> categories, CategoryRepository categoryRepository, boolean isSelectionEnabled) {
        this.context = context;
        // Если categories == null, инициализируем пустым списком
        this.categories = categories != null ? categories : new ArrayList<>();
        this.categoryRepository = categoryRepository;
        this.isSelectionEnabled = isSelectionEnabled;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sample_category_item, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        String categoryName = category.getNameLan(Locale.getDefault().getLanguage());
        holder.categoryText.setText(categoryName);

        // Получаем имя изображения из строки
        String categoryImageName = category.categoryImage;  // Строка с именем изображения

        // Получаем идентификатор ресурса изображения
        int imageResourceId = context.getResources().getIdentifier(categoryImageName, "drawable", context.getPackageName());

        // Устанавливаем изображение, если ресурс найден
        if (imageResourceId != 0) {
            holder.categoryIcon.setImageResource(imageResourceId);
        } else {
            // Если изображения нет, устанавливаем изображение по умолчанию
            holder.categoryIcon.setImageResource(R.drawable.ic_other);
        }

        // Устанавливаем цвет фона для иконки
        holder.categoryIcon.setBackgroundTintList(ColorStateList.valueOf(category.categoryColor));

        if (selectedCategory != null && selectedCategory.equals(categoryName)) {
            holder.itemView.setScaleX(1.1f);
            holder.itemView.setScaleY(1.1f);
            selectedCategoryImage = imageResourceId;  // Обновляем выбранное изображение
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
    }


    private void showDeleteConfirmationDialog(Category category, int position) {
        String categoryName = categoryRepository.getCategoryName(category);
        String currentLanguage = Locale.getDefault().getLanguage();

        new AlertDialog.Builder(context)
                .setTitle(R.string.confirmation_title)
                .setMessage(R.string.confirmation_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    categoryRepository.removeCategory(category, currentLanguage);
                    categories.remove(position);
                    notifyItemRemoved(position);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public int getItemCount() {
        return categories.size();
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



    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories != null ? newCategories : new ArrayList<>();
        notifyDataSetChanged();
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String categoryName) {
        this.selectedCategory = categoryName;
        notifyDataSetChanged();  // Перерисовываем элементы, чтобы отображалась выбранная категория
    }

    public int getSelectedCategoryImage() {
        return selectedCategoryImage;
    }

    public int getSelectedCategoryColor() {
        return selectedCategoryColor;
    }

}