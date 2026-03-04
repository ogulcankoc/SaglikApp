package com.example.saglikapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider; // ViewModel bağlamak için eklendi

import com.example.saglikapp.R;

import java.util.Calendar;

public class SleepActivity extends AppCompatActivity {

    // 1. ViewModel'imizi tanımlıyoruz
    private SleepViewModel viewModel;

    private TextView textCurrentTime;
    private Button btnNap, btnCycle1, btnCycle2, btnCycle3, btnCycle4, btnCycle5, btnCycle6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep);

        // 2. ViewModel'i Activity'ye bağlıyoruz
        viewModel = new ViewModelProvider(this).get(SleepViewModel.class);

        textCurrentTime = findViewById(R.id.textCurrentTime);
        btnNap = findViewById(R.id.btnNap);
        btnCycle1 = findViewById(R.id.btnCycle1);
        btnCycle2 = findViewById(R.id.btnCycle2);
        btnCycle3 = findViewById(R.id.btnCycle3);
        btnCycle4 = findViewById(R.id.btnCycle4);
        btnCycle5 = findViewById(R.id.btnCycle5);
        btnCycle6 = findViewById(R.id.btnCycle6);

        // 3. Arayüzü ViewModel'den gelen hazır hesaplanmış verilerle güncelliyoruz
        textCurrentTime.setText("Şu anda saat: " + viewModel.getFormattedTime(viewModel.getNow()));

        btnNap.setText(viewModel.getFormattedTime(viewModel.getCycleTime(0)) + "\nKestirme 25 dk");
        btnCycle1.setText(viewModel.getFormattedTime(viewModel.getCycleTime(1)) + "\n1 döngü 1s 45dk");
        btnCycle2.setText(viewModel.getFormattedTime(viewModel.getCycleTime(2)) + "\n2 döngü 3s 15dk");
        btnCycle3.setText(viewModel.getFormattedTime(viewModel.getCycleTime(3)) + "\n3 döngü 4s 45dk");
        btnCycle4.setText(viewModel.getFormattedTime(viewModel.getCycleTime(4)) + "\n4 döngü 6s 15dk");
        btnCycle5.setText(viewModel.getFormattedTime(viewModel.getCycleTime(5)) + "\n5 döngü 7s 45dk");
        btnCycle6.setText(viewModel.getFormattedTime(viewModel.getCycleTime(6)) + "\n6 döngü 9s 15dk");

        // 4. Alarm kurma işlemlerini ViewModel'deki zamanlarla ayarlıyoruz
        setAlarmOnClick(btnNap, viewModel.getCycleTime(0), "Kestirme");
        setAlarmOnClick(btnCycle1, viewModel.getCycleTime(1), "1 uyku döngüsü");
        setAlarmOnClick(btnCycle2, viewModel.getCycleTime(2), "2 uyku döngüsü");
        setAlarmOnClick(btnCycle3, viewModel.getCycleTime(3), "3 uyku döngüsü");
        setAlarmOnClick(btnCycle4, viewModel.getCycleTime(4), "4 uyku döngüsü");
        setAlarmOnClick(btnCycle5, viewModel.getCycleTime(5), "5 uyku döngüsü");
        setAlarmOnClick(btnCycle6, viewModel.getCycleTime(6), "6 uyku döngüsü");
    }

    private void setAlarmOnClick(Button btn, Calendar time, String message) {
        btn.setOnClickListener(v -> setAlarm(time, message));
    }

    private void setAlarm(Calendar time, String message) {
        if (time == null) return; // Güvenlik kontrolü eklendi

        // Intent (Android sistemine istek atma) işlemleri Activity'de kalmalıdır
        Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
        intent.putExtra(AlarmClock.EXTRA_HOUR, time.get(Calendar.HOUR_OF_DAY));
        intent.putExtra(AlarmClock.EXTRA_MINUTES, time.get(Calendar.MINUTE));
        intent.putExtra(AlarmClock.EXTRA_MESSAGE, message);

        try {
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent showAlarms = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                startActivity(showAlarms);
                Toast.makeText(this,
                        "Alarm listesi açıldı. " + viewModel.getFormattedTime(time) + " saatine alarm kurun.",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Toast.makeText(this,
                        "Alarm kurulamadı. Saat: " + viewModel.getFormattedTime(time),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}