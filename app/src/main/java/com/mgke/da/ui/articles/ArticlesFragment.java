package com.mgke.da.ui.articles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.ArticleAdapter;
import com.mgke.da.models.Article;
import com.mgke.da.repository.ArticleRepository;
import java.util.ArrayList;
import java.util.List;

public class ArticlesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private List<Article> articles = new ArrayList<>();
    private ArticleRepository articleRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articles, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewArticles);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticleAdapter(getContext(), articles);
        recyclerView.setAdapter(adapter);

        // Инициализация репозитория
        articleRepository = new ArticleRepository(FirebaseFirestore.getInstance());

        // Загрузка статей
        loadArticles();

        // Обработчик нажатия кнопки для добавления новой статьи
        root.findViewById(R.id.add_articles_button).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.fragment_add_articles)
        );

        return root;
    }

    private void loadArticles() {
        articleRepository.getAllArticles().thenAccept(articles -> {
            this.articles.clear();
            this.articles.addAll(articles);
            adapter.notifyDataSetChanged();
        }).exceptionally(e -> {
            // Обработка ошибок
            return null;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очистка ссылок
        recyclerView.setAdapter(null);
    }
}
