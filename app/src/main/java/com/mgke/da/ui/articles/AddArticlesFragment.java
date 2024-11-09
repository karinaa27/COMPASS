package com.mgke.da.ui.articles;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mgke.da.R;
import com.mgke.da.models.Article;
import com.mgke.da.repository.ArticleRepository;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class AddArticlesFragment extends Fragment {

    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 2;
    private Uri imageUri;
    private ArticleRepository articleRepository;
    private String imageUrl;
    private ProgressDialog progressDialog;
    private Article currentArticle;
    private Button addButton;
    private ImageView closeButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null && data.getData() != null) {
                        imageUri = data.getData();
                        uploadImageToFirebase();
                    }
                }
            });

    public static AddArticlesFragment newInstance(Article article) {
        AddArticlesFragment fragment = new AddArticlesFragment();
        Bundle args = new Bundle();
        args.putSerializable("article", article);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация Firestore и репозитория
        db = FirebaseFirestore.getInstance();
        articleRepository = new ArticleRepository(db);
        auth = FirebaseAuth.getInstance();

        // Проверяем, переданы ли аргументы и извлекаем объект article из Bundle
        if (getArguments() != null) {
            // Проверяем, что переданный объект действительно является экземпляром Article
            Serializable article = getArguments().getSerializable("article");
            if (article instanceof Article) {
                currentArticle = (Article) article;
            } else {
                // Обрабатываем случай, если аргумент не был передан или он не является объектом Article
                Log.e("AddArticlesFragment", "Неверный или отсутствующий аргумент article");
                currentArticle = new Article();
            }
        } else {
            // Если аргумент не передан, инициализируем currentArticle как новый объект
            currentArticle = new Article();
        }

        // Получаем информацию о текущем пользователе
        checkIfUserIsAdmin();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_articles, container, false);

        addButton = view.findViewById(R.id.add_article_button);
        closeButton = view.findViewById(R.id.close);

        EditText nameEditTextRu = view.findViewById(R.id.article_name_ru);
        EditText nameEditTextEn = view.findViewById(R.id.article_name_en);
        EditText descriptionEditTextRu = view.findViewById(R.id.article_description_ru);
        EditText descriptionEditTextEn = view.findViewById(R.id.article_description_en);
        EditText textEditTextRu = view.findViewById(R.id.article_text_ru);
        EditText textEditTextEn = view.findViewById(R.id.article_text_en);
        EditText imageUrlEditText = view.findViewById(R.id.article_image);

        closeButton.setOnClickListener(v -> closeFragment());

        if (currentArticle != null) {
            nameEditTextRu.setText(currentArticle.nameRu);
            nameEditTextEn.setText(currentArticle.nameEn);
            descriptionEditTextRu.setText(currentArticle.descriptionRu);
            descriptionEditTextEn.setText(currentArticle.descriptionEn);
            textEditTextRu.setText(currentArticle.textRu);
            textEditTextEn.setText(currentArticle.textEn);
            imageUrl = currentArticle.image;
            imageUrlEditText.setText(imageUrl);
            addButton.setText("Обновить статью");
        }

        imageUrlEditText.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                openGallery();
            } else {
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);
                } else {
                    openGallery();
                }
            }
        });

        addButton.setOnClickListener(v -> {
            String nameRu = nameEditTextRu.getText().toString();
            String nameEn = nameEditTextEn.getText().toString();
            String descriptionRu = descriptionEditTextRu.getText().toString();
            String descriptionEn = descriptionEditTextEn.getText().toString();
            String textRu = textEditTextRu.getText().toString();
            String textEn = textEditTextEn.getText().toString();

            if (validateInputs(nameRu, nameEn, descriptionRu, descriptionEn, textRu, textEn)) {
                saveArticleToDb(nameRu, nameEn, descriptionRu, descriptionEn, textRu, textEn);
            }
        });

        return view;
    }

    private void checkIfUserIsAdmin() {
        // Получаем текущего пользователя
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            db.collection("personalData").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                            if (isAdmin != null && isAdmin) {
                                addButton.setVisibility(View.VISIBLE); // Показываем кнопку, если пользователь администратор
                            } else {
                                addButton.setVisibility(View.GONE); // Скрываем кнопку, если пользователь не администратор
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("AddArticlesFragment", "Ошибка при получении данных пользователя", e));
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void uploadImageToFirebase() {
        if (imageUri != null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Загрузка изображения...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            String id = UUID.randomUUID().toString();
            StorageReference fileReference = FirebaseStorage.getInstance()
                    .getReference("articles_images")
                    .child(id + ".jpg");
            UploadTask uploadTask = fileReference.putFile(imageUri);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                fileReference.getDownloadUrl().addOnSuccessListener(downloadUrl -> {
                    imageUrl = downloadUrl.toString();
                    progressDialog.dismiss();

                    EditText imageUrlEditText = getView().findViewById(R.id.article_image);
                    imageUrlEditText.setText(imageUrl);
                }).addOnFailureListener(e -> {
                    Log.e("Firebase", "Ошибка при получении URL изображения", e);
                    progressDialog.dismiss();
                });
            }).addOnFailureListener(e -> {
                Log.e("Firebase", "Ошибка при загрузке изображения", e);
                progressDialog.dismiss();
            });
        }
    }

    private void closeFragment() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void saveArticleToDb(String nameRu, String nameEn, String descriptionRu, String descriptionEn, String textRu, String textEn) {
        Article article;
        if (currentArticle != null) {
            article = currentArticle;
        } else {
            article = new Article();
        }

        // Всегда устанавливаем метку времени в текущее время
        article.timestamp = new Date();

        // Логируем используемую дату
        Log.d("AddArticlesFragment", "Article Timestamp: " + article.timestamp);

        article.nameRu = nameRu;
        article.nameEn = nameEn;
        article.descriptionRu = descriptionRu;
        article.descriptionEn = descriptionEn;
        article.textRu = textRu;
        article.textEn = textEn;
        article.image = imageUrl;

        articleRepository.addOrUpdateArticle(article)
                .thenAccept(savedArticle -> {
                    Toast.makeText(getActivity(), "Статья успешно сохранена", Toast.LENGTH_SHORT).show();
                    closeFragment();
                })
                .exceptionally(e -> {
                    Log.e("ArticleRepo", "Ошибка при сохранении статьи: " + e.toString());
                    Toast.makeText(getActivity(), "Не удалось сохранить статью", Toast.LENGTH_SHORT).show();
                    return null;
                });
    }


    private boolean validateInputs(String nameRu, String nameEn, String descriptionRu, String descriptionEn, String textRu, String textEn) {
        if (nameRu.isEmpty() || nameEn.isEmpty() || descriptionRu.isEmpty() || descriptionEn.isEmpty() || textRu.isEmpty() || textEn.isEmpty()) {
            Toast.makeText(getActivity(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
