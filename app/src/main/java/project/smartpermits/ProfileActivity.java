package project.smartpermits;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.MessageResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(project.smartpermits.LocaleHelper.applyLocale(newBase));
    }

    private TextView tvProfileName, tvProfileRole, tvInfoName, tvInfoRole, tvInfoEmail;
    private ImageView ivProfileAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        RetrofitClient client = RetrofitClient.getInstance(this);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileRole = findViewById(R.id.tvProfileRole);
        tvInfoName = findViewById(R.id.tvInfoName);
        tvInfoRole = findViewById(R.id.tvInfoRole);
        tvInfoEmail = findViewById(R.id.tvInfoEmail);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnSignOut = findViewById(R.id.btnSignOut);
        MaterialButton btnEditProfile = findViewById(R.id.btnEditProfile);
        MaterialButton btnChangePassword = findViewById(R.id.btnChangePassword);
        MaterialButton btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        btnBack.setOnClickListener(v -> finish());
        btnEditProfile.setOnClickListener(v ->
            startActivity(new Intent(this, EditProfileActivity.class))
        );
        btnChangePassword.setOnClickListener(v ->
            startActivity(new Intent(this, ChangePasswordActivity.class))
        );

        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("This will permanently delete your account, all your permits, documents, and data. This action cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        RetrofitClient.getInstance(this).getApi().deleteAccount()
                                .enqueue(new Callback<MessageResponse>() {
                                    @Override
                                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                                        if (response.isSuccessful()) {
                                            Toast.makeText(ProfileActivity.this, "Account deleted", Toast.LENGTH_LONG).show();
                                            client.clearSession();
                                            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Toast.makeText(ProfileActivity.this, "Failed to delete account", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                                        Toast.makeText(ProfileActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        });

        btnSignOut.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.sign_out_title))
                    .setMessage(getString(R.string.sign_out_confirm))
                    .setPositiveButton(getString(R.string.nav_sign_out), (d, w) -> {
                        client.clearSession();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void loadProfileData() {
        RetrofitClient client = RetrofitClient.getInstance(this);
        String userName = client.getUserName();
        String userRole = client.getUserRole();
        String userEmail = client.getUserEmail();
        String avatarUrl = client.getAvatarUrl();

        if (userName == null) userName = "";
        if (userRole == null || userRole.isEmpty()) userRole = "citizen";
        if (userEmail == null) userEmail = "";

        tvProfileName.setText(userName);
        String roleDisplay = userRole.substring(0, 1).toUpperCase() + userRole.substring(1);
        tvProfileRole.setText(roleDisplay);
        tvInfoName.setText(userName);
        tvInfoRole.setText(roleDisplay);
        tvInfoEmail.setText(userEmail.isEmpty() ? "Not set" : userEmail);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            String url = client.getBaseUrl() + "uploads/" + avatarUrl;
            Glide.with(this)
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.icon)
                    .into(ivProfileAvatar);
        }
    }
}

