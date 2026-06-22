package com.example.saglikapp.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.example.saglikapp.R;
import com.example.saglikapp.data.AppDatabase;
import com.example.saglikapp.data.HeartRateLog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HeartRateHistoryActivity extends AppCompatActivity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_history);

        lineChart = findViewById(R.id.lineChart);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        setupChartAppearance();
        loadChartData();
    }

    private void setupChartAppearance() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getLegend().setEnabled(true);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.LTGRAY);
    }

    private void loadChartData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<HeartRateLog> logs = AppDatabase.getInstance(this).heartRateDao().getLastTenReadings();
            
            // Eskiden yeniye sırala
            Collections.reverse(logs);

            ArrayList<Entry> entries = new ArrayList<>();
            final ArrayList<String> dates = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());

            for (int i = 0; i < logs.size(); i++) {
                entries.add(new Entry(i, logs.get(i).getBpm()));
                dates.add(sdf.format(new Date(logs.get(i).getTimestamp())));
            }

            runOnUiThread(() -> {
                if (entries.isEmpty()) return;

                lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        int index = (int) value;
                        if (index >= 0 && index < dates.size()) {
                            return dates.get(index);
                        }
                        return "";
                    }
                });

                LineDataSet dataSet = new LineDataSet(entries, "Kalp Ritmi (BPM)");
                dataSet.setColor(Color.RED);
                dataSet.setCircleColor(Color.RED);
                dataSet.setLineWidth(2f);
                dataSet.setCircleRadius(4f);
                dataSet.setDrawCircleHole(true);
                dataSet.setValueTextSize(10f);
                dataSet.setDrawFilled(true);
                dataSet.setFillColor(Color.RED);
                dataSet.setFillAlpha(30);
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

                LineData lineData = new LineData(dataSet);
                lineChart.setData(lineData);
                lineChart.animateX(1000);
                lineChart.invalidate();
            });
        });
    }
}
