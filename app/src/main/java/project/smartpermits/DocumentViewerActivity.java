package project.smartpermits;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.button.MaterialButton;

import project.smartpermits.api.RetrofitClient;

public class DocumentViewerActivity extends AppCompatActivity {

    private ImageView ivDocument;
    private ProgressBar progressBar;
    private LinearLayout errorView;
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_viewer);

        ivDocument = findViewById(R.id.ivDocument);
        progressBar = findViewById(R.id.progressBar);
        errorView = findViewById(R.id.errorView);
        TextView tvFileName = findViewById(R.id.tvFileName);
        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnRetry = findViewById(R.id.btnRetry);

        String fileName = getIntent().getStringExtra("file_name");
        if (fileName == null || fileName.isEmpty()) {
            finish();
            return;
        }

        tvFileName.setText(fileName);

        String baseUrl = RetrofitClient.getInstance(this).getBaseUrl();
        imageUrl = baseUrl + "uploads/" + fileName;

        btnBack.setOnClickListener(v -> finish());
        btnRetry.setOnClickListener(v -> loadImage());

        loadImage();
    }

    private void loadImage() {
        progressBar.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);

        Glide.with(this)
                .load(imageUrl)
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        errorView.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(ivDocument);
    }
}

