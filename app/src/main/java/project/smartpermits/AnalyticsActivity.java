package project.smartpermits;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import project.smartpermits.api.RetrofitClient;
import project.smartpermits.models.AnalyticsResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(project.smartpermits.LocaleHelper.applyLocale(newBase));
    }

    private PieChart pieChart;
    private BarChart barChart;
    private TextView tvTotalReviewed, tvApproved, tvRejected, tvPending, tvAvgTime;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        tvTotalReviewed = findViewById(R.id.tvTotalReviewed);
        tvApproved = findViewById(R.id.tvApproved);
        tvRejected = findViewById(R.id.tvRejected);
        tvPending = findViewById(R.id.tvPending);
        tvAvgTime = findViewById(R.id.tvAvgTime);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        loadAnalytics();
    }

    private void loadAnalytics() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getInstance(this).getApi().getAnalytics()
                .enqueue(new Callback<AnalyticsResponse>() {
                    @Override
                    public void onResponse(Call<AnalyticsResponse> call, Response<AnalyticsResponse> response) {
                        progressBar.setVisibility(View.GONE);
                        if (response.isSuccessful() && response.body() != null) {
                            displayAnalytics(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<AnalyticsResponse> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AnalyticsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void displayAnalytics(AnalyticsResponse data) {
        tvTotalReviewed.setText(String.valueOf(data.getTotalReviewed()));
        tvApproved.setText(String.valueOf(data.getTotalApproved()));
        tvRejected.setText(String.valueOf(data.getTotalRejected()));
        tvPending.setText(String.valueOf(data.getTotalPending()));

        double hours = data.getAvgProcessingHours();
        if (hours < 24) {
            tvAvgTime.setText(String.format("%.1f hours", hours));
        } else {
            tvAvgTime.setText(String.format("%.1f days", hours / 24));
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        if (data.getTotalApproved() > 0) pieEntries.add(new PieEntry(data.getTotalApproved(), "Approved"));
        if (data.getTotalRejected() > 0) pieEntries.add(new PieEntry(data.getTotalRejected(), "Rejected"));
        if (data.getTotalPending() > 0) pieEntries.add(new PieEntry(data.getTotalPending(), "Pending"));

        if (!pieEntries.isEmpty()) {
            PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
            pieDataSet.setColors(new int[]{Color.parseColor("#16A34A"), Color.parseColor("#DC2626"), Color.parseColor("#E68A00")});
            pieDataSet.setValueTextSize(14f);
            pieDataSet.setValueTextColor(Color.WHITE);
            pieChart.setData(new PieData(pieDataSet));
            pieChart.setUsePercentValues(true);
            pieChart.getDescription().setEnabled(false);
            pieChart.setHoleRadius(40f);
            pieChart.setTransparentCircleRadius(45f);
            pieChart.setCenterText("Status");
            pieChart.setCenterTextSize(16f);
            pieChart.animateY(1000);
            pieChart.invalidate();
        }

        Map<String, Integer> typeCounts = data.getPermitTypeCounts();
        if (typeCounts != null && !typeCounts.isEmpty()) {
            List<BarEntry> barEntries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int i = 0;
            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                barEntries.add(new BarEntry(i, entry.getValue()));
                String label = entry.getKey();
                if (label.length() > 12) label = label.substring(0, 12) + "..";
                labels.add(label);
                i++;
            }
            BarDataSet barDataSet = new BarDataSet(barEntries, "Permits by Type");
            barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            barDataSet.setValueTextSize(12f);
            barChart.setData(new BarData(barDataSet));
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
            barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
            barChart.getXAxis().setGranularity(1f);
            barChart.getXAxis().setLabelRotationAngle(-30f);
            barChart.getDescription().setEnabled(false);
            barChart.getAxisRight().setEnabled(false);
            barChart.animateY(1000);
            barChart.invalidate();
        }
    }
}

