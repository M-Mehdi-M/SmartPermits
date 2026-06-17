package project.smartpermits;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import project.smartpermits.adapters.PendingPermitAdapter;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.api.SocketIOManager;
import project.smartpermits.models.Permit;
import project.smartpermits.models.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InspectorDashboardActivity extends AppCompatActivity implements PendingPermitAdapter.OnPendingPermitClickListener {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

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

    private BroadcastReceiver socketReceiver;
    private boolean receiverRegistered = false;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final long POLL_INTERVAL_MS = 6000;
    private final Set<Integer> knownPermitIds = new HashSet<>();
    private boolean firstPollDone = false;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            silentRefresh();
            pollHandler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

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
        tvDrawerRole.setText(getString(R.string.inspector));
        loadAvatar();

        adapter = new PendingPermitAdapter(this);
        recyclerPending.setLayoutManager(new LinearLayoutManager(this));
        recyclerPending.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadPendingPermits);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearch = s.toString().trim().toLowerCase();
                    filterPermits();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
            if (id == R.id.nav_dashboard || id == R.id.nav_pending) {
                return true;
            } else if (id == R.id.nav_reviewed) {
                startActivity(new Intent(this, ReviewHistoryActivity.class));
            } else if (id == R.id.nav_analytics) {
                startActivity(new Intent(this, AnalyticsActivity.class));
            } else if (id == R.id.nav_schedule) {
                startActivity(new Intent(this, InspectorScheduleActivity.class));
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
        initializeSocketReceiver();
    }

    private void initializeSocketReceiver() {
        socketReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if ("project.smartpermits.NEW_PERMIT".equals(action)) {
                    loadPendingPermits();
                } else if ("project.smartpermits.COMMENT_NOTIFICATION".equals(action)) {
                    int permitId = intent.getIntExtra("permit_id", -1);
                    String commenter = intent.getStringExtra("commenter_name");
                    String preview = intent.getStringExtra("message_preview");
                    if (commenter == null) commenter = "Someone";
                    if (preview == null) preview = "";
                    NotificationHelper.showNotification(
                            InspectorDashboardActivity.this,
                            "New Comment from " + commenter,
                            preview.isEmpty() ? "Tap to view" : preview,
                            permitId > 0 ? permitId : (int) System.currentTimeMillis());
                } else if ("project.smartpermits.NEW_COMMENT".equals(action)) {
                    int permitId = intent.getIntExtra("permit_id", -1);
                    NotificationHelper.showNotification(
                            InspectorDashboardActivity.this,
                            "New Comment",
                            "A new comment was added to permit #" + permitId,
                            permitId > 0 ? permitId * 1000 : (int) System.currentTimeMillis());
                } else if ("project.smartpermits.APPOINTMENT_SCHEDULED".equals(action)) {
                    int permitId = intent.getIntExtra("permit_id", -1);
                    NotificationHelper.showNotification(
                            InspectorDashboardActivity.this,
                            "Inspection Scheduled",
                            "A citizen scheduled an inspection appointment",
                            permitId > 0 ? permitId * 100 : (int) System.currentTimeMillis());
                    loadPendingPermits();
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
        if (socketReceiver != null && receiverRegistered) {
            unregisterReceiver(socketReceiver);
            receiverRegistered = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (socketReceiver != null && !receiverRegistered) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("project.smartpermits.NEW_PERMIT");
            filter.addAction("project.smartpermits.APPOINTMENT_SCHEDULED");
            filter.addAction("project.smartpermits.COMMENT_NOTIFICATION");
            filter.addAction("project.smartpermits.NEW_COMMENT");
            ContextCompat.registerReceiver(this, socketReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
        ensureSocketConnected();
        String name = RetrofitClient.getInstance(this).getUserName();
        tvInspectorName.setText(name);
        tvDrawerName.setText(name);
        loadAvatar();
        loadPendingPermits();
        pollHandler.removeCallbacks(pollRunnable);
        pollHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void ensureSocketConnected() {
        RetrofitClient client = RetrofitClient.getInstance(this);
        if (SocketIOManager.getInstance(this).isConnected()) return;
        int userId = client.getUserId();
        if (userId > 0) {
            SocketIOManager.getInstance(this).connect(userId);
            return;
        }
        client.getApi().getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User profile = response.body();
                    client.saveUserId(profile.getId());
                    if (profile.getFullName() != null) client.saveUserName(profile.getFullName());
                    if (profile.getRole() != null) client.saveUserRole(profile.getRole());
                    SocketIOManager.getInstance(InspectorDashboardActivity.this).connect(profile.getId());
                    runOnUiThread(() -> {
                        tvInspectorName.setText(client.getUserName());
                        tvDrawerName.setText(client.getUserName());
                    });
                }
            }
            @Override public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void silentRefresh() {
        RetrofitClient.getInstance(this).getApi().getPendingPermits()
                .enqueue(new Callback<List<Permit>>() {
                    @Override
                    public void onResponse(Call<List<Permit>> call, Response<List<Permit>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Permit> fresh = response.body();
                            if (firstPollDone) {
                                for (Permit p : fresh) {
                                    if (!knownPermitIds.contains(p.getId())) {
                                        NotificationHelper.showNotification(
                                                InspectorDashboardActivity.this,
                                                "New Permit Application",
                                                p.getPermitType() != null ? p.getPermitType() + " – tap to review" : "Tap to review",
                                                p.getId());
                                    }
                                }
                            }
                            knownPermitIds.clear();
                            for (Permit p : fresh) knownPermitIds.add(p.getId());
                            firstPollDone = true;
                            allPermits = fresh;
                            tvCount.setText(String.valueOf(fresh.size()));
                            filterPermits();
                        }
                    }
                    @Override public void onFailure(Call<List<Permit>> call, Throwable t) {}
                });
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
                            knownPermitIds.clear();
                            for (Permit p : allPermits) knownPermitIds.add(p.getId());
                            firstPollDone = true;
                            tvCount.setText(String.valueOf(allPermits.size()));
                            filterPermits();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Permit>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(InspectorDashboardActivity.this,
                                getString(R.string.error_generic, t.getMessage()), Toast.LENGTH_LONG).show();
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
            if (matchesSearch) filtered.add(p);
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
                .setTitle(getString(R.string.about_title))
                .setMessage(getString(R.string.about_text) + "\n\n" + getString(R.string.app_version))
                .setPositiveButton(getString(R.string.ok), null)
                .show();
    }

    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.sign_out_title))
                .setMessage(getString(R.string.sign_out_confirm))
                .setPositiveButton(getString(R.string.nav_sign_out), (d, w) -> {
                    RetrofitClient.getInstance(this).clearSession();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}
