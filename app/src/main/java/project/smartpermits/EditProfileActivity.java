package project.smartpermits;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.ProfileUpdateRequest;
import project.smartpermits.models.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etFullName, etEmail, etUsername;
    private ImageView ivAvatar;
    private ProgressBar progressBar;
    private File avatarFile;

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    ivAvatar.setImageURI(uri);
                    try {
                        avatarFile = copyUriToFile(uri);
                        uploadAvatar();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        ivAvatar = findViewById(R.id.ivAvatar);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnSave = findViewById(R.id.btnSave);
        FloatingActionButton fabChangeAvatar = findViewById(R.id.fabChangeAvatar);

        btnBack.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveProfile());
        fabChangeAvatar.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        loadProfile();
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().getProfile()
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            User user = response.body();
                            etFullName.setText(user.getFullName());
                            etEmail.setText(user.getEmail());
                            etUsername.setText(user.getUsername());

                            String avatarUrl = user.getAvatarUrl();
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                String url = RetrofitClient.getInstance(EditProfileActivity.this).getBaseUrl() + "uploads/" + avatarUrl;
                                Glide.with(EditProfileActivity.this)
                                        .load(url)
                                        .circleCrop()
                                        .into(ivAvatar);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        RetrofitClient client = RetrofitClient.getInstance(EditProfileActivity.this);
                        etFullName.setText(client.getUserName());
                        etEmail.setText(client.getUserEmail());
                    }
                });
    }

    private void saveProfile() {
        String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etFullName.setError("Name is required");
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        RetrofitClient.getInstance(this).getApi()
                .updateProfile(new ProfileUpdateRequest(name, email))
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            User user = response.body();
                            RetrofitClient client = RetrofitClient.getInstance(EditProfileActivity.this);
                            client.saveUserName(user.getFullName());
                            client.saveUserEmail(user.getEmail());
                            Toast.makeText(EditProfileActivity.this, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
                            finish();
                        } else if (response.code() == 409) {
                            etEmail.setError("Email already in use");
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EditProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadAvatar() {
        if (avatarFile == null || !avatarFile.exists()) return;

        progressBar.setVisibility(View.VISIBLE);
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), avatarFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", avatarFile.getName(), requestFile);

        RetrofitClient.getInstance(this).getApi()
                .uploadAvatar(body)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            RetrofitClient client = RetrofitClient.getInstance(EditProfileActivity.this);
                            client.saveAvatarUrl(response.body().getAvatarUrl());
                            Toast.makeText(EditProfileActivity.this, getString(R.string.avatar_updated), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(EditProfileActivity.this, "Avatar upload failed", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(EditProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private File copyUriToFile(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        File tempFile = File.createTempFile("avatar_", ".jpg", getCacheDir());
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
}

