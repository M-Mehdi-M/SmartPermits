package project.smartpermits;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import com.google.android.material.chip.Chip;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.Document;
import project.smartpermits.models.Permit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PermitDetailActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(project.smartpermits.LocaleHelper.applyLocale(newBase));
    }

    private TextView tvPermitType, tvIcon, tvDate, tvFee, tvPayment, tvDescription, tvNotes, tvEstTime;
    private Chip chipStatus;
    private MaterialButton btnPay, btnRenew, btnCertificate, btnComments, btnSchedule, btnTrash;
    private MaterialCardView cardDescription, cardNotes, cardDocuments, cardEstTime, cardBlockchain, cardTimeline, cardExpiry;
    private RecyclerView recyclerDocuments;
    private ProgressBar progressBar;
    private MaterialCardView cardMapDetail;
    private MapView mapViewDetail;
    private TextView tvMapCoordsDetail;
    private TextView tvBlockchainStatus, tvTxHash, tvDocHash;
    private LinearLayout layoutTxHash, layoutDocHash;
    private MaterialButton btnViewOnChain;
    private LinearLayout timelineContainer;
    private TextView tvExpiryInfo;
    private ProgressBar progressExpiry;
    private int permitId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_permit_detail);

        tvPermitType = findViewById(R.id.tvPermitType);
        tvIcon = findViewById(R.id.tvIcon);
        tvDate = findViewById(R.id.tvDate);
        tvFee = findViewById(R.id.tvFee);
        tvPayment = findViewById(R.id.tvPayment);
        tvDescription = findViewById(R.id.tvDescription);
        tvNotes = findViewById(R.id.tvNotes);
        tvEstTime = findViewById(R.id.tvEstTime);
        chipStatus = findViewById(R.id.chipStatus);
        btnPay = findViewById(R.id.btnPay);
        btnRenew = findViewById(R.id.btnRenew);
        btnCertificate = findViewById(R.id.btnCertificate);
        btnComments = findViewById(R.id.btnComments);
        btnSchedule = findViewById(R.id.btnSchedule);
        btnTrash = findViewById(R.id.btnTrash);
        cardDescription = findViewById(R.id.cardDescription);
        cardNotes = findViewById(R.id.cardNotes);
        cardDocuments = findViewById(R.id.cardDocuments);
        cardEstTime = findViewById(R.id.cardEstTime);
        recyclerDocuments = findViewById(R.id.recyclerDocuments);
        progressBar = findViewById(R.id.progressBar);
        cardMapDetail = findViewById(R.id.cardMapDetail);
        mapViewDetail = findViewById(R.id.mapViewDetail);
        tvMapCoordsDetail = findViewById(R.id.tvMapCoordsDetail);
        cardBlockchain = findViewById(R.id.cardBlockchain);
        tvBlockchainStatus = findViewById(R.id.tvBlockchainStatus);
        tvTxHash = findViewById(R.id.tvTxHash);
        tvDocHash = findViewById(R.id.tvDocHash);
        layoutTxHash = findViewById(R.id.layoutTxHash);
        layoutDocHash = findViewById(R.id.layoutDocHash);
        btnViewOnChain = findViewById(R.id.btnViewOnChain);
        cardTimeline = findViewById(R.id.cardTimeline);
        timelineContainer = findViewById(R.id.timelineContainer);
        cardExpiry = findViewById(R.id.cardExpiry);
        tvExpiryInfo = findViewById(R.id.tvExpiryInfo);
        progressExpiry = findViewById(R.id.progressExpiry);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        permitId = getIntent().getIntExtra("permit_id", -1);
        if (permitId == -1) {
            finish();
            return;
        }

        recyclerDocuments.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        btnComments.setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("permit_id", permitId);
            startActivity(intent);
        });

        loadPermit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapViewDetail != null) mapViewDetail.onResume();
        if (permitId != -1) loadPermit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapViewDetail != null) mapViewDetail.onPause();
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
                        Toast.makeText(PermitDetailActivity.this, getString(R.string.error_loading), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayPermit(Permit permit) {
        tvPermitType.setText(permit.getPermitType() != null ? PermitTypeHelper.localizeType(this, permit.getPermitType()) : getString(R.string.unknown));

        String type = permit.getPermitType() != null ? permit.getPermitType() : "";
        String icon;
        switch (type) {
            case "Construction Permit": icon = "\uD83C\uDFD7"; break;
            case "Business License": icon = "\uD83C\uDFE2"; break;
            case "Food Service Permit": icon = "\uD83C\uDF7D"; break;
            case "Signage Permit": icon = "\uD83E\uDEA7"; break;
            case "Event Permit": icon = "\uD83C\uDFEA"; break;
            case "Renovation Permit": icon = "\uD83D\uDD28"; break;
            case "Demolition Permit": icon = "\uD83D\uDCA5"; break;
            case "Occupancy Certificate": icon = "\uD83C\uDFE0"; break;
            default: icon = "\uD83D\uDCC4"; break;
        }
        tvIcon.setText(icon);

        String date = permit.getCreatedAt();
        if (date != null && date.length() >= 10) date = date.substring(0, 10);
        tvDate.setText(date);

        tvFee.setText(CurrencyHelper.format(this, permit.getFeeAmount()));
        tvPayment.setText(permit.isPaid() ? getString(R.string.paid) : getString(R.string.unpaid));

        cardDescription.setVisibility(View.GONE);
        cardNotes.setVisibility(View.GONE);
        cardEstTime.setVisibility(View.GONE);
        cardMapDetail.setVisibility(View.GONE);
        cardDocuments.setVisibility(View.GONE);
        cardBlockchain.setVisibility(View.GONE);
        cardTimeline.setVisibility(View.GONE);
        cardExpiry.setVisibility(View.GONE);

        if (permit.getDescription() != null && !permit.getDescription().isEmpty()) {
            cardDescription.setVisibility(View.VISIBLE);
            tvDescription.setText(permit.getDescription());
        }

        if (permit.getReviewerNotes() != null && !permit.getReviewerNotes().isEmpty()) {
            cardNotes.setVisibility(View.VISIBLE);
            tvNotes.setText(permit.getReviewerNotes());
        }

        String estTime = permit.getEstimatedProcessingTime();
        if (estTime != null && !estTime.isEmpty() && "submitted".equals(permit.getStatus())) {
            cardEstTime.setVisibility(View.VISIBLE);
            String estDisplay = estTime;
            if (permit.getPredictionConfidence() != null) {
                estDisplay += "  (" + permit.getPredictionConfidence() + "% " + getString(R.string.confidence) + ")";
            }
            tvEstTime.setText(estDisplay);
        }

        if (permit.getExpiresAt() != null && !permit.getExpiresAt().isEmpty()) {
            cardExpiry.setVisibility(View.VISIBLE);
            Integer daysToExpiry = permit.getDaysToExpiry();
            if (permit.isExpired()) {
                tvExpiryInfo.setText(getString(R.string.expired));
                tvExpiryInfo.setTextColor(Color.parseColor("#EF4444"));
            } else if (daysToExpiry != null) {
                if (daysToExpiry <= 30) {
                    tvExpiryInfo.setText(String.format(getString(R.string.expires_in), daysToExpiry));
                    tvExpiryInfo.setTextColor(Color.parseColor("#F59E0B"));
                } else {
                    String expiryDate = permit.getExpiresAt();
                    if (expiryDate.length() >= 10) expiryDate = expiryDate.substring(0, 10);
                    tvExpiryInfo.setText(String.format(getString(R.string.valid_until), expiryDate));
                    tvExpiryInfo.setTextColor(Color.parseColor("#10B981"));
                }
            }
        }

        if (permit.getTimeline() != null && !permit.getTimeline().isEmpty()) {
            cardTimeline.setVisibility(View.VISIBLE);
            timelineContainer.removeAllViews();
            java.util.List<project.smartpermits.models.PermitEvent> events = permit.getTimeline();
            for (int i = 0; i < events.size(); i++) {
                project.smartpermits.models.PermitEvent event = events.get(i);
                boolean isLast = (i == events.size() - 1);
                addTimelineEntry(event, isLast);
            }
        }

        if (permit.getLatitude() != null && permit.getLongitude() != null) {
            cardMapDetail.setVisibility(View.VISIBLE);
            mapViewDetail.setTileSource(TileSourceFactory.MAPNIK);
            mapViewDetail.setMultiTouchControls(true);
            mapViewDetail.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            });
            mapViewDetail.getOverlays().clear();
            IMapController controller = mapViewDetail.getController();
            controller.setZoom(15.0);
            GeoPoint point = new GeoPoint(permit.getLatitude(), permit.getLongitude());
            controller.setCenter(point);
            Marker marker = new Marker(mapViewDetail);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(getString(R.string.work_location));
            mapViewDetail.getOverlays().add(marker);
            mapViewDetail.invalidate();
            tvMapCoordsDetail.setText(String.format(Locale.US, "%.5f, %.5f", permit.getLatitude(), permit.getLongitude()));
        }

        if (permit.getDocuments() != null && !permit.getDocuments().isEmpty()) {
            cardDocuments.setVisibility(View.VISIBLE);
            recyclerDocuments.setAdapter(new DocCarouselAdapter(permit.getDocuments()));
        }

        String bcHash = permit.getBlockchainHash();
        String bcTxHash = permit.getBlockchainTxHash();
        if (bcHash != null && !bcHash.isEmpty()) {
            cardBlockchain.setVisibility(View.VISIBLE);
            if (bcTxHash != null && !bcTxHash.isEmpty()) {
                tvBlockchainStatus.setText(getString(R.string.verified_blockchain));
                tvBlockchainStatus.setTextColor(Color.parseColor("#10B981"));
                layoutTxHash.setVisibility(View.VISIBLE);
                tvTxHash.setText(bcTxHash);
                btnViewOnChain.setVisibility(View.VISIBLE);
                btnViewOnChain.setOnClickListener(v -> {
                    String url = "https://sepolia.etherscan.io/tx/" + bcTxHash;
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                });
            } else {
                tvBlockchainStatus.setText(getString(R.string.hash_local));
                tvBlockchainStatus.setTextColor(Color.parseColor("#F59E0B"));
            }
            layoutDocHash.setVisibility(View.VISIBLE);
            tvDocHash.setText(bcHash);
        }

        String status = permit.getStatus() != null ? permit.getStatus() : "unknown";
        chipStatus.setText(PermitTypeHelper.localizeStatus(this, status));

        int chipColor;
        switch (status) {
            case "submitted": chipColor = Color.parseColor("#F59E0B"); break;
            case "approved": chipColor = Color.parseColor("#10B981"); break;
            case "rejected": chipColor = Color.parseColor("#EF4444"); break;
            case "completed": chipColor = Color.parseColor("#0D9488"); break;
            default: chipColor = Color.parseColor("#64748B"); break;
        }
        chipStatus.setChipBackgroundColor(ColorStateList.valueOf(chipColor));
        chipStatus.setTextColor(Color.WHITE);

        String role = RetrofitClient.getInstance(this).getUserRole();

        btnPay.setVisibility(View.GONE);
        btnRenew.setVisibility(View.GONE);
        btnCertificate.setVisibility(View.GONE);
        btnSchedule.setVisibility(View.GONE);

        if ("approved".equals(status) && !permit.isPaid() && "citizen".equals(role)) {
            btnPay.setVisibility(View.VISIBLE);
            btnPay.setOnClickListener(v -> payPermit());
        }

        if (("completed".equals(status) || "rejected".equals(status)) && "citizen".equals(role)) {
            btnRenew.setVisibility(View.VISIBLE);
            btnRenew.setText("rejected".equals(status) ? getString(R.string.reapply) : getString(R.string.renew));
            btnRenew.setOnClickListener(v -> renewPermit());
        }

        if ("completed".equals(status)) {
            btnCertificate.setVisibility(View.VISIBLE);
            btnCertificate.setOnClickListener(v -> downloadCertificate());
        }

        if ("approved".equals(status) && "citizen".equals(role)) {
            btnSchedule.setVisibility(View.VISIBLE);
            btnSchedule.setOnClickListener(v -> {
                Intent intent = new Intent(this, ScheduleAppointmentActivity.class);
                intent.putExtra("permit_id", permitId);
                startActivity(intent);
            });
        }

        btnComments.setVisibility(View.VISIBLE);

        if ("citizen".equals(role)) {
            btnTrash.setVisibility(View.VISIBLE);
            btnTrash.setOnClickListener(v -> confirmTrash());
        }
    }

    private void confirmTrash() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.move_to_trash))
                .setMessage(getString(R.string.trash_confirm_msg))
                .setPositiveButton(getString(R.string.move_to_trash), (d, w) -> trashPermit())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void trashPermit() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().trashPermit(permitId)
                .enqueue(new Callback<project.smartpermits.models.MessageResponse>() {
                    @Override
                    public void onResponse(Call<project.smartpermits.models.MessageResponse> call, Response<project.smartpermits.models.MessageResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(PermitDetailActivity.this, getString(R.string.moved_to_trash), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(PermitDetailActivity.this, getString(R.string.failed_to_delete), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<project.smartpermits.models.MessageResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PermitDetailActivity.this, getString(R.string.error_generic, t.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void payPermit() {
        progressBar.setVisibility(View.VISIBLE);
        btnPay.setEnabled(false);
        RetrofitClient.getInstance(this).getApi().payPermit(permitId)
                .enqueue(new Callback<Permit>() {
                    @Override
                    public void onResponse(Call<Permit> call, Response<Permit> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(PermitDetailActivity.this, getString(R.string.payment_success), Toast.LENGTH_LONG).show();
                            displayPermit(response.body());
                        } else {
                            btnPay.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<Permit> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnPay.setEnabled(true);
                    }
                });
    }

    private void renewPermit() {
        progressBar.setVisibility(View.VISIBLE);
        btnRenew.setEnabled(false);
        RetrofitClient.getInstance(this).getApi().renewPermit(permitId)
                .enqueue(new Callback<Permit>() {
                    @Override
                    public void onResponse(Call<Permit> call, Response<Permit> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(PermitDetailActivity.this, getString(R.string.new_app_created), Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(PermitDetailActivity.this, PermitDetailActivity.class);
                            intent.putExtra("permit_id", response.body().getId());
                            startActivity(intent);
                            finish();
                        } else {
                            btnRenew.setEnabled(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<Permit> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnRenew.setEnabled(true);
                    }
                });
    }

    private void downloadCertificate() {
        progressBar.setVisibility(View.VISIBLE);
        btnCertificate.setEnabled(false);
        String currentLang = LocaleHelper.getLanguage(this);
        new Thread(() -> {
            try {
                retrofit2.Response<ResponseBody> response = RetrofitClient.getInstance(this)
                        .getApi().downloadCertificate(permitId, currentLang).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    String errMsg = getString(R.string.download_failed, response.code());
                    try {
                        if (response.errorBody() != null) {
                            errMsg = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    String finalMsg = errMsg;
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnCertificate.setEnabled(true);
                        Toast.makeText(this, finalMsg, Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                byte[] pdfBytes = response.body().bytes();
                if (pdfBytes == null || pdfBytes.length < 50) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnCertificate.setEnabled(true);
                        Toast.makeText(this, getString(R.string.empty_response), Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length));
                if (!header.startsWith("%PDF")) {
                    String preview = new String(pdfBytes, 0, Math.min(200, pdfBytes.length));
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnCertificate.setEnabled(true);
                        Toast.makeText(this, getString(R.string.server_error, preview), Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                String fileName = "permit_certificate_" + permitId + ".pdf";
                java.io.File cacheFile = new java.io.File(getCacheDir(), fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(cacheFile)) {
                    fos.write(pdfBytes);
                    fos.flush();
                }
                try {
                    android.content.ContentValues values = new android.content.ContentValues();
                    values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName);
                    values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf");
                    values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    android.net.Uri dlUri = getContentResolver().insert(
                            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (dlUri != null) {
                        try (java.io.OutputStream os = getContentResolver().openOutputStream(dlUri)) {
                            if (os != null) {
                                os.write(pdfBytes);
                                os.flush();
                            }
                        }
                    }
                } catch (Exception ignored) {}
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnCertificate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.cert_saved), Toast.LENGTH_LONG).show();
                    try {
                        android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                                this, getPackageName() + ".fileprovider", cacheFile);
                        Intent openIntent = new Intent(Intent.ACTION_VIEW);
                        openIntent.setDataAndType(fileUri, "application/pdf");
                        openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(openIntent);
                    } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnCertificate.setEnabled(true);
                    Toast.makeText(this, getString(R.string.download_error, e.toString()), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapViewDetail != null) mapViewDetail.onDetach();
    }

    private void addTimelineEntry(project.smartpermits.models.PermitEvent event, boolean isLast) {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, isLast ? 0 : (int)(4 * density));

        LinearLayout dotColumn = new LinearLayout(this);
        dotColumn.setOrientation(LinearLayout.VERTICAL);
        dotColumn.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        int dotColWidth = (int)(28 * density);
        dotColumn.setLayoutParams(new LinearLayout.LayoutParams(dotColWidth, LinearLayout.LayoutParams.MATCH_PARENT));

        View dot = new View(this);
        int dotSize = (int)(12 * density);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
        dotParams.topMargin = (int)(6 * density);
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(R.drawable.circle_green);
        dotColumn.addView(dot);

        if (!isLast) {
            View line = new View(this);
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams((int)(2 * density), 0, 1f);
            lineParams.topMargin = (int)(4 * density);
            line.setLayoutParams(lineParams);
            line.setBackgroundColor(Color.parseColor("#D1D5DB"));
            dotColumn.addView(line);
        }

        row.addView(dotColumn);

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart((int)(8 * density));
        textColumn.setLayoutParams(textParams);
        textColumn.setPadding(0, 0, 0, (int)(12 * density));

        TextView tvEvent = new TextView(this);
        tvEvent.setText(event.getEventType());
        tvEvent.setTextSize(14);
        tvEvent.setTypeface(tvEvent.getTypeface(), android.graphics.Typeface.BOLD);
        tvEvent.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
        textColumn.addView(tvEvent);

        String actor = event.getActorName();
        if (actor != null && !actor.isEmpty()) {
            TextView tvActor = new TextView(this);
            tvActor.setText(actor);
            tvActor.setTextSize(12);
            tvActor.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            textColumn.addView(tvActor);
        }

        String notes = event.getNotes();
        if (notes != null && !notes.isEmpty()) {
            TextView tvNt = new TextView(this);
            tvNt.setText(notes);
            tvNt.setTextSize(11);
            tvNt.setTextColor(getResources().getColor(R.color.text_secondary, getTheme()));
            textColumn.addView(tvNt);
        }

        String time = event.getCreatedAt();
        if (time != null && time.length() >= 16) {
            time = time.substring(0, 10) + " " + time.substring(11, 16);
        }
        TextView tvTime = new TextView(this);
        tvTime.setText(time);
        tvTime.setTextSize(10);
        tvTime.setTextColor(Color.parseColor("#9CA3AF"));
        textColumn.addView(tvTime);

        row.addView(textColumn);
        timelineContainer.addView(row);
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
            String url = RetrofitClient.getInstance(PermitDetailActivity.this).getBaseUrl() + "uploads/" + doc.getFileName();
            Glide.with(PermitDetailActivity.this).load(url).centerCrop().into((ImageView) holder.itemView);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(PermitDetailActivity.this, DocumentViewerActivity.class);
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
