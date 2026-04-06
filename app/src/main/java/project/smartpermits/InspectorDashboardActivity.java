package project.smartpermits;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import project.smartpermits.adapters.PendingPermitAdapter;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.Permit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InspectorDashboardActivity extends AppCompatActivity implements PendingPermitAdapter.OnPendingPermitClickListener {

    private RecyclerView recyclerPending;
    private PendingPermitAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private TextView tvCount;
    private DrawerLayout drawerLayout;
    private TextView tvInspectorName;
    private TextView tvDrawerName;
    private android.widget.ImageView ivDrawerAvatar;
    private TextInputEditText etSearch;
    private List<Permit> allPermits = new ArrayList<>();
    private String currentSearch = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspector_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navView = findViewById(R.id.navView);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        recyclerPending = findViewById(R.id.recyclerPending);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
        tvCount = findViewById(R.id.tvCount);
        etSearch = findViewById(R.id.etSearch);

        String userName = RetrofitClient.getInstance(this).getUserName();
        tvInspectorName = findViewById(R.id.tvInspectorName);
        tvInspectorName.setText(userName != null ? userName : "");

        View headerView = navView.getHeaderView(0);
        tvDrawerName = headerView.findViewById(R.id.tvDrawerName);
        TextView tvDrawerRole = headerView.findViewById(R.id.tvDrawerRole);
        ivDrawerAvatar = headerView.findViewById(R.id.ivDrawerAvatar);
        tvDrawerName.setText(userName);
        tvDrawerRole.setText("Inspector");
        loadAvatar();

        adapter = new PendingPermitAdapter(this);
        recyclerPending.setLayoutManager(new LinearLayoutManager(this));
        recyclerPending.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadPendingPermits);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearch = s.toString().trim().toLowerCase();
                    filterPermits();
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                return true;
            } else if (id == R.id.nav_pending) {
                return true;
            } else if (id == R.id.nav_reviewed) {
                startActivity(new Intent(this, ReviewHistoryActivity.class));
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalyticsActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_edit_profile) {
                startActivity(new Intent(this, EditProfileActivity.class));
            } else if (id == R.id.nav_change_password) {
                startActivity(new Intent(this, ChangePasswordActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_about) {
                showAboutDialog();
            } else if (id == R.id.nav_sign_out) {
                showSignOutDialog();
            }
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });

        NotificationHelper.createChannel(this);
        NotificationHelper.requestPermissionIfNeeded(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String name = RetrofitClient.getInstance(this).getUserName();
        tvInspectorName.setText(name);
        tvDrawerName.setText(name);
        loadAvatar();
        loadPendingPermits();
    }

    private void loadAvatar() {
        RetrofitClient client = RetrofitClient.getInstance(this);
        String avatarUrl = client.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            String url = client.getBaseUrl() + "uploads/" + avatarUrl;
            Glide.with(this).load(url).circleCrop().placeholder(R.drawable.icon).into(ivDrawerAvatar);
        }
    }

    private void loadPendingPermits() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().getPendingPermits()
                .enqueue(new Callback<List<Permit>>() {
                    @Override
                    public void onResponse(Call<List<Permit>> call, Response<List<Permit>> response) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            allPermits = response.body();
                            tvCount.setText(String.valueOf(allPermits.size()));
                            filterPermits();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Permit>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(InspectorDashboardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void filterPermits() {
        List<Permit> filtered = new ArrayList<>();
        for (Permit p : allPermits) {
            boolean matchesSearch = currentSearch.isEmpty()
                    || (p.getPermitType() != null && p.getPermitType().toLowerCase().contains(currentSearch))
                    || (p.getApplicantName() != null && p.getApplicantName().toLowerCase().contains(currentSearch))
                    || (p.getDescription() != null && p.getDescription().toLowerCase().contains(currentSearch));
            if (matchesSearch) {
                filtered.add(p);
            }
        }
        adapter.setPermits(filtered);
        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerPending.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPendingPermitClick(Permit permit) {
        Intent intent = new Intent(this, PermitReviewActivity.class);
        intent.putExtra("permit_id", permit.getId());
        startActivity(intent);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("About SmartPermits")
                .setMessage(getString(R.string.about_text) + "\n\n" + getString(R.string.app_version))
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage(getString(R.string.sign_out_confirm))
                .setPositiveButton("Sign Out", (d, w) -> {
                    RetrofitClient.getInstance(this).clearSession();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
