package com.mgke.da.ui.articles;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.activity.LoginActivity;
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
    private ImageView emptyStateImageView; // Ссылка на ImageView для гифки

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_articles, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewArticles);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ArticleAdapter(getContext(), articles);
        recyclerView.setAdapter(adapter);

        emptyStateImageView = root.findViewById(R.id.emptyStateImageView); // Инициализация ImageView
        firestore = FirebaseFirestore.getInstance();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            // Пользователь не авторизован, запускаем LoginActivity
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish(); // Завершаем текущую Activity, чтобы пользователь не мог вернуться назад
            return root;
        }

        String userId = auth.getCurrentUser().getUid();
        articleRepository = new ArticleRepository(firestore);

        loadArticles();

        addArticlesButton = root.findViewById(R.id.add_articles_button);
        checkIfAdmin(userId);

        return root;
    }



    private void loadArticles() {
        articleRepository.getAllArticles().thenAccept(articles -> {
            this.articles.clear();
            this.articles.addAll(articles);
            adapter.notifyDataSetChanged();

            // Проверка, если статьи не загружены, показать гифку
            if (this.articles.isEmpty()) {
                recyclerView.setVisibility(View.GONE); // Скрыть RecyclerView
                emptyStateImageView.setVisibility(View.VISIBLE); // Показать ImageView с гифкой
                Glide.with(this)
                        .load(R.drawable.working_chart) // Укажите путь к вашему gif
                        .into(emptyStateImageView); // Загружаем гифку с помощью Glide
            } else {
                recyclerView.setVisibility(View.VISIBLE); // Показываем RecyclerView
                emptyStateImageView.setVisibility(View.GONE); // Скрываем гифку
            }
        }).exceptionally(e -> {
            return null;
        });
    }

    private void checkIfAdmin(String userId) {
        firestore.collection("personalData").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        PersonalData personalData = documentSnapshot.toObject(PersonalData.class);
                        if (personalData.isAdmin == true) {
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
                    addArticlesButton.setVisibility(View.GONE);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
    }
}
