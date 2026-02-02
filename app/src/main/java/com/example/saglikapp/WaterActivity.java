package com.example.saglikapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WaterActivity extends AppCompatActivity {

    private SharedPreferences userPref;
    private SharedPreferences waterPref;

    private ProgressBar progressBar;
    private TextView txtProgress;
    private EditText editWaterGoal;

    private int currentWater = 0;
    private int dailyGoal = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water);

        userPref = getSharedPreferences("UserData", MODE_PRIVATE);
        waterPref = getSharedPreferences("WaterData", MODE_PRIVATE);

        progressBar = findViewById(R.id.progressBarWater);
        txtProgress = findViewById(R.id.txtProgress);
        editWaterGoal = findViewById(R.id.editWaterGoal);

        Button btnUpdateGoal = findViewById(R.id.btnUpdateGoal);
        Button btnAdd200 = findViewById(R.id.btnAdd200);
        Button btnAdd500 = findViewById(R.id.btnAdd500);
        Button btnReset = findViewById(R.id.btnResetWater);

        calculateAndSetGoal();

        currentWater = waterPref.getInt("today_water", 0);
        updateUI();

        btnUpdateGoal.setOnClickListener(v -> {
            String newGoalStr = editWaterGoal.getText().toString();
            if (!newGoalStr.isEmpty()) {
                dailyGoal = Integer.parseInt(newGoalStr);
                waterPref.edit().putInt("daily_goal", dailyGoal).apply();
                updateUI();
                Toast.makeText(this, "Hedef Güncellendi!", Toast.LENGTH_SHORT).show();
            }
        });

        btnAdd200.setOnClickListener(v -> addWater(200));
        btnAdd500.setOnClickListener(v -> addWater(500));

        btnReset.setOnClickListener(v -> {
            currentWater = 0;
            saveWater();
            updateUI();
        });
    }

    private void calculateAndSetGoal() {
        int savedGoal = waterPref.getInt("daily_goal", 0);
        if (savedGoal != 0) {
            dailyGoal = savedGoal;
        } else {
            String weightStr = userPref.getString("weight", "0");
            try {
                int weight = Integer.parseInt(weightStr);
                if (weight > 0) dailyGoal = weight * 33;
                else dailyGoal = 2500;
            } catch (NumberFormatException e) {
                dailyGoal = 2500;
            }
            waterPref.edit().putInt("daily_goal", dailyGoal).apply();
        }
        editWaterGoal.setText(String.valueOf(dailyGoal));
    }

    private void addWater(int amount) {
        currentWater += amount;
        saveWater();
        updateUI();
    }

    private void saveWater() {
        waterPref.edit().putInt("today_water", currentWater).apply();
    }

    private void updateUI() {
        progressBar.setMax(dailyGoal);
        progressBar.setProgress(currentWater);
        txtProgress.setText(currentWater + " / " + dailyGoal + " ml");
    }
}