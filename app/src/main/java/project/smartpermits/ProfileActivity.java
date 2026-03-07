package project.smartpermits;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import project.smartpermits.api.RetrofitClient;

public class ProfileActivity extends AppCompatActivity {

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

        btnBack.setOnClickListener(v -> finish());
        btnEditProfile.setOnClickListener(v ->
            startActivity(new Intent(this, EditProfileActivity.class))
        );
        btnChangePassword.setOnClickListener(v ->
            startActivity(new Intent(this, ChangePasswordActivity.class))
        );

        btnSignOut.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Sign Out")
                    .setMessage(getString(R.string.sign_out_confirm))
                    .setPositiveButton("Sign Out", (d, w) -> {
                        client.clearSession();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
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

