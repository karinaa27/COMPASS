    package com.mgke.da.ui.articles;

    import android.content.res.Configuration;
    import android.os.Bundle;
    import android.text.TextUtils;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.EditText;
    import android.widget.ImageButton;
    import android.widget.ImageView;
    import android.widget.TextView;
    import androidx.fragment.app.Fragment;
    import androidx.navigation.fragment.NavHostFragment;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;
    import com.bumptech.glide.Glide;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.mgke.da.R;
    import com.mgke.da.adapters.CommentsAdapter;
    import com.mgke.da.models.Article;
    import com.mgke.da.models.Comment;
    import com.mgke.da.repository.ArticleRepository;
    import com.mgke.da.repository.CommentRepository;
    import com.mgke.da.repository.LikeRepository;
    import com.mgke.da.repository.PersonalDataRepository;
    import java.text.SimpleDateFormat;
    import java.util.Locale;

    public class ArticleFragment extends Fragment {

        private String articleId;
        private ArticleRepository articleRepository;
        private LikeRepository likeRepository;
        private CommentRepository commentRepository;

        private ImageView articleImage;
        private TextView articleTitle, articleDate, articleContent, likeCount;
        private ImageButton likeButton;
        private RecyclerView commentsRecyclerView;
        private CommentsAdapter commentsAdapter;

        private EditText commentEditText;
        private ImageButton sendCommentButton;

        private boolean isLiked = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null) {
                articleId = getArguments().getString("articleId");
            }

            articleRepository = new ArticleRepository(FirebaseFirestore.getInstance());
            likeRepository = new LikeRepository(FirebaseFirestore.getInstance());
            commentRepository = new CommentRepository(FirebaseFirestore.getInstance());
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

            ImageView deleteButton = root.findViewById(R.id.deleteButton);
            ImageView editButton = root.findViewById(R.id.editButton);
            checkIfUserIsAdmin(deleteButton, editButton);

            loadArticleDetails();
            setupLikeButton();
            setupCommentsRecyclerView();
            setupCommentsButton(root);
            setupSendCommentButton();

            // В методе onCreateView()
            deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
            editButton.setOnClickListener(v -> editArticle()); // Обработчик для редактирования статьи

            return root;
        }

        private void loadArticleDetails() {
            articleRepository.getAllArticles().thenAccept(articles -> {
                for (Article article : articles) {
                    if (article.id.equals(articleId)) {
                        String currentLanguage = Locale.getDefault().getLanguage();

                        if ("ru".equals(currentLanguage)) {
                            articleTitle.setText(article.nameRu);
                            articleContent.setText(article.textRu);
                        } else {
                            articleTitle.setText(article.nameEn);
                            articleContent.setText(article.textEn);
                        }

                        articleDate.setText(new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(article.timestamp));

                        Glide.with(requireContext())
                                .load(article.image)
                                .placeholder(isDarkMode() ? R.drawable.user_icon_night : R.drawable.user_icon)  // Выбираем изображение в зависимости от темы
                                .error(isDarkMode() ? R.drawable.user_icon_night : R.drawable.user_icon)  // Выбираем изображение в зависимости от темы
                                .into(articleImage);
                        break;
                    }
                }
            });
        }
        private boolean isDarkMode() {
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        }


        private void checkIfUserIsAdmin(ImageView deleteButton, ImageView editButton) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

            if (userId != null) {
                PersonalDataRepository personalDataRepository = new PersonalDataRepository(FirebaseFirestore.getInstance());
                personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
                    if (personalData != null && personalData.isAdmin) {
                        deleteButton.setVisibility(View.VISIBLE);
                        editButton.setVisibility(View.VISIBLE); // Показать кнопку редактирования для администратора
                    } else {
                        deleteButton.setVisibility(View.GONE);
                        editButton.setVisibility(View.GONE);
                    }
                }).exceptionally(ex -> {
                    Log.e("ArticleFragment", "Failed to fetch user data", ex);
                    return null;
                });
            }
        }
        // Метод для отображения диалога подтверждения удаления
        private void showDeleteConfirmationDialog() {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Подтвердите удаление")
                    .setMessage("Вы уверены, что хотите удалить эту статью?")
                    .setPositiveButton("Удалить", (dialog, which) -> deleteArticle())
                    .setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)  // Не позволяет закрыть диалог другим способом
                    .show();
        }

        private void deleteArticle() {
            if (articleId != null) {
                articleRepository.deleteArticle(articleId);

                getActivity().onBackPressed();
            }
        }
        private void editArticle() {
            articleRepository.getArticleById(articleId).thenAccept(article -> {
                if (article != null) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("article", article);
                    NavHostFragment.findNavController(this).navigate(R.id.action_articleFragment_to_addArticleFragment, bundle);
                } else {
                    Log.e("ArticleFragment", "Статья с id " + articleId + " не найдена.");
                }
            }).exceptionally(e -> {
                Log.e("ArticleFragment", "Ошибка при загрузке статьи с id " + articleId, e);
                return null;
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
                likeButton.setImageResource(R.drawable.baseline_favorite_24);
            } else {
                likeButton.setImageResource(R.drawable.baseline_favorite_border_24);
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
                loadComments();
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
