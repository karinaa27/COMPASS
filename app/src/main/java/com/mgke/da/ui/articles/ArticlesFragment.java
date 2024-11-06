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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.ArticleAdapter;
import com.mgke.da.models.Article;
import com.mgke.da.models.PersonalData;
import com.mgke.da.repository.ArticleRepository;
import java.util.ArrayList;
import java.util.List;

public class ArticlesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private List<Article> articles = new ArrayList<>();
    private ArticleRepository articleRepository;
    private FirebaseFirestore firestore;
    private View addArticlesButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articles, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewArticles);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticleAdapter(getContext(), articles);
        recyclerView.setAdapter(adapter);

        // Получаем Firestore и ID текущего пользователя
        firestore = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Инициализация репозитория
        articleRepository = new ArticleRepository(firestore);

        // Загрузка статей
        loadArticles();

        // Получаем ссылку на кнопку
        addArticlesButton = root.findViewById(R.id.add_articles_button);

        // Проверяем, является ли пользователь администратором
        checkIfAdmin(userId);

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

    private void checkIfAdmin(String userId) {
        firestore.collection("personalData").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        PersonalData personalData = documentSnapshot.toObject(PersonalData.class);
                        if (personalData.isAdmin = true) {
                            addArticlesButton.setVisibility(View.VISIBLE);
                            addArticlesButton.setOnClickListener(v ->
                                    Navigation.findNavController(v).navigate(R.id.fragment_add_articles)
                            );
                        } else {
                            addArticlesButton.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Обработка ошибок
                    addArticlesButton.setVisibility(View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очистка ссылок
        recyclerView.setAdapter(null);
    }
}
