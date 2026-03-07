package project.smartpermits;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;
import java.util.Set;

import project.smartpermits.adapters.TrashAdapter;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.MessageResponse;
import project.smartpermits.models.Permit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TrashActivity extends AppCompatActivity implements TrashAdapter.OnTrashActionListener {

    private RecyclerView recyclerTrash;
    private TrashAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        recyclerTrash = findViewById(R.id.recyclerTrash);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnEmptyTrash = findViewById(R.id.btnEmptyTrash);

        adapter = new TrashAdapter(this);
        recyclerTrash.setLayoutManager(new LinearLayoutManager(this));
        recyclerTrash.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadTrash);

        btnBack.setOnClickListener(v -> finish());
        btnEmptyTrash.setOnClickListener(v -> confirmEmptyTrash());

        loadTrash();
    }

    private void loadTrash() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().getTrash()
                .enqueue(new Callback<List<Permit>>() {
                    @Override
                    public void onResponse(Call<List<Permit>> call, Response<List<Permit>> response) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.setPermits(response.body());
                            emptyView.setVisibility(response.body().isEmpty() ? View.VISIBLE : View.GONE);
                            recyclerTrash.setVisibility(response.body().isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Permit>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(TrashActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRestore(Permit permit) {
        RetrofitClient.getInstance(this).getApi().restorePermit(permit.getId())
                .enqueue(new Callback<Permit>() {
                    @Override
                    public void onResponse(Call<Permit> call, Response<Permit> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(TrashActivity.this, "Permit restored", Toast.LENGTH_SHORT).show();
                            loadTrash();
                        }
                    }

                    @Override
                    public void onFailure(Call<Permit> call, Throwable t) {
                        Toast.makeText(TrashActivity.this, "Error restoring", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDelete(Permit permit) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Permanently")
                .setMessage("This cannot be undone. Are you sure?")
                .setPositiveButton("Delete", (d, w) -> permanentDelete(permit.getId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onSelectionChanged(int count) {
    }

    private void permanentDelete(int permitId) {
        RetrofitClient.getInstance(this).getApi().permanentDeletePermit(permitId)
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(TrashActivity.this, "Permanently deleted", Toast.LENGTH_SHORT).show();
                            loadTrash();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        Toast.makeText(TrashActivity.this, "Error deleting", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void confirmEmptyTrash() {
        new AlertDialog.Builder(this)
                .setTitle("Empty Trash")
                .setMessage("All items in trash will be permanently deleted. This cannot be undone.")
                .setPositiveButton("Empty Trash", (d, w) -> emptyTrash())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void emptyTrash() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().emptyTrash()
                .enqueue(new Callback<MessageResponse>() {
                    @Override
                    public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(TrashActivity.this, "Trash emptied", Toast.LENGTH_SHORT).show();
                            loadTrash();
                        }
                    }

                    @Override
                    public void onFailure(Call<MessageResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(TrashActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

