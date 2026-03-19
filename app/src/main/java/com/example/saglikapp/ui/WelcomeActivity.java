package com.example.saglikapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.R;

public class WelcomeActivity extends AppCompatActivity {

    private WelcomeViewModel viewModel;
    private TextView textWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        viewModel = new ViewModelProvider(this).get(WelcomeViewModel.class);
        textWelcome = findViewById(R.id.textWelcome);

        // Buton Tanımlamaları
        Button btnBmi = findViewById(R.id.btnBmi);
        Button btnSleep = findViewById(R.id.btnSleep);
        Button btnWater = findViewById(R.id.btnWater);
        ImageButton btnProfile = findViewById(R.id.btnProfile);

        // Tıklama Olayları
        btnBmi.setOnClickListener(v -> startActivity(new Intent(this, BmiActivity.class)));
        btnSleep.setOnClickListener(v -> startActivity(new Intent(this, SleepActivity.class)));
        btnWater.setOnClickListener(v -> startActivity(new Intent(this, WaterActivity.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Profil sayfasından geri dönüldüğünde bilgilerin güncellenmesi için
        // mesajı burada set ediyoruz.
        if (viewModel != null && textWelcome != null) {
            textWelcome.setText(viewModel.getWelcomeMessage());
        }
    }
}