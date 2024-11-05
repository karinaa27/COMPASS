package com.mgke.da.ui.articles;

import android.os.Bundle;
import android.text.TextUtils; // Импортируйте TextUtils
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText; // Импортируйте EditText
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.CommentsAdapter;
import com.mgke.da.models.Article;
import com.mgke.da.models.Comment; // Импортируйте модель для комментариев
import com.mgke.da.repository.ArticleRepository;
import com.mgke.da.repository.CommentRepository; // Импортируйте репозиторий для комментариев
import com.mgke.da.repository.LikeRepository;
import com.mgke.da.repository.PersonalDataRepository;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ArticleFragment extends Fragment {

    private String articleId;
    private ArticleRepository articleRepository;
    private LikeRepository likeRepository;
    private CommentRepository commentRepository; // Репозиторий для комментариев

    private ImageView articleImage;
    private TextView articleTitle, articleDate, articleContent, likeCount;
    private ImageButton likeButton;
    private RecyclerView commentsRecyclerView; // RecyclerView для комментариев
    private CommentsAdapter commentsAdapter; // Адаптер для комментариев

    private EditText commentEditText; // EditText для ввода комментариев
    private ImageButton sendCommentButton; // Кнопка для отправки комментариев

    private boolean isLiked = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            articleId = getArguments().getString("articleId");
        }

        articleRepository = new ArticleRepository(FirebaseFirestore.getInstance());
        likeRepository = new LikeRepository(FirebaseFirestore.getInstance());
        commentRepository = new CommentRepository(FirebaseFirestore.getInstance()); // Инициализация репозитория комментариев
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_article, container, false);

        articleImage = root.findViewById(R.id.articleImage);
        articleTitle = root.findViewById(R.id.articleTitle);
        articleDate = root.findViewById(R.id.articleDate);
        articleContent = root.findViewById(R.id.articleContent);
        likeButton = root.findViewById(R.id.likeButton);
        likeCount = root.findViewById(R.id.likeCount);
        commentsRecyclerView = root.findViewById(R.id.commentsRecyclerView);
        commentEditText = root.findViewById(R.id.commentEditText);
        sendCommentButton = root.findViewById(R.id.sendCommentButton);

        loadArticleDetails();
        setupLikeButton();
        setupCommentsRecyclerView();
        setupCommentsButton(root);
        setupSendCommentButton();

        return root;
    }

    private void loadArticleDetails() {
        articleRepository.getAllArticles().thenAccept(articles -> {
            for (Article article : articles) {
                if (article.id.equals(articleId)) {
                    articleTitle.setText(article.name);
                    articleDate.setText(new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(article.timestamp));
                    articleContent.setText(article.text);

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

    private void setupLikeButton() {
        likeButton.setOnClickListener(v -> toggleLike());
        updateLikeStatus();
    }

    private void toggleLike() {
        if (isLiked) {
            likeRepository.removeLike(articleId).thenRun(() -> {
                isLiked = false;
                updateLikeIcon();
                updateLikeCount(-1);
            });
        } else {
            likeRepository.addLike(articleId).thenRun(() -> {
                isLiked = true;
                updateLikeIcon();
                updateLikeCount(1);
            });
        }
    }

    private void updateLikeStatus() {
        likeRepository.isLiked(articleId).thenAccept(liked -> {
            isLiked = liked;
            updateLikeIcon();
        });

        likeRepository.getLikeCount(articleId).thenAccept(count -> {
            likeCount.setText(String.valueOf(count));
        });
    }

    private void updateLikeCount(int delta) {
        int count = Integer.parseInt(likeCount.getText().toString()) + delta;
        likeCount.setText(String.valueOf(count));
    }

    private void updateLikeIcon() {
        if (isLiked) {
            likeButton.setImageResource(R.drawable.baseline_favorite_24); // Иконка для активного лайка
        } else {
            likeButton.setImageResource(R.drawable.baseline_favorite_border_24); // Иконка для неактивного лайка
        }
    }

    private void setupCommentsRecyclerView() {
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        commentsAdapter = new CommentsAdapter();
        commentsRecyclerView.setAdapter(commentsAdapter);
    }

    private void setupCommentsButton(View root) {
        ImageView commentsButton = root.findViewById(R.id.commentsButton);
        commentsButton.setOnClickListener(v -> toggleComments());
    }

    private void toggleComments() {
        if (commentsRecyclerView.getVisibility() == View.GONE) {
            loadComments(); // Загрузка комментариев при открытии
            commentsRecyclerView.setVisibility(View.VISIBLE);
            commentEditText.setVisibility(View.VISIBLE);
            sendCommentButton.setVisibility(View.VISIBLE);
        } else {
            commentsRecyclerView.setVisibility(View.GONE);
            commentEditText.setVisibility(View.GONE);
            sendCommentButton.setVisibility(View.GONE);
        }
    }

    private void loadComments() {
        commentRepository.getCommentsForArticle(articleId).thenAccept(comments -> {
            commentsAdapter.setComments(comments);
        });
    }

    private void setupSendCommentButton() {
        sendCommentButton.setOnClickListener(v -> {
            String commentText = commentEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(commentText)) {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

                if (userId != null) {
                    PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());

                    personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
                        if (personalData != null) {
                            String userName = personalData.username;
                            String userImage = personalData.avatarUrl != null ? personalData.avatarUrl : "default_user_icon_url";

                            Comment comment = new Comment(userId, userName, userImage, commentText, System.currentTimeMillis());
                            commentRepository.addComment(articleId, comment).thenRun(() -> {
                                commentEditText.setText("");
                                loadComments();
                            });
                        }
                    }).exceptionally(ex -> {
                        Log.e("SetupSendComment", "Failed to fetch user data", ex);
                        return null;
                    });
                }
            }
        });
    }
}
