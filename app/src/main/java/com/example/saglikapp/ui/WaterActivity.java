package com.example.saglikapp.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.R;

public class WaterActivity extends AppCompatActivity {

    private WaterViewModel viewModel;
    private ProgressBar progressBar;
    private TextView txtProgress;
    private EditText editWaterGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water);

        viewModel = new ViewModelProvider(this).get(WaterViewModel.class);

        progressBar = findViewById(R.id.progressBarWater);
        txtProgress = findViewById(R.id.txtProgress);
        editWaterGoal = findViewById(R.id.editWaterGoal);

        Button btnUpdateGoal = findViewById(R.id.btnUpdateGoal);
        Button btnAdd200 = findViewById(R.id.btnAdd200);
        Button btnAdd500 = findViewById(R.id.btnAdd500);
        Button btnReset = findViewById(R.id.btnResetWater);

        // Başlangıç değerini set et
        editWaterGoal.setText(String.valueOf(viewModel.getDailyGoal()));

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
                    Toast.makeText(this, "Lütfen geçerli bir sayı girin.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAdd200.setOnClickListener(v -> {
            viewModel.addWater(200);
            updateUI();
        });

        btnAdd500.setOnClickListener(v -> {
            viewModel.addWater(500);
            updateUI();
        });

        btnReset.setOnClickListener(v -> {
            viewModel.resetWater();
            updateUI();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Uygulama arka plandan öne geldiğinde (veya gün değiştiğinde) UI'ı tazele
        updateUI();
    }

    private void updateUI() {
        int current = viewModel.getCurrentWater();
        int goal = viewModel.getDailyGoal();

        // ProgressBar'ın max değerini güncelle
        progressBar.setMax(goal);
        progressBar.setProgress(current);

        // UYARIYI DÜZELTEN KISIM:
        // getString(R.string.id, arg1, arg2) formatı en sağlıklı yöntemdir.
        String progressText = getString(R.string.water_progress_format, current, goal);
        txtProgress.setText(progressText);

        // Hedefe ulaşıldıysa görsel geri bildirim
        if (current >= goal && goal > 0) {
            txtProgress.append(getString(R.string.goal_reached_msg));
        }
    }
}