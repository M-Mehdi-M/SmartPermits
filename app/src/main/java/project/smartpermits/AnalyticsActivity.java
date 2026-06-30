package project.smartpermits;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.renderer.BarChartRenderer;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
                        Toast.makeText(AnalyticsActivity.this, getString(R.string.error_generic, t.getMessage()), Toast.LENGTH_LONG).show();
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
            int nightMode = getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            boolean isDark = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            int textColor = isDark ? Color.parseColor("#E5E7EB") : Color.parseColor("#374151");

            List<BarEntry> barEntries = new ArrayList<>();
            final List<String> wrappedLabels = new ArrayList<>();
            int i = 0;
            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                barEntries.add(new BarEntry(i, entry.getValue()));
                wrappedLabels.add(wrapLabel(entry.getKey()));
                i++;
            }

            BarDataSet barDataSet = new BarDataSet(barEntries, "");
            barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
            barDataSet.setValueTextSize(12f);
            barDataSet.setValueTextColor(textColor);
            barDataSet.setDrawValues(true);
            // Draw the permit type name above each bar, stacked over its value.
            barDataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getBarLabel(BarEntry entry) {
                    int index = Math.round(entry.getX());
                    String name = (index >= 0 && index < wrappedLabels.size())
                            ? wrappedLabels.get(index) : "";
                    String val = String.format(Locale.US, "%d", Math.round(entry.getY()));
                    return name.isEmpty() ? val : name + "\n" + val;
                }
            });

            BarData barData = new BarData(barDataSet);
            barData.setBarWidth(0.6f);

            barChart.setRenderer(new MultiLineValueRenderer(
                    barChart, barChart.getAnimator(), barChart.getViewPortHandler()));
            barChart.setData(barData);

            // Type names now sit on top of the bars, so the bottom axis labels
            // are no longer needed.
            XAxis xAxis = barChart.getXAxis();
            xAxis.setEnabled(false);
            xAxis.setDrawGridLines(false);

            barChart.getAxisLeft().setTextColor(textColor);
            barChart.getAxisLeft().setAxisMinimum(0f);
            barChart.getAxisLeft().setDrawGridLines(false);
            barChart.getAxisRight().setEnabled(false);
            barChart.getDescription().setEnabled(false);
            barChart.getLegend().setEnabled(false);
            barChart.setFitBars(true);
            // Headroom for the multi-line labels drawn above the tallest bar.
            barChart.setExtraTopOffset(56f);
            barChart.setExtraBottomOffset(8f);
            barChart.setExtraLeftOffset(16f);
            barChart.setExtraRightOffset(16f);
            barChart.animateY(1000);
            barChart.invalidate();
        }
    }

    private String wrapLabel(String label) {
        if (label == null) return "";
        String trimmed = label.trim();
        String[] words = trimmed.split("\\s+");
        if (words.length < 2) return trimmed;

        int bestSplit = 1;
        int bestDiff = Integer.MAX_VALUE;
        for (int split = 1; split < words.length; split++) {
            int firstLen = 0;
            for (int j = 0; j < split; j++) firstLen += words[j].length() + 1;
            int secondLen = 0;
            for (int j = split; j < words.length; j++) secondLen += words[j].length() + 1;
            int diff = Math.abs(firstLen - secondLen);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestSplit = split;
            }
        }

        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        for (int j = 0; j < words.length; j++) {
            StringBuilder target = j < bestSplit ? first : second;
            if (target.length() > 0) target.append(' ');
            target.append(words[j]);
        }
        return first + "\n" + second;
    }

    /**
     * Renders bar values as multi-line text (permit type name on top, numeric
     * value at the bottom) drawn above each bar.
     */
    private static class MultiLineValueRenderer extends BarChartRenderer {

        MultiLineValueRenderer(com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider chart,
                               ChartAnimator animator, ViewPortHandler viewPortHandler) {
            super(chart, animator, viewPortHandler);
        }

        @Override
        public void drawValue(Canvas c, String valueText, float x, float y, int color) {
            String[] lines = valueText.split("\n");
            mValuePaint.setColor(color);
            Paint.Align oldAlign = mValuePaint.getTextAlign();
            mValuePaint.setTextAlign(Paint.Align.CENTER);
            float lineHeight = mValuePaint.getTextSize() + Utils.convertDpToPixel(3f);
            // The last line (the value) sits at y, just above the bar; earlier
            // lines (the type name) stack upward above it.
            for (int i = 0; i < lines.length; i++) {
                float lineY = y - (lines.length - 1 - i) * lineHeight;
                c.drawText(lines[i], x, lineY, mValuePaint);
            }
            mValuePaint.setTextAlign(oldAlign);
        }
    }
}

