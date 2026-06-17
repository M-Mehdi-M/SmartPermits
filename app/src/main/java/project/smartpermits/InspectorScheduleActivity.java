package project.smartpermits;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import project.smartpermits.adapters.AppointmentAdapter;
import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.Appointment;
import project.smartpermits.models.AppointmentRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InspectorScheduleActivity extends AppCompatActivity implements AppointmentAdapter.OnAppointmentActionListener {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    private RecyclerView recyclerSchedule;
    private AppointmentAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyView;
    private ProgressBar progressBar;
    private TextView tvTodayCount, tvUpcomingCount, tvTotalCount;

    private List<Appointment> allAppointments = new ArrayList<>();
    private BroadcastReceiver appointmentReceiver;
    private boolean receiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspector_schedule);

        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnRefresh = findViewById(R.id.btnRefresh);
        recyclerSchedule = findViewById(R.id.recyclerSchedule);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyView = findViewById(R.id.emptyView);
        progressBar = findViewById(R.id.progressBar);
        tvTodayCount = findViewById(R.id.tvTodayCount);
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount);
        tvTotalCount = findViewById(R.id.tvTotalCount);

        adapter = new AppointmentAdapter(this);
        recyclerSchedule.setLayoutManager(new LinearLayoutManager(this));
        recyclerSchedule.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.accent);
        swipeRefresh.setOnRefreshListener(this::loadSchedule);

        btnBack.setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> loadSchedule());

        appointmentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("project.smartpermits.APPOINTMENT_SCHEDULED".equals(intent.getAction())) {
                    loadSchedule();
                }
            }
        };

        loadSchedule();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!receiverRegistered) {
            IntentFilter filter = new IntentFilter("project.smartpermits.APPOINTMENT_SCHEDULED");
            ContextCompat.registerReceiver(this, appointmentReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            receiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistered) {
            unregisterReceiver(appointmentReceiver);
            receiverRegistered = false;
        }
    }

    private void loadSchedule() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().getAppointments()
                .enqueue(new Callback<List<Appointment>>() {
                    @Override
                    public void onResponse(Call<List<Appointment>> call, Response<List<Appointment>> response) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            allAppointments = response.body();
                            buildScheduleList();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Appointment>> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        Toast.makeText(InspectorScheduleActivity.this, "Failed to load schedule", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void buildScheduleList() {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        String tomorrowStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

        int todayCount = 0;
        int upcomingCount = 0;

        Map<String, List<Appointment>> grouped = new LinkedHashMap<>();
        for (Appointment appt : allAppointments) {
            String date = appt.getDate();
            if (date == null) continue;
            if (!grouped.containsKey(date)) grouped.put(date, new ArrayList<>());
            grouped.get(date).add(appt);
            if (date.equals(todayStr)) todayCount++;
            else upcomingCount++;
        }

        tvTodayCount.setText(String.valueOf(todayCount));
        tvUpcomingCount.setText(String.valueOf(upcomingCount));
        tvTotalCount.setText(String.valueOf(allAppointments.size()));

        List<Object> flatList = new ArrayList<>();
        for (Map.Entry<String, List<Appointment>> entry : grouped.entrySet()) {
            String date = entry.getKey();
            List<Appointment> dayAppts = entry.getValue();
            String label = formatDateLabel(date, todayStr, tomorrowStr);
            int count = dayAppts.size();
            flatList.add(label + "|" + count + (count == 1 ? " inspection" : " inspections"));
            flatList.addAll(dayAppts);
        }

        adapter.setItems(flatList);

        if (flatList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerSchedule.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerSchedule.setVisibility(View.VISIBLE);
        }
    }

    private String formatDateLabel(String dateStr, String today, String tomorrow) {
        if (dateStr.equals(today)) return "TODAY — " + formatReadable(dateStr);
        if (dateStr.equals(tomorrow)) return "TOMORROW — " + formatReadable(dateStr);
        return formatReadable(dateStr).toUpperCase(Locale.getDefault());
    }

    private String formatReadable(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            SimpleDateFormat out = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
            return out.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    @Override
    public void onViewPermit(Appointment appointment) {
        Intent intent = new Intent(this, PermitReviewActivity.class);
        intent.putExtra("permit_id", appointment.getPermitId());
        startActivity(intent);
    }

    @Override
    public void onUpdateStatus(Appointment appointment) {
        String current = appointment.getStatus() != null ? appointment.getStatus() : "scheduled";
        String[] options;
        if ("scheduled".equals(current)) {
            options = new String[]{"✅ Confirm", "✔️ Mark Completed", "❌ Cancel"};
        } else {
            options = new String[]{"✔️ Mark Completed", "❌ Cancel"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Update Appointment Status")
                .setItems(options, (dialog, which) -> {
                    String newStatus;
                    if ("scheduled".equals(current)) {
                        switch (which) {
                            case 0: newStatus = "confirmed"; break;
                            case 1: newStatus = "completed"; break;
                            default: newStatus = "cancelled"; break;
                        }
                    } else {
                        newStatus = which == 0 ? "completed" : "cancelled";
                    }
                    updateAppointmentStatus(appointment.getId(), newStatus);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateAppointmentStatus(int apptId, String newStatus) {
        AppointmentRequest req = new AppointmentRequest(null, null, newStatus, null);
        RetrofitClient.getInstance(this).getApi().updateAppointment(apptId, req)
                .enqueue(new Callback<Appointment>() {
                    @Override
                    public void onResponse(Call<Appointment> call, Response<Appointment> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(InspectorScheduleActivity.this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                            loadSchedule();
                        } else {
                            Toast.makeText(InspectorScheduleActivity.this, "Failed to update", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Appointment> call, Throwable t) {
                        Toast.makeText(InspectorScheduleActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

