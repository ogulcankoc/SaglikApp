package com.example.saglikapp.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.R;
import com.example.saglikapp.data.AppDatabase;
import com.example.saglikapp.data.WaterLog;
import com.example.saglikapp.receiver.AlarmReceiver;
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

public class WaterFragment extends Fragment {

    private WaterViewModel viewModel;
    private ProgressBar progressBar;
    private TextView txtProgress;
    private TextView txtProgressPercent;
    private TextView txtGoalReached;
    private EditText editWaterGoal;
    private EditText editCustomWater;

    private BarChart summaryChart;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_water, container, false);

        // Activity scope'unda ViewModel'i alıyoruz ki HomeFragment ile aynı veriyi paylaşsın
        if (getActivity() != null) {
            viewModel = new ViewModelProvider(getActivity()).get(WaterViewModel.class);
        } else {
            viewModel = new ViewModelProvider(this).get(WaterViewModel.class);
        }

        progressBar = view.findViewById(R.id.progressBarWater);
        txtProgress = view.findViewById(R.id.txtProgress);
        txtProgressPercent = view.findViewById(R.id.txtProgressPercent);
        txtGoalReached = view.findViewById(R.id.txtGoalReached);
        editWaterGoal = view.findViewById(R.id.editWaterGoal);
        editCustomWater = view.findViewById(R.id.editCustomWater);
        summaryChart = view.findViewById(R.id.summaryChart);

        Button btnUpdateGoal = view.findViewById(R.id.btnUpdateGoal);
        Button btnAdd200 = view.findViewById(R.id.btnAdd200);
        Button btnAdd500 = view.findViewById(R.id.btnAdd500);
        Button btnReset = view.findViewById(R.id.btnResetWater);
        Button btnShowHistory = view.findViewById(R.id.btnShowHistory);
        Button btnAddCustom = view.findViewById(R.id.btnAddCustom);

        editWaterGoal.setText(String.valueOf(viewModel.getDailyGoal()));

        setupSummaryChart();

        // LiveData Gözlemleme
        viewModel.getCurrentWaterLiveData().observe(getViewLifecycleOwner(), current -> {
            updateUI();
            loadSummaryData();
        });

        viewModel.getDailyGoalLiveData().observe(getViewLifecycleOwner(), goal -> {
            updateUI();
            loadSummaryData();
        });

        btnUpdateGoal.setOnClickListener(v -> {
            String newGoalStr = editWaterGoal.getText().toString().trim();
            if (!newGoalStr.isEmpty()) {
                try {
                    int newGoal = Integer.parseInt(newGoalStr);
                    if (newGoal > 0) {
                        viewModel.updateGoal(newGoal);
                        Toast.makeText(getContext(), "Hedef Güncellendi!", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Geçerli bir sayı girin.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAdd200.setOnClickListener(v -> {
            viewModel.addWater(200);
            rescheduleAlarm();
        });

        btnAdd500.setOnClickListener(v -> {
            viewModel.addWater(500);
            rescheduleAlarm();
        });

        btnAddCustom.setOnClickListener(v -> {
            String amountStr = editCustomWater.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    int amount = Integer.parseInt(amountStr);
                    if (amount > 0) {
                        viewModel.addWater(amount);
                        rescheduleAlarm();
                        editCustomWater.setText("");
                        Toast.makeText(getContext(), amount + " ml eklendi!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Lütfen 0'dan büyük bir değer girin.", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Geçerli bir sayı girin.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext(), "Lütfen miktar girin.", Toast.LENGTH_SHORT).show();
            }
        });

        btnReset.setOnClickListener(v -> {
            viewModel.resetWater();
            Toast.makeText(getContext(), "Veriler Sıfırlandı", Toast.LENGTH_SHORT).show();
        });

        btnShowHistory.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), WaterHistoryActivity.class);
            startActivity(intent);
        });

        return view;
    }

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
        xAxis.setAxisMaximum(6.5f);

        summaryChart.getAxisRight().setEnabled(false);
        summaryChart.getAxisLeft().setAxisMinimum(0f);
        summaryChart.getAxisLeft().setDrawGridLines(true);
    }

    private void loadSummaryData() {
        executorService.execute(() -> {
            if (getContext() == null) return;
            AppDatabase db = AppDatabase.getInstance(getContext());
            List<WaterLog> logs = db.waterDao().getLastSevenDays();
            Collections.reverse(logs);

            ArrayList<BarEntry> entries = new ArrayList<>();
            final ArrayList<String> dayNames = new ArrayList<>();
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE", new Locale("tr"));

            float maxAmount = 0f;
            for (int i = 0; i < logs.size(); i++) {
                int amount = logs.get(i).getTotalAmount();
                entries.add(new BarEntry(i, amount));
                if (amount > maxAmount) {
                    maxAmount = amount;
                }
                try {
                    Date date = inputFormat.parse(logs.get(i).getDate());
                    if (date != null) {
                        dayNames.add(outputFormat.format(date));
                    } else {
                        dayNames.add("");
                    }
                } catch (Exception e) {
                    dayNames.add("");
                }
            }
            final float finalMaxAmount = maxAmount;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    int goal = viewModel.getDailyGoal();
                    float yMax = Math.max((float) goal, finalMaxAmount);
                    
                    com.github.mikephil.charting.components.YAxis leftAxis = summaryChart.getAxisLeft();
                    leftAxis.setAxisMaximum(yMax);
                    leftAxis.setGranularity(200f);
                    
                    int labelCount = (int) (yMax / 200f) + 1;
                    leftAxis.setLabelCount(Math.min(labelCount, 15), false);

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
                    dataSet.setColor(Color.parseColor("#42A5F5"));
                    dataSet.setDrawValues(false);

                    BarData data = new BarData(dataSet);
                    data.setBarWidth(0.5f);
                    summaryChart.setData(data);
                    summaryChart.animateY(800);
                    summaryChart.invalidate();
                });
            }
        });
    }

    private void updateUI() {
        if (txtProgress == null || progressBar == null) return;
        
        int current = viewModel.getCurrentWater();
        int goal = viewModel.getDailyGoal();

        progressBar.setMax(goal);
        progressBar.setProgress(current);

        String progressText = current + " / " + goal + " ml";
        txtProgress.setText(progressText);

        if (txtProgressPercent != null) {
            int percent = (goal > 0) ? (current * 100 / goal) : 0;
            txtProgressPercent.setText("%" + percent);
        }

        if (current >= goal && goal > 0) {
            if (txtGoalReached != null) {
                txtGoalReached.setVisibility(View.VISIBLE);
            }
        } else {
            if (txtGoalReached != null) {
                txtGoalReached.setVisibility(View.GONE);
            }
        }
    }

    private void rescheduleAlarm() {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), AlarmReceiver.class);
            intent.putExtra("type", "reschedule");
            getContext().sendBroadcast(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}