package project.smartpermits;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;

import project.smartpermits.api.RetrofitClient;

public class SettingsActivity extends AppCompatActivity {

    private TextView tvCurrentLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        SwitchMaterial switchFollowSystem = findViewById(R.id.switchFollowSystem);
        SwitchMaterial switchNotifications = findViewById(R.id.switchNotifications);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);
        findViewById(R.id.layoutLanguage).setOnClickListener(v -> showLanguagePicker());

        SharedPreferences prefs = getSharedPreferences("smart_permits_prefs", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        boolean followSystem = prefs.getBoolean("follow_system_theme", false);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);

        switchDarkMode.setChecked(isDark);
        if (switchFollowSystem != null) {
            switchFollowSystem.setChecked(followSystem);
            switchDarkMode.setEnabled(!followSystem);
        }
        if (switchNotifications != null) {
            switchNotifications.setChecked(notificationsEnabled);
        }

        updateLanguageLabel();

        btnBack.setOnClickListener(v -> finish());

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });

        if (switchFollowSystem != null) {
            switchFollowSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("follow_system_theme", isChecked).apply();
                switchDarkMode.setEnabled(!isChecked);
                if (isChecked) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    boolean dark = prefs.getBoolean("dark_mode", false);
                    AppCompatDelegate.setDefaultNightMode(
                            dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                }
            });
        }

        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                prefs.edit().putBoolean("notifications_enabled", isChecked).apply();
            });
        }
    }

    private void showLanguagePicker() {
        String[] names = LocaleHelper.getSupportedLanguageNames();
        String[] codes = LocaleHelper.getSupportedLanguageCodes();
        String current = LocaleHelper.getLanguage(this);
        int selected = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) {
                selected = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.language)
                .setSingleChoiceItems(names, selected, (dialog, which) -> {
                    dialog.dismiss();
                    String chosenCode = codes[which];
                    String oldCode = LocaleHelper.getLanguage(this);
                    if (chosenCode.equals(oldCode)) return;
                    LocaleHelper.setLanguage(this, chosenCode);
                    LocaleHelper.updateResources(this, chosenCode);
                    restartApp();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void restartApp() {
        String role = RetrofitClient.getInstance(this).getUserRole();
        Class<?> target;
        if ("inspector".equals(role)) {
            target = InspectorDashboardActivity.class;
        } else {
            target = CitizenDashboardActivity.class;
        }
        Intent intent = new Intent(this, target);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private void updateLanguageLabel() {
        String current = LocaleHelper.getLanguage(this);
        String[] codes = LocaleHelper.getSupportedLanguageCodes();
        String[] names = LocaleHelper.getSupportedLanguageNames();
        String label = names[0];
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) {
                label = names[i];
                break;
            }
        }
        if (tvCurrentLanguage != null) {
            tvCurrentLanguage.setText(label);
        }
    }
}
