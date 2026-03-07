package project.smartpermits.adapters;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import project.smartpermits.R;
import project.smartpermits.models.Comment;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Comment> comments = new ArrayList<>();
    private final int currentUserId;

    public ChatAdapter(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout container, bubble;
        TextView tvMessage, tvAuthor, tvTime;

        ChatViewHolder(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.chatContainer);
            bubble = itemView.findViewById(R.id.chatBubble);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(Comment comment) {
            tvMessage.setText(comment.getMessage());
            String role = comment.getAuthorRole() != null ? comment.getAuthorRole() : "";
            String name = comment.getAuthorName() != null ? comment.getAuthorName() : "";
            String roleLabel = role.isEmpty() ? "" : " (" + role.substring(0, 1).toUpperCase() + role.substring(1) + ")";
            tvAuthor.setText(name + roleLabel);

            String time = comment.getCreatedAt();
            if (time != null && time.length() >= 16) {
                time = time.substring(0, 16).replace("T", " ");
            }
            tvTime.setText(time);

            boolean isMe = comment.getUserId() == currentUserId;
            container.setGravity(isMe ? Gravity.END : Gravity.START);
            bubble.setBackgroundResource(isMe ? R.drawable.bg_chat_me : R.drawable.bg_chat_other);
        }
    }
}

