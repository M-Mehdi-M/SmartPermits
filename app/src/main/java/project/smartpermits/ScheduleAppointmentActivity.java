package project.smartpermits;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.Appointment;
import project.smartpermits.models.AppointmentRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleAppointmentActivity extends AppCompatActivity {

    private TextInputEditText etDate, etNotes;
    private Spinner spinnerTimeSlot;
    private ProgressBar progressBar;
    private int permitId;

    private final String[] TIME_SLOTS = {
            "09:00 - 10:00", "10:00 - 11:00", "11:00 - 12:00",
            "13:00 - 14:00", "14:00 - 15:00", "15:00 - 16:00", "16:00 - 17:00"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_appointment);

        etDate = findViewById(R.id.etDate);
        etNotes = findViewById(R.id.etNotes);
        spinnerTimeSlot = findViewById(R.id.spinnerTimeSlot);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);
        MaterialButton btnSchedule = findViewById(R.id.btnSchedule);

        permitId = getIntent().getIntExtra("permit_id", -1);
        if (permitId == -1) { finish(); return; }

        spinnerTimeSlot.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TIME_SLOTS));

        etDate.setOnClickListener(v -> showDatePicker());
        etDate.setFocusable(false);

        btnBack.setOnClickListener(v -> finish());
        btnSchedule.setOnClickListener(v -> schedule());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(cal.getTimeInMillis());
        dialog.show();
    }

    private void schedule() {
        String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String timeSlot = spinnerTimeSlot.getSelectedItem().toString();
        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        if (date.isEmpty()) {
            etDate.setError("Required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi()
                .scheduleAppointment(permitId, new AppointmentRequest(date, timeSlot, notes))
                .enqueue(new Callback<Appointment>() {
                    @Override
                    public void onResponse(Call<Appointment> call, Response<Appointment> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            Toast.makeText(ScheduleAppointmentActivity.this, "Appointment scheduled!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(ScheduleAppointmentActivity.this, "Appointment already exists or permit not approved", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Appointment> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ScheduleAppointmentActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

