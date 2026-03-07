package project.smartpermits;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.Document;
import project.smartpermits.models.Permit;
import project.smartpermits.models.ReviewRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PermitReviewActivity extends AppCompatActivity {

    private TextView tvApplicant, tvPermitType, tvDate, tvFee, tvDescription, tvDocuments;
    private TextInputEditText etNotes;
    private MaterialButton btnApprove, btnReject, btnComments;
    private MaterialCardView cardDescription, cardDocuments;
    private ProgressBar progressBar;
    private RecyclerView recyclerDocPreview;
    private int permitId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permit_review);

        tvApplicant = findViewById(R.id.tvApplicant);
        tvPermitType = findViewById(R.id.tvPermitType);
        tvDate = findViewById(R.id.tvDate);
        tvFee = findViewById(R.id.tvFee);
        tvDescription = findViewById(R.id.tvDescription);
        tvDocuments = findViewById(R.id.tvDocuments);
        etNotes = findViewById(R.id.etNotes);
        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnComments = findViewById(R.id.btnComments);
        cardDescription = findViewById(R.id.cardDescription);
        cardDocuments = findViewById(R.id.cardDocuments);
        progressBar = findViewById(R.id.progressBar);
        recyclerDocPreview = findViewById(R.id.recyclerDocPreview);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        permitId = getIntent().getIntExtra("permit_id", -1);
        if (permitId == -1) {
            finish();
            return;
        }

        if (recyclerDocPreview != null) {
            recyclerDocPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        }

        btnApprove.setOnClickListener(v -> reviewPermit("approved"));
        btnReject.setOnClickListener(v -> reviewPermit("rejected"));

        if (btnComments != null) {
            btnComments.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("permit_id", permitId);
                startActivity(intent);
            });
        }

        loadPermit();
    }

    private void loadPermit() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().getPermit(permitId)
                .enqueue(new Callback<Permit>() {
                    @Override
                    public void onResponse(Call<Permit> call, Response<Permit> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            displayPermit(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<Permit> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PermitReviewActivity.this, "Error loading permit", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayPermit(Permit permit) {
        tvApplicant.setText(permit.getApplicantName() != null ? permit.getApplicantName() : "Unknown");
        tvPermitType.setText(permit.getPermitType());

        String date = permit.getCreatedAt();
        if (date != null && date.length() >= 10) {
            date = date.substring(0, 10);
        }
        tvDate.setText(date);
        tvFee.setText(String.format("$%.2f", permit.getFeeAmount()));

        if (permit.getDescription() != null && !permit.getDescription().isEmpty()) {
            cardDescription.setVisibility(View.VISIBLE);
            tvDescription.setText(permit.getDescription());
        }

        if (permit.getDocuments() != null && !permit.getDocuments().isEmpty()) {
            cardDocuments.setVisibility(View.VISIBLE);
            StringBuilder docs = new StringBuilder();
            for (Document doc : permit.getDocuments()) {
                docs.append("\u2022 ").append(doc.getFileName()).append("\n");
            }
            tvDocuments.setText(docs.toString().trim());

            if (recyclerDocPreview != null) {
                recyclerDocPreview.setVisibility(View.VISIBLE);
                recyclerDocPreview.setAdapter(new DocCarouselAdapter(permit.getDocuments()));
            }
        }
    }

    private void reviewPermit(String action) {
        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
        progressBar.setVisibility(View.VISIBLE);
        btnApprove.setEnabled(false);
        btnReject.setEnabled(false);

        RetrofitClient.getInstance(this).getApi()
                .reviewPermit(permitId, new ReviewRequest(action, notes))
                .enqueue(new Callback<Permit>() {
                    @Override
                    public void onResponse(Call<Permit> call, Response<Permit> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            String msg = "approved".equals(action) ? "Permit Approved" : "Permit Rejected";
                            Toast.makeText(PermitReviewActivity.this, msg, Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            btnApprove.setEnabled(true);
                            btnReject.setEnabled(true);
                            Toast.makeText(PermitReviewActivity.this, "Review failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Permit> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnApprove.setEnabled(true);
                        btnReject.setEnabled(true);
                        Toast.makeText(PermitReviewActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private class DocCarouselAdapter extends RecyclerView.Adapter<DocCarouselAdapter.VH> {
        private final List<Document> docs;
        DocCarouselAdapter(List<Document> docs) { this.docs = docs; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            int size = (int) (120 * getResources().getDisplayMetrics().density);
            iv.setLayoutParams(new RecyclerView.LayoutParams(size, size));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setPadding(8, 8, 8, 8);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Document doc = docs.get(position);
            String url = RetrofitClient.getInstance(PermitReviewActivity.this).getBaseUrl() + "uploads/" + doc.getFileName();
            Glide.with(PermitReviewActivity.this).load(url).centerCrop().into((ImageView) holder.itemView);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(PermitReviewActivity.this, DocumentViewerActivity.class);
                intent.putExtra("file_name", doc.getFileName());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return docs.size(); }

        class VH extends RecyclerView.ViewHolder {
            VH(View v) { super(v); }
        }
    }
}
