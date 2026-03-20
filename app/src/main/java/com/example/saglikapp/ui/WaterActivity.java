package com.example.saglikapp.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.R;
import com.example.saglikapp.data.WaterDatabase;
import com.example.saglikapp.data.WaterLog;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WaterActivity extends AppCompatActivity {

    private WaterViewModel viewModel;
    private ProgressBar progressBar;
    private TextView txtProgress;
    private EditText editWaterGoal;

    // Özet Grafik Değişkenleri
    private BarChart summaryChart;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water);

        // ViewModel Bağlantısı
        viewModel = new ViewModelProvider(this).get(WaterViewModel.class);

        // UI Bileşenlerini Tanımlama
        progressBar = findViewById(R.id.progressBarWater);
        txtProgress = findViewById(R.id.txtProgress);
        editWaterGoal = findViewById(R.id.editWaterGoal);
        summaryChart = findViewById(R.id.summaryChart); // XML'e eklediğimiz grafik

        Button btnUpdateGoal = findViewById(R.id.btnUpdateGoal);
        Button btnAdd200 = findViewById(R.id.btnAdd200);
        Button btnAdd500 = findViewById(R.id.btnAdd500);
        Button btnReset = findViewById(R.id.btnResetWater);
        Button btnShowHistory = findViewById(R.id.btnShowHistory);

        // Başlangıç değerini set et
        editWaterGoal.setText(String.valueOf(viewModel.getDailyGoal()));

        // Grafiği Hazırla
        setupSummaryChart();

        // --- TIKLAMA OLAYLARI ---

        btnUpdateGoal.setOnClickListener(v -> {
            String newGoalStr = editWaterGoal.getText().toString().trim();
            if (!newGoalStr.isEmpty()) {
                try {
                    int newGoal = Integer.parseInt(newGoalStr);
                    if (newGoal > 0) {
                        viewModel.updateGoal(newGoal);
                        updateUI();
                        Toast.makeText(this, "Hedef Güncellendi!", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Geçerli bir sayı girin.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAdd200.setOnClickListener(v -> {
            viewModel.addWater(200);
            updateUI();
            loadSummaryData(); // Su ekleyince grafiği tazele
        });

        btnAdd500.setOnClickListener(v -> {
            viewModel.addWater(500);
            updateUI();
            loadSummaryData(); // Su ekleyince grafiği tazele
        });

        btnReset.setOnClickListener(v -> {
            viewModel.resetWater();
            updateUI();
            loadSummaryData(); // Sıfırlayınca grafiği tazele
            Toast.makeText(this, "Veriler Sıfırlandı", Toast.LENGTH_SHORT).show();
        });

        btnShowHistory.setOnClickListener(v -> {
            Intent intent = new Intent(WaterActivity.this, WaterHistoryActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        loadSummaryData(); // Ekran her açıldığında grafiği güncelle
    }

    // Grafiğin Görsel Ayarları
    private void setupSummaryChart() {
        summaryChart.getDescription().setEnabled(false);
        summaryChart.getLegend().setEnabled(false);
        summaryChart.setExtraBottomOffset(15f);
        summaryChart.setDrawValueAboveBar(true);

        XAxis xAxis = summaryChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(6.5f); // 7 günlük yer ayır

        summaryChart.getAxisRight().setEnabled(false);
        summaryChart.getAxisLeft().setAxisMinimum(0f);
        summaryChart.getAxisLeft().setDrawGridLines(true);
    }

    // Verileri Room'dan Çekip Grafiğe Basma
    private void loadSummaryData() {
        executorService.execute(() -> {
            WaterDatabase db = WaterDatabase.getInstance(this);
            List<WaterLog> logs = db.waterDao().getLastSevenDays();
            Collections.reverse(logs);

            ArrayList<BarEntry> entries = new ArrayList<>();
            final ArrayList<String> dayNames = new ArrayList<>();
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", new Locale("tr"));

            for (int i = 0; i < logs.size(); i++) {
                entries.add(new BarEntry(i, logs.get(i).getTotalAmount()));
                try {
                    Date date = inputFormat.parse(logs.get(i).getDate());
                    dayNames.add(outputFormat.format(date));
                } catch (Exception e) {
                    dayNames.add("");
                }
            }

            runOnUiThread(() -> {
                summaryChart.getXAxis().setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        int index = (int) value;
                        if (index >= 0 && index < dayNames.size()) {
                            return dayNames.get(index);
                        }
                        return "";
                    }
                });

                BarDataSet dataSet = new BarDataSet(entries, "");
                dataSet.setColor(Color.parseColor("#42A5F5")); // Açık mavi
                dataSet.setDrawValues(false); // Ana ekranda kalabalık olmasın diye rakamları gizledik

                BarData data = new BarData(dataSet);
                data.setBarWidth(0.5f);
                summaryChart.setData(data);
                summaryChart.animateY(800);
                summaryChart.invalidate();
            });
        });
    }

    private void updateUI() {
        int current = viewModel.getCurrentWater();
        int goal = viewModel.getDailyGoal();

        progressBar.setMax(goal);
        progressBar.setProgress(current);

        String progressText;
        try {
            progressText = getString(R.string.water_progress_format, current, goal);
        } catch (Exception e) {
            progressText = current + " / " + goal + " ml";
        }
        txtProgress.setText(progressText);

        if (current >= goal && goal > 0) {
            String goalMsg;
            try {
                goalMsg = "\n" + getString(R.string.goal_reached_msg);
            } catch (Exception e) {
                goalMsg = "\nHarika! Günlük hedefe ulaştın.";
            }
            txtProgress.append(goalMsg);
        }
    }
}