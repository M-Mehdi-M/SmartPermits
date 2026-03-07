package project.smartpermits;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        SwitchMaterial switchDarkMode = findViewById(R.id.switchDarkMode);
        SwitchMaterial switchFollowSystem = findViewById(R.id.switchFollowSystem);
        SwitchMaterial switchNotifications = findViewById(R.id.switchNotifications);

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
}
