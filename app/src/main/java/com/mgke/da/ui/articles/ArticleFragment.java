package com.mgke.da.ui.articles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.models.Article;
import com.mgke.da.repository.ArticleRepository;

public class ArticleFragment extends Fragment {

    private String articleId;
    private ArticleRepository articleRepository;

    private ImageView articleImage;
    private TextView articleTitle, articleDate, articleContent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            articleId = getArguments().getString("articleId");
        }

        // Инициализация репозитория
        articleRepository = new ArticleRepository(FirebaseFirestore.getInstance());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_article, container, false);

        // Инициализация элементов интерфейса
        articleImage = root.findViewById(R.id.articleImage);
        articleTitle = root.findViewById(R.id.articleTitle);
        articleDate = root.findViewById(R.id.articleDate);
        articleContent = root.findViewById(R.id.articleContent);

        // Загрузка статьи и отображение данных
        loadArticleDetails();

        return root;
    }

    private void loadArticleDetails() {
        articleRepository.getAllArticles().thenAccept(articles -> {
            for (Article article : articles) {
                if (article.id.equals(articleId)) {
                    // Установка данных в элементы интерфейса
                    articleTitle.setText(article.name);
                    articleDate.setText(article.getFormattedTimestamp());
                    articleContent.setText(article.text);

                    // Загрузка изображения
                    Glide.with(requireContext())
                            .load(article.image)
                            .placeholder(R.drawable.account_fon1)
                            .error(R.drawable.account_fon2)
                            .into(articleImage);
                    break;
                }
            }
        });
    }
}
