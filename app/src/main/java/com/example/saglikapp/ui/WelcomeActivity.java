package com.example.saglikapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider; // ViewModel bağlamak için eklendi

import com.example.saglikapp.R;

public class WelcomeActivity extends AppCompatActivity {

    // 1. ViewModel'imizi tanımlıyoruz
    private WelcomeViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // 2. ViewModel'i Activity'ye bağlıyoruz
        viewModel = new ViewModelProvider(this).get(WelcomeViewModel.class);

        TextView textWelcome = findViewById(R.id.textWelcome);

        // 3. Karmaşık veri okuma işlemi yerine doğrudan ViewModel'den hazır mesajı alıyoruz
        textWelcome.setText(viewModel.getWelcomeMessage());

        // --- Yönlendirme Butonları ---
        Button btnBmi = findViewById(R.id.btnBmi);
        Button btnSleep = findViewById(R.id.btnSleep);
        Button btnWater = findViewById(R.id.btnWater);
        ImageButton btnProfile = findViewById(R.id.btnProfile);

        // Sayfa geçişleri (Intent) birer UI işlemidir, bu yüzden Activity'de kalması doğrudur.
        // Kodu biraz daha sadeleştirdik:
        btnBmi.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, BmiActivity.class));
        });

        btnSleep.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, SleepActivity.class));
        });

        btnWater.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, WaterActivity.class));
        });

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, ProfileActivity.class));
        });
    }
}