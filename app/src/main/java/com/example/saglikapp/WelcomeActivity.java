package com.example.saglikapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        SharedPreferences sharedPref = getSharedPreferences("UserData", MODE_PRIVATE);

        String name = sharedPref.getString("name", "Kullanıcı");
        String age = sharedPref.getString("age", "-");
        String weight = sharedPref.getString("weight", "-");
        String height = sharedPref.getString("height", "-");
        String gender = sharedPref.getString("gender", "-");

        TextView textWelcome = findViewById(R.id.textWelcome);
        String message = name + " hoşgeldin\n\n"
                + "Yaş: " + age + "\n"
                + "Kilo: " + weight + " kg\n"
                + "Boy: " + height + " cm\n"
                + "Cinsiyet: " + gender;
        textWelcome.setText(message);

        // --- Butonlar ---
        Button btnBmi = findViewById(R.id.btnBmi);
        Button btnSleep = findViewById(R.id.btnSleep);
        Button btnWater = findViewById(R.id.btnWater);

        // Vücut Kitle Endeksi butonuna tıklayınca BmiActivity açılacak
        btnBmi.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, BmiActivity.class);
            startActivity(intent);
        });

        // Uyku Döngüsü Alarm butonuna tıklayınca SleepActivity açılacak
        btnSleep.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, SleepActivity.class);
            startActivity(intent);

        });

        //su icme hatırlatma
        btnWater.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, WaterActivity.class);
            startActivity(intent);
        });


        ImageButton btnProfile = findViewById(R.id.btnProfile);
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });


    }
}
