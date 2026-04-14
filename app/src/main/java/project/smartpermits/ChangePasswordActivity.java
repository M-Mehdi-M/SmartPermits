package project.smartpermits;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.ChangePasswordRequest;
import project.smartpermits.models.MessageResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(project.smartpermits.LocaleHelper.applyLocale(newBase));
    }

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnChange = findViewById(R.id.btnChange);

        btnBack.setOnClickListener(v -> finish());
        btnChange.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String current = etCurrentPassword.getText() != null ? etCurrentPassword.getText().toString().trim() : "";
        String newPass = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (current.isEmpty()) {
            etCurrentPassword.setError(getString(R.string.field_required));
            return;
        }
        if (newPass.length() < 6) {
            etNewPassword.setError(getString(R.string.at_least_6_chars));
            return;
        }
        if (!newPass.equals(confirm)) {
            etConfirmPassword.setError(getString(R.string.passwords_dont_match));
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi()
                .changePassword(new ChangePasswordRequest(current, newPass))
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(ChangePasswordActivity.this, getString(R.string.password_changed), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(ChangePasswordActivity.this, getString(R.string.current_password_incorrect), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ChangePasswordActivity.this, getString(R.string.error_generic, t.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

