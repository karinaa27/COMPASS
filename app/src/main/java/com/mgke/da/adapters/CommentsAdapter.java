package com.mgke.da.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.models.Comment;
import com.mgke.da.repository.PersonalDataRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments = new ArrayList<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
    private Context context;  // Declare context

    public CommentsAdapter(Context context) {
        this.context = context;  // Initialize context
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment, context);  // Pass context to the bind method
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public void setComments(List<Comment> comments) {
        this.comments.clear();
        this.comments.addAll(comments);
        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        private ImageView userImage;
        private TextView userName;
        private TextView commentText;
        private TextView commentDate;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.userImage);
            userName = itemView.findViewById(R.id.userName);
            commentText = itemView.findViewById(R.id.commentText);
            commentDate = itemView.findViewById(R.id.commentDate);
        }

        public void bind(Comment comment, Context context) {
            // Отображаем имя пользователя
            userName.setText(comment.getUserName());
            commentText.setText(comment.getText());

            // Форматируем дату в зависимости от языка
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy", getLocale(context));
            String formattedDate = dateFormat.format(comment.getTimestamp());
            commentDate.setText(formattedDate);

            // Загружаем персональные данные пользователя по userId
            PersonalDataRepository repository = new PersonalDataRepository(FirebaseFirestore.getInstance());
            repository.getPersonalDataById(comment.getUserId()).thenAccept(personalData -> {
                if (personalData != null) {
                    // Обновляем имя пользователя, если оно изменилось
                    if (personalData.username != null && !personalData.username.isEmpty()) {
                        userName.setText(personalData.username);  // Обновляем имя пользователя
                    }
                    // Загружаем аватар, если он существует
                    if (personalData.avatarUrl != null && !personalData.avatarUrl.isEmpty()) {
                        Glide.with(itemView.getContext())
                                .load(personalData.avatarUrl)
                                .circleCrop()
                                .placeholder(isDarkMode(context) ? R.drawable.user_icon_night : R.drawable.user_icon)
                                .error(isDarkMode(context) ? R.drawable.user_icon_night : R.drawable.user_icon)
                                .into(userImage);
                    } else {
                        Glide.with(itemView.getContext())
                                .load(isDarkMode(context) ? R.drawable.user_icon_night : R.drawable.user_icon)
                                .into(userImage);
                    }
                }
            }).exceptionally(e -> {
                // Логируем ошибку, но оставляем стандартное изображение и имя
                Glide.with(itemView.getContext())
                        .load(isDarkMode(context) ? R.drawable.user_icon_night : R.drawable.user_icon)
                        .into(userImage);
                return null;
            });
        }


        private boolean isDarkMode(Context context) {
            int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
        }
        private Locale getLocale(Context context) {
            return context.getResources().getConfiguration().locale;
        }
    }
}

