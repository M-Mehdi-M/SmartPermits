package project.smartpermits;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
import project.smartpermits.models.AiAnalysisResponse;
import project.smartpermits.models.Document;
import project.smartpermits.models.Permit;
import project.smartpermits.models.ReviewRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PermitReviewActivity extends AppCompatActivity {

    private TextView tvApplicant, tvPermitType, tvDate, tvFee, tvDescription, tvDocuments, tvAiAnalysis;
    private TextInputEditText etNotes;
    private MaterialButton btnApprove, btnReject, btnComments, btnRunAi;
    private MaterialCardView cardDescription, cardDocuments, cardMap, cardAiAnalysis, cardBlockchainReview;
    private ProgressBar progressBar, progressAi;
    private RecyclerView recyclerDocPreview;
    private LinearLayout documentPreviewContainer;
    private MapView mapViewReview;
    private TextView tvMapCoords;
    private int permitId;
    private CharSequence fullAiText = null;

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
        cardAiAnalysis = findViewById(R.id.cardAiAnalysis);
        tvAiAnalysis = findViewById(R.id.tvAiAnalysis);
        cardBlockchainReview = findViewById(R.id.cardBlockchainReview);
        progressBar = findViewById(R.id.progressBar);
        recyclerDocPreview = findViewById(R.id.recyclerDocPreview);
        documentPreviewContainer = findViewById(R.id.documentPreviewContainer);
        mapViewReview = findViewById(R.id.mapViewReview);
        tvMapCoords = findViewById(R.id.tvMapCoords);
        btnRunAi = findViewById(R.id.btnRunAi);
        progressAi = findViewById(R.id.progressAi);
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

        if (btnRunAi != null) {
            btnRunAi.setOnClickListener(v -> triggerAiAnalysis());
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

        String aiAnalysis = permit.getAiAnalysis();
        boolean isError = aiAnalysis != null && (aiAnalysis.startsWith("AI analysis unavailable") || aiAnalysis.startsWith("AI analysis failed"));
        if (aiAnalysis != null && !aiAnalysis.isEmpty() && !isError) {
            cardAiAnalysis.setVisibility(View.VISIBLE);
            fullAiText = formatMarkdown(aiAnalysis);
            tvAiAnalysis.setText(fullAiText);
            tvAiAnalysis.setMaxLines(4);
            tvAiAnalysis.setEllipsize(TextUtils.TruncateAt.END);
            cardAiAnalysis.setOnClickListener(v -> showAiAnalysisDialog());
            if (btnRunAi != null) btnRunAi.setVisibility(View.GONE);
        } else {
            cardAiAnalysis.setVisibility(View.VISIBLE);
            tvAiAnalysis.setText("Analyzing documents with AI...");
            if (btnRunAi != null) btnRunAi.setVisibility(View.GONE);
            if (progressAi != null) progressAi.setVisibility(View.VISIBLE);
            triggerAiAnalysis();
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

        if (cardBlockchainReview != null && "submitted".equals(permit.getStatus())) {
            cardBlockchainReview.setVisibility(View.VISIBLE);
        }
    }

    private void setupReviewMap(double lat, double lng) {
        mapViewReview.setTileSource(TileSourceFactory.MAPNIK);
        mapViewReview.setMultiTouchControls(true);
        mapViewReview.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        mapViewReview.getOverlays().clear();
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

    private void showAiAnalysisDialog() {
        if (fullAiText == null) return;
        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(48, 32, 48, 32);
        TextView tv = new TextView(this);
        tv.setText(fullAiText);
        tv.setTextSize(14);
        tv.setLineSpacing(0, 1.4f);
        tv.setTextColor(getResources().getColor(R.color.text_primary, null));
        scrollView.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle("AI Document Analysis")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .show();
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

    private void triggerAiAnalysis() {
        if (btnRunAi != null) btnRunAi.setEnabled(false);
        cardAiAnalysis.setVisibility(View.VISIBLE);
        tvAiAnalysis.setText("Analyzing documents with AI...");
        if (progressAi != null) progressAi.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance(this).getApi()
                .triggerAiAnalysis(permitId)
                .enqueue(new Callback<AiAnalysisResponse>() {
                    @Override
                    public void onResponse(Call<AiAnalysisResponse> call, Response<AiAnalysisResponse> response) {
                        if (progressAi != null) progressAi.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null && response.body().getAiAnalysis() != null) {
                            fullAiText = formatMarkdown(response.body().getAiAnalysis());
                            tvAiAnalysis.setText(fullAiText);
                            tvAiAnalysis.setMaxLines(4);
                            tvAiAnalysis.setEllipsize(TextUtils.TruncateAt.END);
                            cardAiAnalysis.setOnClickListener(v -> showAiAnalysisDialog());
                            if (btnRunAi != null) btnRunAi.setVisibility(View.GONE);
                        } else {
                            tvAiAnalysis.setText("AI analysis failed. Try again later.");
                            if (btnRunAi != null) btnRunAi.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<AiAnalysisResponse> call, Throwable t) {
                        if (progressAi != null) progressAi.setVisibility(View.GONE);
                        tvAiAnalysis.setText("AI analysis error: " + t.getMessage());
                        if (btnRunAi != null) btnRunAi.setEnabled(true);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapViewReview != null) mapViewReview.onDetach();
    }

    private SpannableStringBuilder formatMarkdown(String raw) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        if (raw == null || raw.isEmpty()) return sb;

        String[] lines = raw.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            boolean isHeader = false;
            float headerScale = 1.0f;
            if (line.startsWith("### ")) {
                line = line.substring(4);
                isHeader = true;
                headerScale = 1.1f;
            } else if (line.startsWith("## ")) {
                line = line.substring(3);
                isHeader = true;
                headerScale = 1.2f;
            } else if (line.startsWith("# ")) {
                line = line.substring(2);
                isHeader = true;
                headerScale = 1.3f;
            }

            if (line.startsWith("- ") || line.startsWith("* ")) {
                line = "  \u2022 " + line.substring(2);
            }

            SpannableStringBuilder lineSb = new SpannableStringBuilder();
            int pos = 0;
            while (pos < line.length()) {
                int boldStart = line.indexOf("**", pos);
                if (boldStart == -1) {
                    String segment = line.substring(pos);
                    lineSb.append(processItalic(segment));
                    break;
                }
                if (boldStart > pos) {
                    lineSb.append(processItalic(line.substring(pos, boldStart)));
                }
                int boldEnd = line.indexOf("**", boldStart + 2);
                if (boldEnd == -1) {
                    lineSb.append(processItalic(line.substring(boldStart)));
                    break;
                }
                String boldText = line.substring(boldStart + 2, boldEnd);
                int start = lineSb.length();
                lineSb.append(processItalic(boldText));
                lineSb.setSpan(new StyleSpan(Typeface.BOLD), start, lineSb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                pos = boldEnd + 2;
            }

            if (isHeader) {
                int start = sb.length();
                sb.append(lineSb);
                sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new RelativeSizeSpan(headerScale), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                sb.append(lineSb);
            }

            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        return sb;
    }

    private SpannableStringBuilder processItalic(String text) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int pos = 0;
        while (pos < text.length()) {
            int italicStart = text.indexOf("*", pos);
            if (italicStart == -1) {
                sb.append(text.substring(pos));
                break;
            }
            if (italicStart > pos) {
                sb.append(text.substring(pos, italicStart));
            }
            int italicEnd = text.indexOf("*", italicStart + 1);
            if (italicEnd == -1) {
                sb.append(text.substring(italicStart));
                break;
            }
            String italicText = text.substring(italicStart + 1, italicEnd);
            int start = sb.length();
            sb.append(italicText);
            sb.setSpan(new StyleSpan(Typeface.ITALIC), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos = italicEnd + 1;
        }
        return sb;
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
            String url = RetrofitClient.getInstance(row.getContext()).getBaseUrl() + "uploads/" + doc.getFileName();
            Glide.with(row.getContext()).load(url).centerCrop().into(iv);
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
