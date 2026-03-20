package com.example.saglikapp.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.saglikapp.R;
import com.example.saglikapp.data.WaterDao;
import com.example.saglikapp.data.WaterDatabase;
import com.example.saglikapp.data.WaterLog;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WaterHistoryActivity extends AppCompatActivity {

    private BarChart barChart;
    private WaterDao waterDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_history);

        barChart = findViewById(R.id.barChart);
        Button btnBack = findViewById(R.id.btnBack);

        waterDao = WaterDatabase.getInstance(this).waterDao();

        setupChartAppearance(); // Grafiğin görsel ayarları
        loadChartData();        // Verilerin yüklenmesi

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupChartAppearance() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.getLegend().setEnabled(false);

        // --- ÖNEMLİ: Gün isimlerinin (Pzt, Sal vb.) sığması için alt boşluk ---
        barChart.setExtraBottomOffset(25f);

        barChart.setFitBars(true);
        barChart.setDrawValueAboveBar(true);

        // X Ekseni Ayarları
        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(11f);
        xAxis.setYOffset(5f); // Yazıyı eksen çizgisine yaklaştır (kesilmeyi önler)

        // --- ÖNEMLİ: Sütunların devleşmesini engellemek için 7 günlük alan ayırıyoruz ---
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(6.5f);

        // Sol Y Ekseni Ayarları
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setSpaceTop(25f); // Sütun üzerindeki rakamların tavan yapmaması için
        leftAxis.setGranularity(500f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);

        // Sağ Y Ekseni Kapat
        barChart.getAxisRight().setEnabled(false);
    }

    private void loadChartData() {
        executorService.execute(() -> {
            List<WaterLog> logs = waterDao.getLastSevenDays();

            // Verileri eskiden yeniye doğru sırala
            Collections.reverse(logs);

            ArrayList<BarEntry> entries = new ArrayList<>();
            final ArrayList<String> dayNames = new ArrayList<>();

            // Tarih formatlayıcılar
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", new Locale("tr"));

            for (int i = 0; i < logs.size(); i++) {
                entries.add(new BarEntry(i, logs.get(i).getTotalAmount()));

                // Tarihi gün ismine dönüştür (Pzt, Sal...)
                String dateStr = logs.get(i).getDate();
                try {
                    Date date = inputFormat.parse(dateStr);
                    String dayName = outputFormat.format(date);
                    dayNames.add(dayName);
                } catch (ParseException e) {
                    dayNames.add(dateStr);
                }
            }

            runOnUiThread(() -> {
                // X eksenine gün isimlerini yazdır
                barChart.getXAxis().setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        int index = (int) value;
                        if (index >= 0 && index < dayNames.size()) {
                            return dayNames.get(index);
                        }
                        return "";
                    }
                });

                BarDataSet dataSet = new BarDataSet(entries, "Su (ml)");
                dataSet.setColor(Color.parseColor("#2196F3")); // Su mavisi
                dataSet.setValueTextSize(11f);
                dataSet.setValueTextColor(Color.DKGRAY);

                // Sütunların üzerine değerlerini (ml) yazdır
                dataSet.setValueFormatter(new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return (int) value + " ml";
                    }
                });

                BarData data = new BarData(dataSet);
                data.setBarWidth(0.5f); // Sütun genişliğini sabitledik

                barChart.setData(data);
                barChart.animateY(1000);
                barChart.invalidate();
            });
        });
    }
}