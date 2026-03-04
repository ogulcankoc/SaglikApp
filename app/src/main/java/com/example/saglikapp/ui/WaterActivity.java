package com.example.saglikapp.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider; // ViewModel bağlamak için eklendi

import com.example.saglikapp.R;

public class WaterActivity extends AppCompatActivity {

    // 1. ViewModel'imizi tanımlıyoruz
    private WaterViewModel viewModel;

    private ProgressBar progressBar;
    private TextView txtProgress;
    private EditText editWaterGoal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water);

        // 2. ViewModel'i Activity'ye bağlıyoruz
        viewModel = new ViewModelProvider(this).get(WaterViewModel.class);

        progressBar = findViewById(R.id.progressBarWater);
        txtProgress = findViewById(R.id.txtProgress);
        editWaterGoal = findViewById(R.id.editWaterGoal);

        Button btnUpdateGoal = findViewById(R.id.btnUpdateGoal);
        Button btnAdd200 = findViewById(R.id.btnAdd200);
        Button btnAdd500 = findViewById(R.id.btnAdd500);
        Button btnReset = findViewById(R.id.btnResetWater);

        // Uygulama açıldığında ekrandaki hedef kutusuna ViewModel'deki hedefi yazdırıyoruz
        editWaterGoal.setText(String.valueOf(viewModel.getDailyGoal()));

        // Arayüzü ilk açılışta güncelliyoruz
        updateUI();

        // 3. Buton tıklamalarını dinleyip işi ViewModel'e devrediyoruz
        btnUpdateGoal.setOnClickListener(v -> {
            String newGoalStr = editWaterGoal.getText().toString();
            if (!newGoalStr.isEmpty()) {
                int newGoal = Integer.parseInt(newGoalStr);
                viewModel.updateGoal(newGoal); // Veriyi ViewModel günceller
                updateUI(); // Arayüzü biz güncelleriz
                Toast.makeText(this, "Hedef Güncellendi!", Toast.LENGTH_SHORT).show();
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

    // 4. Arayüzü güncelleyen temizlenmiş metodumuz
    private void updateUI() {
        // Verileri sadece ViewModel'den çekiyoruz, Activity'de değişken tutmuyoruz
        int current = viewModel.getCurrentWater();
        int goal = viewModel.getDailyGoal();

        progressBar.setMax(goal);
        progressBar.setProgress(current);
        txtProgress.setText(current + " / " + goal + " ml");
    }
}