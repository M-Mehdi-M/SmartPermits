package project.smartpermits;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;

import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.LoginRequest;
import project.smartpermits.models.LoginResponse;
import project.smartpermits.models.RegisterRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword, etEmail, etFullName;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private LinearLayout registerFields;
    private MaterialButtonToggleGroup roleToggle;
    private TextView tvFormTitle, tvSwitchLabel, tvSwitchAction;
    private boolean isRegisterMode = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences darkPrefs = getSharedPreferences("smart_permits_prefs", MODE_PRIVATE);
        boolean followSystem = darkPrefs.getBoolean("follow_system_theme", false);
        if (followSystem) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            boolean isDark = darkPrefs.getBoolean("dark_mode", false);
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    isDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                           : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        RetrofitClient client = RetrofitClient.getInstance(this);
        if (client.isLoggedIn()) {
            navigateToDashboard(client.getUserRole());
            return;
        }

        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etEmail = findViewById(R.id.etEmail);
        etFullName = findViewById(R.id.etFullName);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        registerFields = findViewById(R.id.registerFields);
        roleToggle = findViewById(R.id.roleToggle);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvSwitchLabel = findViewById(R.id.tvSwitchLabel);
        tvSwitchAction = findViewById(R.id.tvSwitchAction);

        btnLogin.setOnClickListener(v -> {
            if (isRegisterMode) {
                performRegister();
            } else {
                performLogin();
            }
        });

        tvSwitchAction.setOnClickListener(v -> toggleMode());
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        if (isRegisterMode) {
            registerFields.setVisibility(View.VISIBLE);
            btnLogin.setText(R.string.register);
            tvFormTitle.setText(R.string.create_account);
            tvSwitchLabel.setText(R.string.have_account);
            tvSwitchAction.setText(getString(R.string.login));
        } else {
            registerFields.setVisibility(View.GONE);
            btnLogin.setText(R.string.login);
            tvFormTitle.setText(R.string.welcome_back);
            tvSwitchLabel.setText(R.string.no_account);
            tvSwitchAction.setText(getString(R.string.register));
        }
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        RetrofitClient.getInstance(this).getApi()
                .login(new LoginRequest(username, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            handleAuthSuccess(response.body());
                        } else {
                            Toast.makeText(LoginActivity.this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, getString(R.string.connection_error) + ": " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void performRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || fullName.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }
        if (username.length() < 3) {
            Toast.makeText(this, getString(R.string.username_min_chars), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.valid_email_required), Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, getString(R.string.password_min_chars), Toast.LENGTH_SHORT).show();
            return;
        }

        String role = roleToggle.getCheckedButtonId() == R.id.btnRoleInspector ? "inspector" : "citizen";

        setLoading(true);
        RetrofitClient.getInstance(this).getApi()
                .register(new RegisterRequest(username, password, email, role, fullName))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            handleAuthSuccess(response.body());
                        } else {
                            Toast.makeText(LoginActivity.this, getString(R.string.registration_failed), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this, getString(R.string.connection_error) + ": " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleAuthSuccess(LoginResponse response) {
        if (response == null || response.getToken() == null) {
            Toast.makeText(this, getString(R.string.login_failed_invalid), Toast.LENGTH_SHORT).show();
            return;
        }
        RetrofitClient client = RetrofitClient.getInstance(this);
        client.saveToken(response.getToken());
        if (response.getUser() != null) {
            String role = response.getUser().getRole() != null ? response.getUser().getRole() : "citizen";
            client.saveUserRole(role);
            client.saveUserName(response.getUser().getFullName() != null ? response.getUser().getFullName() : "");
            client.saveUserEmail(response.getUser().getEmail() != null ? response.getUser().getEmail() : "");
            String avatar = response.getUser().getAvatarUrl();
            client.saveAvatarUrl(avatar != null ? avatar : "");
            int userId = response.getUser().getId();
            client.saveUserId(userId);
            navigateToDashboard(role);
        } else {
            client.saveUserRole("citizen");
            navigateToDashboard("citizen");
        }
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("inspector".equals(role)) {
            intent = new Intent(this, InspectorDashboardActivity.class);
        } else {
            intent = new Intent(this, CitizenDashboardActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}

