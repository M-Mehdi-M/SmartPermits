package project.smartpermits;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private ViewFlipper viewFlipper;
    private Spinner spinnerPermitType;
    private TextInputEditText etDescription;
    private TextView tvFeePreview;
    private ImageView ivDocument;
    private LinearLayout placeholderPhoto;
    private ProgressBar progressBar;

    private List<PermitType> permitTypes;
    private String selectedType = "";
    private double selectedFee = 0;
    private Uri photoUri;
    private File photoFile;
    private java.util.ArrayList<File> documentFiles = new java.util.ArrayList<>();
    private int createdPermitId = -1;

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), result -> {
                if (result && photoUri != null) {
                    showPhoto(photoUri);
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    photoUri = uris.get(0);
                    showPhoto(photoUri);
                    for (Uri uri : uris) {
                        try {
                            documentFiles.add(copyUriToFile(uri));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (!documentFiles.isEmpty()) {
                        photoFile = documentFiles.get(0);
                    }
                    Toast.makeText(this, documentFiles.size() + " document(s) selected", Toast.LENGTH_SHORT).show();
                    TextView tvDocCount = findViewById(R.id.tvDocCount);
                    if (tvDocCount != null) {
                        tvDocCount.setText(documentFiles.size() + " document(s) attached");
                    }
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchCamera();
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_permit);

        viewFlipper = findViewById(R.id.viewFlipper);
        spinnerPermitType = findViewById(R.id.spinnerPermitType);
        etDescription = findViewById(R.id.etDescription);
        tvFeePreview = findViewById(R.id.tvFeePreview);
        ivDocument = findViewById(R.id.ivDocument);
        placeholderPhoto = findViewById(R.id.placeholderPhoto);
        progressBar = findViewById(R.id.progressBar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnNext1 = findViewById(R.id.btnNext1);
        MaterialButton btnCamera = findViewById(R.id.btnCamera);
        MaterialButton btnGallery = findViewById(R.id.btnGallery);
        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);
        MaterialButton btnBackToDashboard = findViewById(R.id.btnBackToDashboard);
        FrameLayout framePhoto = findViewById(R.id.framePhoto);

        btnBack.setOnClickListener(v -> finish());
        btnNext1.setOnClickListener(v -> goToStep2());
        btnCamera.setOnClickListener(v -> checkCameraPermission());
        btnGallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        framePhoto.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        btnSubmit.setOnClickListener(v -> submitApplication());
        btnBackToDashboard.setOnClickListener(v -> finish());

        loadPermitTypes();
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
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                });
    }

    private void goToStep2() {
        if (selectedType.isEmpty()) {
            Toast.makeText(this, "Please select a permit type", Toast.LENGTH_SHORT).show();
            return;
        }
        viewFlipper.setDisplayedChild(1);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = getExternalCacheDir();
            photoFile = File.createTempFile("PERMIT_" + timeStamp + "_", ".jpg", storageDir);
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating photo file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPhoto(Uri uri) {
        ivDocument.setImageURI(uri);
        ivDocument.setVisibility(View.VISIBLE);
        placeholderPhoto.setVisibility(View.GONE);
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
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance(this).getApi()
                .createPermit(new PermitRequest(selectedType, description))
                .enqueue(new Callback<Permit>() {
                    @Override
                    public void onResponse(Call<Permit> call, Response<Permit> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            createdPermitId = response.body().getId();
                            if (!documentFiles.isEmpty()) {
                                uploadAllDocuments(createdPermitId, 0);
                            } else if (photoFile != null && photoFile.exists()) {
                                uploadDocument(createdPermitId);
                            } else {
                                onSubmitSuccess();
                            }
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ApplyPermitActivity.this, "Failed to create permit", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Permit> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ApplyPermitActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadDocument(int permitId) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), photoFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", photoFile.getName(), requestFile);

        RetrofitClient.getInstance(this).getApi()
                .uploadDocument(permitId, body)
                .enqueue(new Callback<Document>() {
                    @Override
                    public void onResponse(Call<Document> call, Response<Document> response) {
                        onSubmitSuccess();
                    }

                    @Override
                    public void onFailure(Call<Document> call, Throwable t) {
                        onSubmitSuccess();
                    }
                });
    }

    private void uploadAllDocuments(int permitId, int index) {
        if (index >= documentFiles.size()) {
            onSubmitSuccess();
            return;
        }
        File file = documentFiles.get(index);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        RetrofitClient.getInstance(this).getApi()
                .uploadDocument(permitId, body)
                .enqueue(new Callback<Document>() {
                    @Override
                    public void onResponse(Call<Document> call, Response<Document> response) {
                        uploadAllDocuments(permitId, index + 1);
                    }
                    @Override
                    public void onFailure(Call<Document> call, Throwable t) {
                        uploadAllDocuments(permitId, index + 1);
                    }
                });
    }

    private void onSubmitSuccess() {
        progressBar.setVisibility(View.GONE);
        viewFlipper.setDisplayedChild(2);
    }
}

