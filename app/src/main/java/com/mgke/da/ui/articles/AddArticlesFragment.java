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
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mgke.da.R;
import com.mgke.da.models.Article;
import com.mgke.da.repository.ArticleRepository;
import java.util.Date;
import java.util.UUID;

public class AddArticlesFragment extends Fragment {

    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 2;
    private ImageView articleImage;
    private Uri imageUri;
    private ArticleRepository articleRepository;
    private String imageUrl;
    private ProgressDialog progressDialog;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        articleRepository = new ArticleRepository(db);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_articles, container, false);

        articleImage = view.findViewById(R.id.articleImage);
        Button addButton = view.findViewById(R.id.add_article_button);
        ImageView closeButton = view.findViewById(R.id.close);
        EditText nameEditText = view.findViewById(R.id.article_name);
        EditText descriptionEditText = view.findViewById(R.id.article_description);
        EditText textEditText = view.findViewById(R.id.article_text);

        closeButton.setOnClickListener(v -> closeFragment());

        articleImage.setOnClickListener(v -> {
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
            String name = nameEditText.getText().toString();
            String description = descriptionEditText.getText().toString();
            String text = textEditText.getText().toString();
            if (validateInputs(name, description, text)) {
                saveArticleToDb(name, description, text);
            } else {
                Toast.makeText(getActivity(), getText(R.string.fill_in_all_the_fields), Toast.LENGTH_SHORT).show();
            }
        });
        return view;
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
                    Log.d("ImageURL", "URL изображения: " + imageUrl);
                    progressDialog.dismiss();

                    Glide.with(getActivity())
                            .load(imageUrl)
                            .timeout(60000)
                            .placeholder(R.drawable.account_fon1)
                            .error(R.drawable.account_fon2)
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(articleImage);

                }).addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getActivity(), "Ошибка получения URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

            }).addOnFailureListener(e -> {
                progressDialog.dismiss(); // Закрываем диалог
                Toast.makeText(getActivity(), "Ошибка загрузки изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(getActivity(), "Изображение не выбрано", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveArticleToDb(String name, String description, String text) {
        if (imageUrl == null) {
            Toast.makeText(getActivity(), "Ошибка: изображение не загружено", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = UUID.randomUUID().toString();
        Date timestamp = new Date();
        Article article = new Article(id, name, description, text, imageUrl, timestamp);

        articleRepository.addArticle(article).thenAccept(savedArticle -> {
            Toast.makeText(getActivity(), "Статья успешно добавлена", Toast.LENGTH_SHORT).show();
            closeFragment();
        }).exceptionally(e -> {
            Toast.makeText(getActivity(), "Ошибка при добавлении статьи: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        });
    }

    private boolean validateInputs(String name, String description, String text) {
        return !name.isEmpty() && !description.isEmpty() && !text.isEmpty() && imageUrl != null;
    }

    private void closeFragment() {
        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(getActivity(), "Разрешение на чтение хранилища отклонено", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
