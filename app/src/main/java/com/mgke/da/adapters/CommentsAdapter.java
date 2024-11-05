package com.mgke.da.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mgke.da.R;
import com.mgke.da.models.Comment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private List<Comment> comments = new ArrayList<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
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

        public void bind(Comment comment) {
            userName.setText(comment.getUserName());
            commentText.setText(comment.getText());

            // Форматирование даты
            // Убедитесь, что comment.getTimestamp() возвращает Date
            String formattedDate = dateFormat.format(comment.getTimestamp());
            commentDate.setText(formattedDate);

            Glide.with(itemView.getContext())
                    .load(comment.getUserImage())
                    .placeholder(R.drawable.account_fon1)
                    .error(R.drawable.account_fon2)
                    .into(userImage);
        }


    }
}
