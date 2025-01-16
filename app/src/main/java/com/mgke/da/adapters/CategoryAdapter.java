package com.mgke.da.adapters;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.mgke.da.R;
import com.mgke.da.models.Category;
import com.mgke.da.repository.CategoryRepository;
import com.mgke.da.ui.categories.ExpensesFragment;

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

    // Добавляем контекст в конструктор адаптера
    public CategoryAdapter(Fragment fragment, List<Category> categories, CategoryRepository categoryRepository, boolean isSelectionEnabled, String language) {
        this.fragment = fragment;
        this.context = fragment.getContext(); // Используем контекст фрагмента
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

            // Получаем имя изображения из строки и загружаем его
            String categoryImageName = category.categoryImage;  // Теперь это строка с именем ресурса

            // Получаем идентификатор ресурса по имени
            int imageResourceId = context.getResources().getIdentifier(categoryImageName, "drawable", context.getPackageName());

            // Устанавливаем изображение, если ресурс найден
            if (imageResourceId != 0) {
                ((CategoryViewHolder) holder).categoryIcon.setImageResource(imageResourceId);
            } else {
                // Если изображения нет, устанавливаем изображение по умолчанию
                ((CategoryViewHolder) holder).categoryIcon.setImageResource(R.drawable.ic_other);
            }

            // Устанавливаем цвет фона для иконки
            if (category.categoryColor != 0) {
                ((CategoryViewHolder) holder).categoryIcon.setBackgroundTintList(ColorStateList.valueOf(category.categoryColor));
            } else {
                // Если нет цвета, устанавливаем цвет по умолчанию
                ((CategoryViewHolder) holder).categoryIcon.setBackgroundTintList(ColorStateList.valueOf(context.getResources().getColor(R.color.lavander)));
            }

            // Логика выбора категории
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
                showDeleteConfirmationDialog(category, position);
                return true;
            });
        } else if (holder instanceof AddCategoryViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(v);
                Bundle bundle = new Bundle();

                // Проверяем, какой тип категории должен быть передан
                if (fragment instanceof ExpensesFragment) {
                    bundle.putString("category_type", "expense");
                } else {
                    bundle.putString("category_type", "income");
                }

                navController.navigate(R.id.addCategoryFragment, bundle);
            });
        }
    }


    private void showDeleteConfirmationDialog(Category category, int position) {
        String categoryName = categoryRepository.getCategoryName(category);
        String currentLanguage = Locale.getDefault().getLanguage();

        if (context != null) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.confirmation_title)
                    .setMessage(String.format(context.getString(R.string.confirmation_message)))
                    .setPositiveButton(R.string.confirm, (dialog, which) -> {
                        // Показываем ProgressDialog перед удалением
                        ProgressDialog progressDialog = new ProgressDialog(context);
                        progressDialog.setMessage(context.getString(R.string.deleting_category));
                        progressDialog.setCancelable(false); // Диалог не отменяется пользователем
                        progressDialog.show();

                        // Удаляем категорию
                        categoryRepository.removeCategory(category, currentLanguage);

                        // Ждем завершения удаления
                        categoryRepository.deleteCategory(category.id)
                                .addOnSuccessListener(aVoid -> {
                                    // Скрываем ProgressDialog после успешного удаления
                                    progressDialog.dismiss();
                                    categories.remove(position);
                                    notifyItemRemoved(position);
                                })
                                .addOnFailureListener(e -> {
                                    // Скрываем ProgressDialog в случае ошибки
                                    progressDialog.dismiss();
                                    // Показываем ошибку
                                    Toast.makeText(context, context.getString(R.string.delete_category_error), Toast.LENGTH_SHORT).show();
                                });

                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
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

    // Добавляем метод для получения списка категорий
    public List<Category> getCategories() {
        return categories;
    }
}
