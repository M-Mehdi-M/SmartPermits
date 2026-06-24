package project.smartpermits;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
import project.smartpermits.models.Document;
import project.smartpermits.models.Permit;
import project.smartpermits.models.PermitRequest;
import project.smartpermits.models.PermitType;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ApplyPermitActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(project.smartpermits.LocaleHelper.applyLocale(newBase));
    }

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
    private TextInputEditText etMapSearch;
    private MaterialButton btnMapSearch;

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

    private String prefillType = null;
    private String prefillDescription = null;



    private final ActivityResultLauncher<String> requiredDocLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && pendingDocLabel != null) {
                    try {
                        File file = copyUriToFile(uri);
                        requiredDocumentFiles.put(pendingDocLabel, file);
                        CheckBox cb = requiredDocumentChecks.get(pendingDocLabel);
                        if (cb != null) {
                            cb.setChecked(true);
                            cb.setText(pendingDocLabel + "  \u2705");
                        }
                        Toast.makeText(this, getString(R.string.doc_uploaded, pendingDocLabel), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, getString(R.string.failed_read_file), Toast.LENGTH_SHORT).show();
                    }
                    pendingDocLabel = null;
                }
                hideKeyboardAndClearFocus();
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
        etMapSearch = findViewById(R.id.etMapSearch);
        btnMapSearch = findViewById(R.id.btnMapSearch);

        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnSubmit = findViewById(R.id.btnNext1);
        MaterialButton btnBackToDashboard = findViewById(R.id.btnBackToDashboard);

        btnBack.setOnClickListener(v -> finish());
        btnSubmit.setOnClickListener(v -> submitApplication());
        btnBackToDashboard.setOnClickListener(v -> finish());

        prefillType = getIntent().getStringExtra(CopilotActivity.EXTRA_PREFILL_TYPE);
        prefillDescription = getIntent().getStringExtra(CopilotActivity.EXTRA_PREFILL_DESCRIPTION);

        setupMap();
        loadPermitTypes();
    }

    private void hideKeyboardAndClearFocus() {
        View focus = getCurrentFocus();
        if (focus != null) {
            focus.clearFocus();
        }
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            View decor = getWindow().getDecorView();
            imm.hideSoftInputFromWindow(decor.getWindowToken(), 0);
        }
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        IMapController controller = mapView.getController();
        controller.setZoom(3.0);
        GeoPoint defaultCenter = new GeoPoint(20.0, 0.0);
        controller.setCenter(defaultCenter);

        mapView.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        if (btnMapSearch != null) {
            btnMapSearch.setOnClickListener(v -> {
                String query = etMapSearch.getText() != null ? etMapSearch.getText().toString().trim() : "";
                if (query.isEmpty()) {
                    Toast.makeText(this, getString(R.string.enter_location), Toast.LENGTH_SHORT).show();
                    return;
                }
                btnMapSearch.setEnabled(false);
                hideKeyboardAndClearFocus();
                new Thread(() -> {
                    try {
                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        java.util.List<Address> results = geocoder.getFromLocationName(query, 1);
                        runOnUiThread(() -> {
                            btnMapSearch.setEnabled(true);
                            if (results != null && !results.isEmpty()) {
                                Address addr = results.get(0);
                                GeoPoint point = new GeoPoint(addr.getLatitude(), addr.getLongitude());
                                mapView.getController().animateTo(point);
                                mapView.getController().setZoom(14.0);
                                Toast.makeText(this, getString(R.string.found_location, addr.getAddressLine(0)), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, getString(R.string.location_not_found), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            btnMapSearch.setEnabled(true);
                            Toast.makeText(this, getString(R.string.search_failed), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            });
        }

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
            currentMarker.setTitle(getString(R.string.work_location));
            mapView.getOverlays().add(currentMarker);
            mapView.invalidate();
            tvLocationInfo.setText(String.format(Locale.US, "\uD83D\uDCCD %.5f, %.5f", selectedLatitude, selectedLongitude));
            Toast.makeText(this, getString(R.string.location_pinned), Toast.LENGTH_SHORT).show();
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
                                names[i] = PermitTypeHelper.localizeType(ApplyPermitActivity.this, permitTypes.get(i).getName()) + " - " + CurrencyHelper.format(ApplyPermitActivity.this, permitTypes.get(i).getFee());
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
                                    tvFeePreview.setText(getString(R.string.estimated_fee, CurrencyHelper.format(ApplyPermitActivity.this, selectedFee)));
                                    onPermitTypeChanged(selectedType);
                                }
                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {}
                            });
                            applyPrefill(true);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<PermitType>> call, Throwable t) {
                        String[] defaultTypeKeys = {"Business License", "Construction Permit", "Food Service Permit",
                                "Signage Permit", "Event Permit", "Renovation Permit"};
                        String[] displayNames = new String[defaultTypeKeys.length];
                        for (int i = 0; i < defaultTypeKeys.length; i++) {
                            displayNames[i] = PermitTypeHelper.localizeType(ApplyPermitActivity.this, defaultTypeKeys[i]);
                        }
                        spinnerPermitType.setAdapter(new ArrayAdapter<>(
                                ApplyPermitActivity.this,
                                android.R.layout.simple_spinner_dropdown_item,
                                displayNames
                        ));
                        spinnerPermitType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                selectedType = defaultTypeKeys[position];
                                onPermitTypeChanged(selectedType);
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                        applyPrefill(false);
                    }
                });
    }

    private void applyPrefill(boolean fromServerTypes) {
        if (prefillType != null && fromServerTypes && permitTypes != null) {
            for (int i = 0; i < permitTypes.size(); i++) {
                if (prefillType.equals(permitTypes.get(i).getName())) {
                    spinnerPermitType.setSelection(i);
                    break;
                }
            }
        }
        if (prefillDescription != null && !prefillDescription.isEmpty()) {
            etDescription.setText(prefillDescription);
        }
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
            tvLocationInfo.setText(getString(R.string.map_instruction));
        }

        requiredDocumentFiles.clear();
        requiredDocumentChecks.clear();
        checklistItems.removeAllViews();

        String[][] docKeys = PermitTypeHelper.getRequiredDocKeys(type);
        String[] docs = docKeys != null ? PermitTypeHelper.getLocalizedRequiredDocs(this, type) : null;
        if (docs != null && docs.length > 0) {
            documentChecklistContainer.setVisibility(View.VISIBLE);
            for (int i = 0; i < docs.length; i++) {
                String localizedName = docs[i];
                String englishKey = docKeys[i][0];
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, 8, 0, 8);

                CheckBox cb = new CheckBox(this);
                cb.setText(localizedName);
                cb.setEnabled(false);
                cb.setTextSize(13);
                LinearLayout.LayoutParams cbParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                cb.setLayoutParams(cbParams);
                requiredDocumentChecks.put(englishKey, cb);

                MaterialButton uploadBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                uploadBtn.setText(getString(R.string.upload));
                uploadBtn.setTextSize(11);
                uploadBtn.setCornerRadius(24);
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                uploadBtn.setLayoutParams(btnParams);
                final String docKey = englishKey;
                uploadBtn.setOnClickListener(v -> {
                    pendingDocLabel = docKey;
                    requiredDocLauncher.launch("image/*");
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
        File tempFile = File.createTempFile("upload_", ".jpg", getCacheDir());
        try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
            if (inputStream == null) throw new IOException("Failed to open input stream");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }
        }
        return tempFile;
    }

    private void submitApplication() {
        if (selectedType.isEmpty()) {
            Toast.makeText(this, getString(R.string.select_type_msg), Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(ApplyPermitActivity.this, getString(R.string.session_expired), Toast.LENGTH_LONG).show();
                                RetrofitClient.getInstance(ApplyPermitActivity.this).clearSession();
                                Intent loginIntent = new Intent(ApplyPermitActivity.this, LoginActivity.class);
                                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(loginIntent);
                            } else {
                                String errorMsg = getString(R.string.failed_create_permit);
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
                        Toast.makeText(ApplyPermitActivity.this, getString(R.string.error_generic, t.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private int uploadFailCount = 0;

    private void uploadAllRequiredDocuments(int permitId, List<String> labels, int index) {
        if (index >= labels.size()) {
            if (uploadFailCount > 0) {
                Toast.makeText(this, getString(R.string.docs_failed_upload, uploadFailCount), Toast.LENGTH_LONG).show();
            }
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
                        if (!response.isSuccessful()) uploadFailCount++;
                        uploadAllRequiredDocuments(permitId, labels, index + 1);
                    }
                    @Override
                    public void onFailure(Call<Document> call, Throwable t) {
                        uploadFailCount++;
                        uploadAllRequiredDocuments(permitId, labels, index + 1);
                    }
                });
    }


    private void onSubmitSuccess() {
        progressBar.setVisibility(View.GONE);
        setResult(RESULT_OK);
        viewFlipper.setDisplayedChild(1);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDetach();
        for (File f : requiredDocumentFiles.values()) {
            if (f != null && f.exists()) f.delete();
        }
    }
}
