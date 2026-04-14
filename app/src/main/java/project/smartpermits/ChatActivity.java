package project.smartpermits;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import project.smartpermits.adapters.ChatAdapter;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.Comment;
import project.smartpermits.models.CommentRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(project.smartpermits.LocaleHelper.applyLocale(newBase));
    }

    private RecyclerView recyclerChat;
    private ChatAdapter adapter;
    private TextInputEditText etMessage;
    private ProgressBar progressBar;
    private int permitId;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadComments();
            refreshHandler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerChat = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessage);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnSend = findViewById(R.id.btnSend);

        permitId = getIntent().getIntExtra("permit_id", -1);
        if (permitId == -1) { finish(); return; }

        int currentUserId = RetrofitClient.getInstance(this).getUserId();

        adapter = new ChatAdapter(currentUserId);
        recyclerChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerChat.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        loadComments();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshHandler.postDelayed(refreshRunnable, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshRunnable);
    }

    private void loadComments() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().getComments(permitId)
                .enqueue(new Callback<List<Comment>>() {
                    @Override
                    public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.setComments(response.body());
                            if (!response.body().isEmpty()) {
                                recyclerChat.scrollToPosition(response.body().size() - 1);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Comment>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void sendMessage() {
        String msg = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (msg.isEmpty()) return;

        etMessage.setText("");
        RetrofitClient.getInstance(this).getApi()
                .addComment(permitId, new CommentRequest(msg))
                .enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(Call<Comment> call, Response<Comment> response) {
                        if (response.isSuccessful()) {
                            loadComments();
                        }
                    }

                    @Override
                    public void onFailure(Call<Comment> call, Throwable t) {
                        Toast.makeText(ChatActivity.this, getString(R.string.failed_to_send), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

