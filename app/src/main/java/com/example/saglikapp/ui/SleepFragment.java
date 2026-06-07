package com.example.saglikapp.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.R;

import java.util.Calendar;

public class SleepFragment extends Fragment {

    private SleepViewModel viewModel;
    private TextView textCurrentTime;
    private TextView textNapTime, textCycle1Time, textCycle2Time, textCycle3Time, textCycle4Time, textCycle5Time, textCycle6Time;
    private TextView textNapLabel, textCycle1Label, textCycle2Label, textCycle3Label, textCycle4Label, textCycle5Label, textCycle6Label;
    private Button btnNap, btnCycle1, btnCycle2, btnCycle3, btnCycle4, btnCycle5, btnCycle6;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sleep, container, false);

        viewModel = new ViewModelProvider(this).get(SleepViewModel.class);

        textCurrentTime = view.findViewById(R.id.textCurrentTime);
        
        // Zaman gösteren TextView'lar
        textNapTime = view.findViewById(R.id.textNapTime);
        textCycle1Time = view.findViewById(R.id.textCycle1Time);
        textCycle2Time = view.findViewById(R.id.textCycle2Time);
        textCycle3Time = view.findViewById(R.id.textCycle3Time);
        textCycle4Time = view.findViewById(R.id.textCycle4Time);
        textCycle5Time = view.findViewById(R.id.textCycle5Time);
        textCycle6Time = view.findViewById(R.id.textCycle6Time);

        // Açıklama gösteren TextView'lar
        textNapLabel = view.findViewById(R.id.textNapLabel);
        textCycle1Label = view.findViewById(R.id.textCycle1Label);
        textCycle2Label = view.findViewById(R.id.textCycle2Label);
        textCycle3Label = view.findViewById(R.id.textCycle3Label);
        textCycle4Label = view.findViewById(R.id.textCycle4Label);
        textCycle5Label = view.findViewById(R.id.textCycle5Label);
        textCycle6Label = view.findViewById(R.id.textCycle6Label);

        // Alarm kuran Button'lar
        btnNap = view.findViewById(R.id.btnNap);
        btnCycle1 = view.findViewById(R.id.btnCycle1);
        btnCycle2 = view.findViewById(R.id.btnCycle2);
        btnCycle3 = view.findViewById(R.id.btnCycle3);
        btnCycle4 = view.findViewById(R.id.btnCycle4);
        btnCycle5 = view.findViewById(R.id.btnCycle5);
        btnCycle6 = view.findViewById(R.id.btnCycle6);

        updateUI();

        setAlarmOnClick(btnNap, viewModel.getCycleTime(0), "Kestirme");
        setAlarmOnClick(btnCycle1, viewModel.getCycleTime(1), "1 uyku döngüsü");
        setAlarmOnClick(btnCycle2, viewModel.getCycleTime(2), "2 uyku döngüsü");
        setAlarmOnClick(btnCycle3, viewModel.getCycleTime(3), "3 uyku döngüsü");
        setAlarmOnClick(btnCycle4, viewModel.getCycleTime(4), "4 uyku döngüsü");
        setAlarmOnClick(btnCycle5, viewModel.getCycleTime(5), "5 uyku döngüsü");
        setAlarmOnClick(btnCycle6, viewModel.getCycleTime(6), "6 uyku döngüsü");

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.calculateCycles();
            updateUI();
        }
    }

    private void updateUI() {
        textCurrentTime.setText("Şu anda saat: " + viewModel.getFormattedTime(viewModel.getNow()));
        
        textNapTime.setText(viewModel.getFormattedTime(viewModel.getCycleTime(0)));
        textCycle1Time.setText(viewModel.getFormattedTime(viewModel.getCycleTime(1)));
        textCycle2Time.setText(viewModel.getFormattedTime(viewModel.getCycleTime(2)));
        textCycle3Time.setText(viewModel.getFormattedTime(viewModel.getCycleTime(3)));
        textCycle4Time.setText(viewModel.getFormattedTime(viewModel.getCycleTime(4)));
        textCycle5Time.setText(viewModel.getFormattedTime(viewModel.getCycleTime(5)));
        textCycle6Time.setText(viewModel.getFormattedTime(viewModel.getCycleTime(6)));
        
        // Etiketleri dinamik değerlere göre güncelle
        if (getContext() != null) {
            SharedPreferences prefs = getContext().getSharedPreferences("UserData", Context.MODE_PRIVATE);
            int napDur = prefs.getInt("napDuration", 25);
            int fallDur = prefs.getInt("fallAsleepDuration", 15);
            int cycleDur = prefs.getInt("cycleDuration", 90);

            textNapLabel.setText("Kestirme (" + napDur + " dk)");
            
            // Döngü etiketlerini "X Döngü (Y s Z dk)" formatında ayarla
            updateCycleLabel(textCycle1Label, 1, fallDur, cycleDur);
            updateCycleLabel(textCycle2Label, 2, fallDur, cycleDur);
            updateCycleLabel(textCycle3Label, 3, fallDur, cycleDur);
            updateCycleLabel(textCycle4Label, 4, fallDur, cycleDur);
            updateCycleLabel(textCycle5Label, 5, fallDur, cycleDur);
            updateCycleLabel(textCycle6Label, 6, fallDur, cycleDur);
        }
    }

    private void updateCycleLabel(TextView tv, int cycleCount, int fallAsleep, int cycleDuration) {
        int totalMinutes = fallAsleep + (cycleCount * cycleDuration);
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        
        String timeStr = "";
        if (hours > 0) {
            timeStr += hours + "s ";
        }
        timeStr += mins + "dk";
        
        tv.setText(cycleCount + " Döngü (" + timeStr + ")");
    }

    private void setAlarmOnClick(Button btn, Calendar time, String message) {
        btn.setOnClickListener(v -> setAlarm(time, message));
    }

    private void setAlarm(Calendar time, String message) {
        if (time == null) return;

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
                Toast.makeText(getContext(),
                        "Alarm listesi açıldı. " + viewModel.getFormattedTime(time) + " saatine alarm kurun.",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Toast.makeText(getContext(),
                        "Alarm kurulamadı. Saat: " + viewModel.getFormattedTime(time),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}