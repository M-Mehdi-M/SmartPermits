package project.smartpermits;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.Locale;

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
    private MaterialCardView cardDescription, cardDocuments, cardMap;
    private ProgressBar progressBar;
    private RecyclerView recyclerDocPreview;
    private LinearLayout documentPreviewContainer;
    private MapView mapViewReview;
    private TextView tvMapCoords;
    private int permitId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

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
        cardMap = findViewById(R.id.cardMap);
        progressBar = findViewById(R.id.progressBar);
        recyclerDocPreview = findViewById(R.id.recyclerDocPreview);
        documentPreviewContainer = findViewById(R.id.documentPreviewContainer);
        mapViewReview = findViewById(R.id.mapViewReview);
        tvMapCoords = findViewById(R.id.tvMapCoords);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        permitId = getIntent().getIntExtra("permit_id", -1);
        if (permitId == -1) {
            finish();
            return;
        }

        if (recyclerDocPreview != null) {
            recyclerDocPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
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

        if (permit.getLatitude() != null && permit.getLongitude() != null) {
            cardMap.setVisibility(View.VISIBLE);
            setupReviewMap(permit.getLatitude(), permit.getLongitude());
            tvMapCoords.setText(String.format(Locale.US, "%.5f, %.5f", permit.getLatitude(), permit.getLongitude()));
        }

        if (permit.getDocuments() != null && !permit.getDocuments().isEmpty()) {
            cardDocuments.setVisibility(View.VISIBLE);
            StringBuilder docs = new StringBuilder();
            for (Document doc : permit.getDocuments()) {
                String label = doc.getDocumentLabel();
                if (label != null && !label.isEmpty()) {
                    docs.append("\u2022 ").append(label).append(" — ").append(doc.getFileName()).append("\n");
                } else {
                    docs.append("\u2022 ").append(doc.getFileName()).append("\n");
                }
            }
            tvDocuments.setText(docs.toString().trim());

            if (documentPreviewContainer != null) {
                documentPreviewContainer.setVisibility(View.VISIBLE);
            }
            if (recyclerDocPreview != null) {
                recyclerDocPreview.setVisibility(View.VISIBLE);
                recyclerDocPreview.setAdapter(new DocCarouselAdapter(permit.getDocuments()));
            }
        }
    }

    private void setupReviewMap(double lat, double lng) {
        mapViewReview.setTileSource(TileSourceFactory.MAPNIK);
        mapViewReview.setMultiTouchControls(true);
        IMapController controller = mapViewReview.getController();
        controller.setZoom(15.0);
        GeoPoint point = new GeoPoint(lat, lng);
        controller.setCenter(point);
        Marker marker = new Marker(mapViewReview);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Work Location");
        mapViewReview.getOverlays().add(marker);
        mapViewReview.invalidate();
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

    @Override
    protected void onResume() {
        super.onResume();
        if (mapViewReview != null) mapViewReview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapViewReview != null) mapViewReview.onPause();
    }

    private class DocCarouselAdapter extends RecyclerView.Adapter<DocCarouselAdapter.VH> {
        private final List<Document> docs;
        DocCarouselAdapter(List<Document> docs) { this.docs = docs; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(8, 12, 8, 12);
            row.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));
            return new VH(row);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Document doc = docs.get(position);
            LinearLayout row = (LinearLayout) holder.itemView;
            row.removeAllViews();

            ImageView iv = new ImageView(PermitReviewActivity.this);
            int size = (int) (72 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(size, size);
            imgParams.setMarginEnd(16);
            iv.setLayoutParams(imgParams);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            String url = RetrofitClient.getInstance(PermitReviewActivity.this).getBaseUrl() + "uploads/" + doc.getFileName();
            Glide.with(PermitReviewActivity.this).load(url).centerCrop().into(iv);
            row.addView(iv);

            LinearLayout textCol = new LinearLayout(PermitReviewActivity.this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvNum = new TextView(PermitReviewActivity.this);
            String label = doc.getDocumentLabel();
            if (label != null && !label.isEmpty()) {
                tvNum.setText("#" + (position + 1) + " — " + label);
            } else {
                tvNum.setText("#" + (position + 1) + " — Document");
            }
            tvNum.setTextSize(14);
            tvNum.setTextColor(getResources().getColor(R.color.text_primary, null));
            tvNum.setTypeface(null, android.graphics.Typeface.BOLD);
            textCol.addView(tvNum);

            TextView tvFile = new TextView(PermitReviewActivity.this);
            tvFile.setText(doc.getFileName());
            tvFile.setTextSize(11);
            tvFile.setTextColor(getResources().getColor(R.color.text_secondary, null));
            tvFile.setMaxLines(1);
            tvFile.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            textCol.addView(tvFile);

            row.addView(textCol);

            row.setOnClickListener(v -> {
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
