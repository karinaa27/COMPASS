    package com.mgke.da.ui.articles;

    import android.app.AlertDialog;
    import android.app.ProgressDialog;
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
    import android.widget.Toast;

    import androidx.fragment.app.Fragment;
    import androidx.navigation.NavController;
    import androidx.navigation.Navigation;
    import androidx.navigation.fragment.NavHostFragment;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;
    import com.bumptech.glide.Glide;
    import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
    import java.util.Date;
    import java.util.Locale;
    import java.util.concurrent.CompletableFuture;

    public class ArticleFragment extends Fragment {

        private String articleId;
        private ArticleRepository articleRepository;
        private LikeRepository likeRepository;
        private CommentRepository commentRepository;
        private ProgressDialog progressDialog;
        private ImageView articleImage;
        private TextView articleTitle, articleDate, articleContent, likeCount;
        private ImageButton likeButton;
        private RecyclerView commentsRecyclerView;
        private CommentsAdapter commentsAdapter;
        private EditText commentEditText;
        private ImageButton sendCommentButton;
        private boolean isLiked = false;
        private ImageView backButton;

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

            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage(getString(R.string.load));
            progressDialog.setCancelable(false);
            articleImage = root.findViewById(R.id.articleImage);
            articleTitle = root.findViewById(R.id.articleTitle);
            articleDate = root.findViewById(R.id.articleDate);
            articleContent = root.findViewById(R.id.articleContent);
            likeButton = root.findViewById(R.id.likeButton);
            likeCount = root.findViewById(R.id.likeCount);
            commentsRecyclerView = root.findViewById(R.id.commentsRecyclerView);
            commentEditText = root.findViewById(R.id.commentEditText);
            sendCommentButton = root.findViewById(R.id.sendCommentButton);
            ImageView backButton = root.findViewById(R.id.backButton);
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

            backButton.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                navController.popBackStack();
            });

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

                        // Логирование перед установкой даты
                        long timestamp = article.timestamp.getTime();  // Преобразуем Date в long (миллисекунды)
                        String formattedDate = formatDate(timestamp);

                        articleDate.setText(formattedDate);

                        Glide.with(getActivity())
                                .load(article.image)
                                .placeholder(R.drawable.account_fon1)
                                .error(R.drawable.account_fon2)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .into(articleImage);
                        break;
                    }
                }
            }).exceptionally(ex -> {
                return null;
            });
        }

        private String formatDate(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()); // Укажите нужный формат
            Date date = new Date(timestamp);
            return sdf.format(date);
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
                    return null;
                });
            }
        }
        // Метод для отображения диалога подтверждения удаления
        private void showDeleteConfirmationDialog() {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.article_delete_confirmation_title))
                    .setMessage(getString(R.string.article_delete_confirmation_message))
                    .setPositiveButton(getString(R.string.article_delete_button), (dialog, which) -> deleteArticle())
                    .setNegativeButton(getString(R.string.article_cancel_button), (dialog, which) -> dialog.dismiss())
                    .setCancelable(false)  // Не позволяет закрыть диалог другим способом
                    .show();
        }

        private void deleteArticle() {
            if (articleId == null) {
                return;
            }

            // Определяем текущий язык и выбираем соответствующие строки
            String deletingMessage = getString(R.string.deleting_article);
            String errorMessage = getString(R.string.delete_article_error);
            String confirmMessage = getString(R.string.confirm_delete_article); // Сообщение для подтверждения

            // Создаем и показываем диалоговое окно для подтверждения удаления
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.confirm_delete) // Заголовок диалога
                    .setMessage(confirmMessage) // Сообщение
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        // Если пользователь подтвердил, показываем ProgressDialog и начинаем удаление
                        ProgressDialog progressDialog = new ProgressDialog(getContext());
                        progressDialog.setMessage(deletingMessage);
                        progressDialog.setCancelable(false);
                        progressDialog.show();

                        articleRepository.deleteArticle(articleId);

                        FirebaseFirestore.getInstance().collection("article").document(articleId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Скрываем ProgressDialog после успешного удаления
                                    progressDialog.dismiss();
                                    getActivity().onBackPressed();
                                })
                                .addOnFailureListener(e -> {
                                    // Скрываем ProgressDialog в случае ошибки
                                    progressDialog.dismiss();
                                    // Отображаем сообщение об ошибке
                                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        // Если пользователь отменил, просто закрываем диалог
                        dialog.dismiss();
                    })
                    .create()
                    .show();
        }



        private void editArticle() {
            articleRepository.getArticleById(articleId).thenAccept(article -> {
                if (article != null) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("article", article);
                    NavHostFragment.findNavController(this).navigate(R.id.action_articleFragment_to_addArticleFragment, bundle);
                } else {

                }
            }).exceptionally(e -> {

                return null;
            });
        }


        private void setupLikeButton() {
            likeButton.setOnClickListener(v -> toggleLike());
            updateLikeStatus();
        }

        private void toggleLike() {
            CompletableFuture<Void> future;

            // Определяем действие: добавление или удаление лайка
            if (isLiked) {
                future = likeRepository.removeLike(articleId).thenRun(() -> {
                    isLiked = false;
                    // Обновляем UI в основном потоке после успешного удаления лайка
                    updateUIAfterLikeToggle(-1);
                });
            } else {
                future = likeRepository.addLike(articleId).thenRun(() -> {
                    isLiked = true;
                    // Обновляем UI в основном потоке после успешного добавления лайка
                    updateUIAfterLikeToggle(1);
                });
            }

            future.exceptionally(ex -> {

                return null;
            });
        }
        private void updateUIAfterLikeToggle(int delta) {
            // Обновляем UI в главном потоке
            getActivity().runOnUiThread(() -> {
                updateLikeIcon();
                updateLikeCount(delta); // Обновляем счетчик лайков

            });
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
            // Получаем текущее количество лайков и обновляем
            int currentCount = Integer.parseInt(likeCount.getText().toString());
            int newCount = currentCount + delta;

            // Обновляем текст лайков с проверкой на отрицательное значение
            likeCount.setText(String.valueOf(Math.max(newCount, 0))); // Не допускаем отрицательных значений
        }

        private void updateLikeIcon() {
            // Обновляем иконку в зависимости от состояния лайка
            if (isLiked) {
                likeButton.setImageResource(R.drawable.baseline_favorite_24);
            } else {
                likeButton.setImageResource(R.drawable.baseline_favorite_border_24);
            }
        }

        private void setupCommentsRecyclerView() {
            commentsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            commentsAdapter = new CommentsAdapter(requireContext()); // Передаем контекст здесь
            commentsRecyclerView.setAdapter(commentsAdapter);
        }

        private void setupCommentsButton(View root) {
            ImageView commentsButton = root.findViewById(R.id.commentsButton);
            commentsButton.setOnClickListener(v -> toggleComments());
        }

        private void toggleComments() {
            if (commentsRecyclerView.getVisibility() == View.GONE) {
                showLoadingDialog(); // Show the loading dialog before fetching comments
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

        private void showLoadingDialog() {
            if (progressDialog != null && !progressDialog.isShowing()) {
                progressDialog.show();
            }
        }

        private void dismissLoadingDialog() {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }

        private void loadComments() {
            commentRepository.getCommentsForArticle(articleId).thenAccept(comments -> {

                comments.sort((comment1, comment2) -> Long.compare(comment2.getTimestamp().getTime(), comment1.getTimestamp().getTime()));

                dismissLoadingDialog();

                commentsAdapter.setComments(comments);
            }).exceptionally(ex -> {

                dismissLoadingDialog();

                return null;
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
                            return null;
                        });
                    }
                }
            });
        }
    }
