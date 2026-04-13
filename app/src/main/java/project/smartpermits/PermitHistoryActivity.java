package project.smartpermits;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import project.smartpermits.adapters.PermitAdapter;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.Permit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PermitHistoryActivity extends AppCompatActivity implements PermitAdapter.OnPermitClickListener {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(project.smartpermits.LocaleHelper.applyLocale(newBase));
    }

    private RecyclerView recyclerHistory;
    private PermitAdapter adapter;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private TextView tvEmptyMessage;
    private List<Permit> allPermits = new ArrayList<>();
    private int currentFilter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permit_history);

        recyclerHistory = findViewById(R.id.recyclerHistory);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);
        ImageButton btnBack = findViewById(R.id.btnBack);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        btnBack.setOnClickListener(v -> finish());

        adapter = new PermitAdapter(this);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistory.setAdapter(adapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getPosition();
                filterPermits();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        loadPermits();
    }

    private void loadPermits() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().getMyPermits()
                .enqueue(new Callback<List<Permit>>() {
                    @Override
                    public void onResponse(Call<List<Permit>> call, Response<List<Permit>> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            allPermits = response.body();
                            filterPermits();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Permit>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(PermitHistoryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void filterPermits() {
        List<Permit> filtered = new ArrayList<>();
        String filterStatus;
        switch (currentFilter) {
            case 1: filterStatus = "approved"; break;
            case 2: filterStatus = "completed"; break;
            case 3: filterStatus = "rejected"; break;
            default: filterStatus = null; break;
        }

        for (Permit p : allPermits) {
            if (filterStatus == null || filterStatus.equals(p.getStatus())) {
                filtered.add(p);
            }
        }

        adapter.setPermits(filtered);
        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerHistory.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);

        if (filtered.isEmpty()) {
            switch (currentFilter) {
                case 1: tvEmptyMessage.setText("No approved permits"); break;
                case 2: tvEmptyMessage.setText("No completed permits"); break;
                case 3: tvEmptyMessage.setText("No rejected permits"); break;
                default: tvEmptyMessage.setText("No permits found"); break;
            }
        }
    }

    @Override
    public void onPermitClick(Permit permit) {
        Intent intent = new Intent(this, PermitDetailActivity.class);
        intent.putExtra("permit_id", permit.getId());
        startActivity(intent);
    }
}

