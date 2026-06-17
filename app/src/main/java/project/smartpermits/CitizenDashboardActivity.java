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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.smartpermits.adapters.PermitAdapter;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.api.SocketIOManager;
import project.smartpermits.models.Permit;
import project.smartpermits.models.User;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CitizenDashboardActivity extends AppCompatActivity implements PermitAdapter.OnPermitClickListener {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    private RecyclerView recyclerPermits;
    private PermitAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private DrawerLayout drawerLayout;
    private TextView tvUserName;
    private TextView tvDrawerName;
    private android.widget.ImageView ivDrawerAvatar;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private List<Permit> allPermits = new ArrayList<>();
    private String currentSearch = "";
    private String currentStatusFilter = "";

    private BroadcastReceiver socketReceiver;
    private ActivityResultLauncher<Intent> applyPermitLauncher;
    private boolean receiverRegistered = false;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private static final long POLL_INTERVAL_MS = 6000;
    private final Map<Integer, String> knownStatuses = new HashMap<>();
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
        setContentView(R.layout.activity_citizen_dashboard);

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navView = findViewById(R.id.navView);
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        recyclerPermits = findViewById(R.id.recyclerPermits);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
        ExtendedFloatingActionButton fabApply = findViewById(R.id.fabApply);
        etSearch = findViewById(R.id.etSearch);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);

        applyPermitLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> loadPermits());

        String userName = RetrofitClient.getInstance(this).getUserName();
        tvUserName = findViewById(R.id.tvUserName);
        tvUserName.setText(userName != null ? userName : "");

        View headerView = navView.getHeaderView(0);
        tvDrawerName = headerView.findViewById(R.id.tvDrawerName);
        TextView tvDrawerRole = headerView.findViewById(R.id.tvDrawerRole);
        ivDrawerAvatar = headerView.findViewById(R.id.ivDrawerAvatar);
        tvDrawerName.setText(userName);
        tvDrawerRole.setText(getString(R.string.citizen));
        loadAvatar();

        adapter = new PermitAdapter(this);
        recyclerPermits.setLayoutManager(new LinearLayoutManager(this));
        recyclerPermits.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadPermits);

        fabApply.setOnClickListener(v ->
                applyPermitLauncher.launch(new Intent(this, ApplyPermitActivity.class)));

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

        if (chipGroupFilter != null) {
            chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) {
                    currentStatusFilter = "";
                } else {
                    Chip chip = group.findViewById(checkedIds.get(0));
                    if (chip != null && chip.getTag() != null) {
                        currentStatusFilter = chip.getTag().toString();
                    } else {
                        currentStatusFilter = "";
                    }
                }
                filterPermits();
            });
        }

        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, PermitHistoryActivity.class));
            } else if (id == R.id.nav_trash) {
                startActivity(new Intent(this, TrashActivity.class));
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
            public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                String action = intent.getAction();
                if ("project.smartpermits.PERMIT_STATUS_UPDATED".equals(action)
                        || "project.smartpermits.PERMIT_REVIEWED".equals(action)) {
                    loadPermits();
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
            filter.addAction("project.smartpermits.PERMIT_STATUS_UPDATED");
            filter.addAction("project.smartpermits.PERMIT_REVIEWED");
            filter.addAction("project.smartpermits.NEW_COMMENT");
            filter.addAction("project.smartpermits.COMMENT_NOTIFICATION");
            ContextCompat.registerReceiver(this, socketReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
        ensureSocketConnected();
        String name = RetrofitClient.getInstance(this).getUserName();
        tvUserName.setText(name);
        tvDrawerName.setText(name);
        loadAvatar();
        loadPermits();
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
                    SocketIOManager.getInstance(CitizenDashboardActivity.this).connect(profile.getId());
                    runOnUiThread(() -> {
                        tvUserName.setText(client.getUserName());
                        tvDrawerName.setText(client.getUserName());
                    });
                }
            }
            @Override public void onFailure(Call<User> call, Throwable t) {}
        });
    }

    private void silentRefresh() {
        RetrofitClient.getInstance(this).getApi().getMyPermits()
                .enqueue(new Callback<List<Permit>>() {
                    @Override
                    public void onResponse(Call<List<Permit>> call, Response<List<Permit>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            List<Permit> fresh = response.body();
                            if (firstPollDone) {
                                for (Permit p : fresh) {
                                    String oldStatus = knownStatuses.get(p.getId());
                                    String newStatus = p.getStatus();
                                    if (newStatus != null && !newStatus.equals(oldStatus) && oldStatus != null) {
                                        String title = "approved".equals(newStatus) ? "Permit Approved"
                                                : "rejected".equals(newStatus) ? "Permit Rejected"
                                                : "completed".equals(newStatus) ? "Permit Completed"
                                                : "Permit Updated";
                                        String type = p.getPermitType() != null ? p.getPermitType() : "Your permit";
                                        NotificationHelper.showNotification(
                                                CitizenDashboardActivity.this, title, type + " status changed", p.getId());
                                    }
                                }
                            }
                            knownStatuses.clear();
                            for (Permit p : fresh) {
                                if (p.getStatus() != null) knownStatuses.put(p.getId(), p.getStatus());
                            }
                            firstPollDone = true;
                            allPermits = fresh;
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

    private void loadPermits() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().getMyPermits()
                .enqueue(new Callback<List<Permit>>() {
                    @Override
                    public void onResponse(Call<List<Permit>> call, Response<List<Permit>> response) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            allPermits = response.body();
                            knownStatuses.clear();
                            for (Permit p : allPermits) {
                                if (p.getStatus() != null) knownStatuses.put(p.getId(), p.getStatus());
                            }
                            firstPollDone = true;
                            filterPermits();
                        }
                    }
                    @Override
                    public void onFailure(Call<List<Permit>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(CitizenDashboardActivity.this,
                                getString(R.string.error_generic, t.getMessage()), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void filterPermits() {
        List<Permit> filtered = new ArrayList<>();
        for (Permit p : allPermits) {
            boolean matchesSearch = currentSearch.isEmpty()
                    || (p.getPermitType() != null && p.getPermitType().toLowerCase().contains(currentSearch))
                    || (p.getDescription() != null && p.getDescription().toLowerCase().contains(currentSearch))
                    || (p.getCreatedAt() != null && p.getCreatedAt().contains(currentSearch));
            boolean matchesStatus = currentStatusFilter.isEmpty()
                    || currentStatusFilter.equals(p.getStatus());
            if (matchesSearch && matchesStatus) filtered.add(p);
        }
        adapter.setPermits(filtered);
        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerPermits.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onPermitClick(Permit permit) {
        Intent intent = new Intent(this, PermitDetailActivity.class);
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
