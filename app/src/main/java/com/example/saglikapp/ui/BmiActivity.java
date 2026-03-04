package com.example.saglikapp.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider; // ViewModel'i bağlamak için gerekli

import com.example.saglikapp.R;

import java.util.Locale;

public class BmiActivity extends AppCompatActivity {

    // ViewModel'imizi tanımlıyoruz
    private BmiViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi);

        // 1. ViewModel'i Activity'ye bağlıyoruz
        viewModel = new ViewModelProvider(this).get(BmiViewModel.class);

        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);

        String name     = prefs.getString("name", "Kullanıcı");
        String gender   = prefs.getString("gender", "-");
        String heightS  = prefs.getString("height", "0");
        String weightS  = prefs.getString("weight", "0");

        double heightCm, weightKg;
        try {
            heightCm = Double.parseDouble(heightS);
            weightKg = Double.parseDouble(weightS);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Kayıtlı boy/kilo bilgisi hatalı.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (heightCm <= 0 || weightKg <= 0) {
            Toast.makeText(this, "Boy ve kilo bilgisi eksik.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. HESAPLAMA İŞİNİ VİEWMODEL'E DEVREDİYORUZ (Business Logic Activity'den çıktı!)
        viewModel.calculateBmi(heightCm, weightKg);

        TextView tvSummary  = findViewById(R.id.tvSummary);
        TextView tvBmi      = findViewById(R.id.tvBmi);
        TextView tvIdeal    = findViewById(R.id.tvIdeal);
        TextView tvCategory = findViewById(R.id.tvCategory);

        // 3. Sonuçları ViewModel'den alıp ekrana yazdırıyoruz
        tvSummary.setText(String.format(Locale.getDefault(),
                "%s (%s)\nBoy: %.0f cm   |   Kilo: %.1f kg",
                name, gender, heightCm, weightKg));

        tvBmi.setText(String.format(Locale.getDefault(), "VKİ: %.1f", viewModel.getBmi()));

        tvIdeal.setText(String.format(Locale.getDefault(),
                "İdeal kilo aralığı: %.1f – %.1f kg", viewModel.getIdealMin(), viewModel.getIdealMax()));

        tvCategory.setText("Durum: " + viewModel.getCategory());
    }
}