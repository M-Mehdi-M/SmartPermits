package project.smartpermits;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.AiAnalysisResponse;
import project.smartpermits.models.Document;
import project.smartpermits.models.Permit;
import project.smartpermits.models.PermitRequest;
import project.smartpermits.models.PermitType;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApplyPermitActivity extends AppCompatActivity {

    private ViewFlipper viewFlipper;
    private Spinner spinnerPermitType;
    private TextInputEditText etDescription;
    private TextView tvFeePreview;
    private ProgressBar progressBar;
    private LinearLayout mapContainer;
    private MapView mapView;
    private TextView tvLocationInfo;
    private MaterialButton btnPinHere;
    private LinearLayout documentChecklistContainer;
    private LinearLayout checklistItems;

    private List<PermitType> permitTypes;
    private String selectedType = "";
    private double selectedFee = 0;
    private int createdPermitId = -1;

    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private Marker currentMarker = null;

    private final Map<String, File> requiredDocumentFiles = new LinkedHashMap<>();
    private final Map<String, CheckBox> requiredDocumentChecks = new LinkedHashMap<>();
    private String pendingDocLabel = null;

    private static final Map<String, String[]> REQUIRED_DOCUMENTS = new LinkedHashMap<>();
    static {
        REQUIRED_DOCUMENTS.put("Construction Permit", new String[]{
                "Certificat de urbanism",
                "Extras carte funciară (CF)",
                "Plan topografic vizat de OCPI",
                "Proiect tehnic (DTAC) autorizat",
                "Avize utilități (apă, gaz, electricitate)",
                "Studiu geotehnic",
                "Dovada achitării taxei"
        });
        REQUIRED_DOCUMENTS.put("Renovation Permit", new String[]{
                "Certificat de urbanism",
                "Releveu stare existentă",
                "Proiect tehnic renovare",
                "Acord asociație proprietari (dacă e cazul)",
                "Avize utilități afectate",
                "Dovada achitării taxei"
        });
        REQUIRED_DOCUMENTS.put("Business License", new String[]{
                "Certificat înregistrare ORC (Registrul Comerțului)",
                "Act constitutiv societate",
                "Contract spațiu / sediu social",
                "Aviz PSI / ISU",
                "Cazier fiscal",
                "Certificat constatator ORC"
        });
        REQUIRED_DOCUMENTS.put("Food Service Permit", new String[]{
                "Autorizație sanitară veterinară (DSVSA)",
                "Plan HACCP",
                "Contract dezinsecție și deratizare",
                "Aviz de mediu",
                "Certificat înregistrare ORC",
                "Buletin analiză apă"
        });
        REQUIRED_DOCUMENTS.put("Event Permit", new String[]{
                "Cerere organizare eveniment",
                "Plan de securitate",
                "Aviz Poliție",
                "Aviz ISU (pompieri)",
                "Contract salubrizare",
                "Poliță asigurare răspundere civilă"
        });
        REQUIRED_DOCUMENTS.put("Signage Permit", new String[]{
                "Cerere amplasare firmă",
                "Schița amplasament",
                "Aviz urbanism / arhitectură",
                "Acord proprietar imobil",
                "Simulare foto montaj"
        });
        REQUIRED_DOCUMENTS.put("Demolition Permit", new String[]{
                "Certificat de urbanism",
                "Extras carte funciară (CF)",
                "Proiect tehnic desființare (DTAD)",
                "Plan de demolare",
                "Aviz de mediu",
                "Studiu privind gestionarea deșeurilor",
                "Dovada achitării taxei"
        });
        REQUIRED_DOCUMENTS.put("Occupancy Certificate", new String[]{
                "Proces verbal recepție la terminarea lucrărilor",
                "Certificat de performanță energetică",
                "Documentație cadastrală",
                "Referatele verificatorilor de proiecte",
                "Declarație conformitate instalații",
                "Dovada achitării taxei"
        });
    }


    private final ActivityResultLauncher<String> requiredDocLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && pendingDocLabel != null) {
                    try {
                        File file = copyUriToFile(uri);
                        requiredDocumentFiles.put(pendingDocLabel, file);
                        CheckBox cb = requiredDocumentChecks.get(pendingDocLabel);
                        if (cb != null) {
                            cb.setChecked(true);
                            cb.setText(pendingDocLabel + "  ✅");
                        }
                        Toast.makeText(this, pendingDocLabel + " uploaded", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show();
                    }
                    pendingDocLabel = null;
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid_prefs", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_apply_permit);

        viewFlipper = findViewById(R.id.viewFlipper);
        spinnerPermitType = findViewById(R.id.spinnerPermitType);
        etDescription = findViewById(R.id.etDescription);
        tvFeePreview = findViewById(R.id.tvFeePreview);
        progressBar = findViewById(R.id.progressBar);
        mapContainer = findViewById(R.id.mapContainer);
        mapView = findViewById(R.id.mapView);
        tvLocationInfo = findViewById(R.id.tvLocationInfo);
        btnPinHere = findViewById(R.id.btnPinHere);
        documentChecklistContainer = findViewById(R.id.documentChecklistContainer);
        checklistItems = findViewById(R.id.checklistItems);

        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnSubmit = findViewById(R.id.btnNext1);
        MaterialButton btnBackToDashboard = findViewById(R.id.btnBackToDashboard);

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitApplication());
        btnBackToDashboard.setOnClickListener(v -> finish());

        setupMap();
        loadPermitTypes();
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        IMapController controller = mapView.getController();
        controller.setZoom(7.0);
        GeoPoint romaniaCenter = new GeoPoint(45.9432, 24.9668);
        controller.setCenter(romaniaCenter);

        mapView.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        btnPinHere.setOnClickListener(v -> {
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            selectedLatitude = center.getLatitude();
            selectedLongitude = center.getLongitude();
            if (currentMarker != null) {
                mapView.getOverlays().remove(currentMarker);
            }
            currentMarker = new Marker(mapView);
            currentMarker.setPosition(center);
            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            currentMarker.setTitle("Work Location");
            mapView.getOverlays().add(currentMarker);
            mapView.invalidate();
            tvLocationInfo.setText(String.format(Locale.US, "\uD83D\uDCCD %.5f, %.5f", selectedLatitude, selectedLongitude));
            Toast.makeText(this, "Location pinned!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadPermitTypes() {
        RetrofitClient.getInstance(this).getApi().getPermitTypes()
                .enqueue(new Callback<List<PermitType>>() {
                    @Override
                    public void onResponse(Call<List<PermitType>> call, Response<List<PermitType>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            permitTypes = response.body();
                            String[] names = new String[permitTypes.size()];
                            for (int i = 0; i < permitTypes.size(); i++) {
                                names[i] = permitTypes.get(i).getName() + " - $" + String.format("%.0f", permitTypes.get(i).getFee());
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    ApplyPermitActivity.this,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    names
                            );
                            spinnerPermitType.setAdapter(adapter);
                            spinnerPermitType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    selectedType = permitTypes.get(position).getName();
                                    selectedFee = permitTypes.get(position).getFee();
                                    tvFeePreview.setText(String.format("Estimated fee: $%.2f", selectedFee));
                                    onPermitTypeChanged(selectedType);
                                }
                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<List<PermitType>> call, Throwable t) {
                        String[] defaultTypes = {"Business License", "Construction Permit", "Food Service Permit",
                                "Signage Permit", "Event Permit", "Renovation Permit"};
                        spinnerPermitType.setAdapter(new ArrayAdapter<>(
                                ApplyPermitActivity.this,
                                android.R.layout.simple_spinner_dropdown_item,
                                defaultTypes
                        ));
                        spinnerPermitType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                selectedType = defaultTypes[position];
                                onPermitTypeChanged(selectedType);
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                });
    }

    private void onPermitTypeChanged(String type) {
        boolean showMap = "Construction Permit".equals(type) || "Renovation Permit".equals(type);
        mapContainer.setVisibility(showMap ? View.VISIBLE : View.GONE);
        if (!showMap) {
            selectedLatitude = null;
            selectedLongitude = null;
            if (currentMarker != null) {
                mapView.getOverlays().remove(currentMarker);
                currentMarker = null;
                mapView.invalidate();
            }
            tvLocationInfo.setText("Move the map, then press Pin Here");
        }

        requiredDocumentFiles.clear();
        requiredDocumentChecks.clear();
        checklistItems.removeAllViews();

        String[] docs = REQUIRED_DOCUMENTS.get(type);
        if (docs != null && docs.length > 0) {
            documentChecklistContainer.setVisibility(View.VISIBLE);
            for (String docName : docs) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 8, 0, 8);

                CheckBox cb = new CheckBox(this);
                cb.setText(docName);
                cb.setEnabled(false);
                cb.setTextSize(13);
                LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                cb.setLayoutParams(cbParams);
                requiredDocumentChecks.put(docName, cb);

                MaterialButton uploadBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                uploadBtn.setText("Upload");
                uploadBtn.setTextSize(11);
                uploadBtn.setCornerRadius(24);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                uploadBtn.setLayoutParams(btnParams);
                uploadBtn.setOnClickListener(v -> {
                    pendingDocLabel = docName;
                    requiredDocLauncher.launch("*/*");
                });

                row.addView(cb);
                row.addView(uploadBtn);
                checklistItems.addView(row);
            }
        } else {
            documentChecklistContainer.setVisibility(View.GONE);
        }
    }

    private File copyUriToFile(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("upload_", ".jpg", getCacheDir());
        FileOutputStream fos = new FileOutputStream(tempFile);
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            fos.write(buffer, 0, len);
        }
        fos.close();
        inputStream.close();
        return tempFile;
    }

    private void submitApplication() {
        if (selectedType.isEmpty()) {
            Toast.makeText(this, "Please select a permit type", Toast.LENGTH_SHORT).show();
            return;
        }
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        progressBar.setVisibility(View.VISIBLE);

        PermitRequest permitRequest;
        if (selectedLatitude != null && selectedLongitude != null) {
            permitRequest = new PermitRequest(selectedType, description, selectedLatitude, selectedLongitude);
        } else {
            permitRequest = new PermitRequest(selectedType, description);
        }

        RetrofitClient.getInstance(this).getApi()
                .createPermit(permitRequest)
                .enqueue(new Callback<Permit>() {
                    @Override
                    public void onResponse(Call<Permit> call, Response<Permit> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            createdPermitId = response.body().getId();
                            uploadAllRequiredDocuments(createdPermitId, new ArrayList<>(requiredDocumentFiles.keySet()), 0);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            if (response.code() == 401 || response.code() == 422) {
                                Toast.makeText(ApplyPermitActivity.this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
                                RetrofitClient.getInstance(ApplyPermitActivity.this).clearSession();
                                Intent loginIntent = new Intent(ApplyPermitActivity.this, LoginActivity.class);
                                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(loginIntent);
                            } else {
                                String errorMsg = "Failed to create permit";
                                try {
                                    if (response.errorBody() != null) {
                                        errorMsg += ": " + response.errorBody().string();
                                    }
                                } catch (Exception ignored) {}
                                Toast.makeText(ApplyPermitActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Permit> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ApplyPermitActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadAllRequiredDocuments(int permitId, List<String> labels, int index) {
        if (index >= labels.size()) {
            onSubmitSuccess();
            return;
        }
        String label = labels.get(index);
        File file = requiredDocumentFiles.get(label);
        if (file == null || !file.exists()) {
            uploadAllRequiredDocuments(permitId, labels, index + 1);
            return;
        }
        RequestBody requestFile = RequestBody.create(MediaType.parse("application/octet-stream"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        RequestBody labelBody = RequestBody.create(MediaType.parse("text/plain"), label);
        RetrofitClient.getInstance(this).getApi()
                .uploadDocument(permitId, body, labelBody)
                .enqueue(new Callback<Document>() {
                    @Override
                    public void onResponse(Call<Document> call, Response<Document> response) {
                        uploadAllRequiredDocuments(permitId, labels, index + 1);
                    }
                    @Override
                    public void onFailure(Call<Document> call, Throwable t) {
                        uploadAllRequiredDocuments(permitId, labels, index + 1);
                    }
                });
    }


    private void onSubmitSuccess() {
        if (createdPermitId > 0) {
            RetrofitClient.getInstance(this).getApi()
                    .triggerAiAnalysis(createdPermitId)
                    .enqueue(new Callback<AiAnalysisResponse>() {
                        @Override
                        public void onResponse(Call<AiAnalysisResponse> call, Response<AiAnalysisResponse> response) {
                            progressBar.setVisibility(View.GONE);
                            viewFlipper.setDisplayedChild(1);
                        }
                        @Override
                        public void onFailure(Call<AiAnalysisResponse> call, Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            viewFlipper.setDisplayedChild(1);
                        }
                    });
        } else {
            progressBar.setVisibility(View.GONE);
            viewFlipper.setDisplayedChild(1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
