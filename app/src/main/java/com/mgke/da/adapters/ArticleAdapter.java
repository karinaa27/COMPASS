package com.mgke.da.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.mgke.da.R;
import com.mgke.da.models.Article;
import java.util.List;
import java.util.Locale;

public class ArticleAdapter extends RecyclerView.Adapter<ArticleAdapter.ArticleViewHolder> {

    private final Context context;
    private final List<Article> articles;

    public ArticleAdapter(Context context, List<Article> articles) {
        this.context = context;
        this.articles = articles;
    }

    @NonNull
    @Override
    public ArticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false);
        return new ArticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
        Article article = articles.get(position);

        // Определяем текущий язык системы
        String currentLanguage = Locale.getDefault().getLanguage();

        // Устанавливаем название и описание в зависимости от языка
        if ("ru".equals(currentLanguage)) {
            holder.articleTitle.setText(article.nameRu);
            holder.articleDescription.setText(article.descriptionRu);
        } else {
            holder.articleTitle.setText(article.nameEn);
            holder.articleDescription.setText(article.descriptionEn);
        }

        // Загрузка изображения с использованием Glide
        Glide.with(context)
                .load(article.image)
                .placeholder(isDarkMode() ? R.drawable.balance_fon_night : R.drawable.balance_fon) // Используем изображение в зависимости от темы
                .error(isDarkMode() ? R.drawable.balance_fon_night : R.drawable.balance_fon) // Используем изображение в зависимости от темы
                .into(holder.articleImage);

        // Обработчик клика для перехода к фрагменту с деталями статьи
        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("articleId", article.id);
            Navigation.findNavController(v).navigate(R.id.fragment_article, bundle);
        });
    }
    private boolean isDarkMode() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    @Override
    public int getItemCount() {
        return articles.size();
    }

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        ImageView articleImage;
        TextView articleTitle;
        TextView articleDescription;

        public ArticleViewHolder(@NonNull View itemView) {
            super(itemView);
            articleImage = itemView.findViewById(R.id.articleImage);
            articleTitle = itemView.findViewById(R.id.articleTitle);
            articleDescription = itemView.findViewById(R.id.articleDescription);
        }
    }
}
