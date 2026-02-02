package com.example.saglikapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class BmiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bmi);

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

        double hM = heightCm / 100.0;
        double bmi = weightKg / (hM * hM);
        double idealMin = 18.5 * hM * hM;
        double idealMax = 24.9 * hM * hM;

        String category;
        if (bmi < 18.5)       category = "Zayıf";
        else if (bmi < 25.0)  category = "Normal";
        else if (bmi < 30.0)  category = "Fazla kilolu";
        else                  category = "Obez";

        TextView tvSummary  = findViewById(R.id.tvSummary);
        TextView tvBmi      = findViewById(R.id.tvBmi);
        TextView tvIdeal    = findViewById(R.id.tvIdeal);
        TextView tvCategory = findViewById(R.id.tvCategory);

        tvSummary.setText(String.format(Locale.getDefault(),
                "%s (%s)\nBoy: %.0f cm   |   Kilo: %.1f kg",
                name, gender, heightCm, weightKg));
        tvBmi.setText(String.format(Locale.getDefault(), "VKİ: %.1f", bmi));
        tvIdeal.setText(String.format(Locale.getDefault(),
                "İdeal kilo aralığı: %.1f – %.1f kg", idealMin, idealMax));
        tvCategory.setText("Durum: " + category);
    }
}
