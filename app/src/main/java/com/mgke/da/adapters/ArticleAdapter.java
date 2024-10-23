package com.mgke.da.adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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
        holder.articleTitle.setText(article.name);
        holder.articleDescription.setText(article.description);

        // Загружаем изображение с использованием Glide
        Glide.with(context)
                .load(article.image)
                .placeholder(R.drawable.account_fon1)
                .error(R.drawable.account_fon2)
                .into(holder.articleImage);

        // Обработчик клика для перехода к фрагменту с деталями статьи
        holder.itemView.setOnClickListener(v -> {
            // Создание Bundle для передачи данных
            Bundle bundle = new Bundle();
            bundle.putString("articleId", article.id);

            // Навигация к ArticleFragment с передачей данных
            Navigation.findNavController(v).navigate(R.id.fragment_article, bundle);
        });
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
