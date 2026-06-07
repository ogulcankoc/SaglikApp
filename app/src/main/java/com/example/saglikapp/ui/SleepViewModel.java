package com.example.saglikapp.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class SleepViewModel extends AndroidViewModel {

    private Calendar now;
    private ArrayList<Calendar> sleepCycles;
    private final SharedPreferences prefs;

    public SleepViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        calculateCycles();
    }

    public void calculateCycles() {
        now = Calendar.getInstance();
        sleepCycles = new ArrayList<>();

        // Ayarlardan değerleri oku (Default: 25, 15, 90)
        int napDuration = prefs.getInt("napDuration", 25);
        int fallAsleepDuration = prefs.getInt("fallAsleepDuration", 15);
        int cycleDuration = prefs.getInt("cycleDuration", 90);

        // Kestirme
        Calendar nap = (Calendar) now.clone();
        nap.add(Calendar.MINUTE, napDuration);
        sleepCycles.add(nap);

        // Uykuya dalma süresi eklendikten sonra döngüler başlar
        Calendar base = (Calendar) now.clone();
        base.add(Calendar.MINUTE, fallAsleepDuration);

        // 6 adet uyku döngüsü
        for (int i = 1; i <= 6; i++) {
            Calendar c = (Calendar) base.clone();
            c.add(Calendar.MINUTE, i * cycleDuration);
            sleepCycles.add(c);
        }
    }

    public Calendar getNow() {
        return now;
    }

    public Calendar getCycleTime(int index) {
        if (sleepCycles != null && index >= 0 && index < sleepCycles.size()) {
            return sleepCycles.get(index);
        }
        return null;
    }

    public String getFormattedTime(Calendar c) {
        if (c == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(c.getTime());
    }
}