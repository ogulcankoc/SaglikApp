package com.example.saglikapp.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WaterViewModel extends AndroidViewModel {

    private final SharedPreferences userPref;
    private final SharedPreferences waterPref;

    private int currentWater = 0;
    private int dailyGoal = 2500;

    public WaterViewModel(@NonNull Application application) {
        super(application);
        userPref = application.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        waterPref = application.getSharedPreferences("WaterData", Context.MODE_PRIVATE);

        // 1. Önce gün kontrolü yap ve gerekirse sıfırla
        checkAndResetDailyWater();

        // 2. Hedefi hesapla
        calculateAndSetGoal();
    }

    // GÜN KONTROLÜ: Eğer yeni bir güne girildiyse su miktarını sıfırla
    private void checkAndResetDailyWater() {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastSavedDate = waterPref.getString("last_saved_date", "");

        if (!todayDate.equals(lastSavedDate)) {
            // Yeni gün! Suyu sıfırla ve tarihi güncelle
            currentWater = 0;
            waterPref.edit()
                    .putInt("today_water", 0)
                    .putString("last_saved_date", todayDate)
                    .apply();
        } else {
            // Aynı gün, mevcut suyu yükle
            currentWater = waterPref.getInt("today_water", 0);
        }
    }

    private void calculateAndSetGoal() {
        int savedGoal = waterPref.getInt("daily_goal", 0);
        if (savedGoal > 0) {
            dailyGoal = savedGoal;
        } else {
            String weightStr = userPref.getString("weight", "70"); // Varsayılan 70
            try {
                int weight = Integer.parseInt(weightStr);
                if (weight > 0) {
                    dailyGoal = weight * 33;
                } else {
                    dailyGoal = 2500;
                }
            } catch (NumberFormatException e) {
                dailyGoal = 2500;
            }
            saveGoal();
        }
    }

    public void addWater(int amount) {
        currentWater += amount;
        saveWater();
    }

    public void resetWater() {
        currentWater = 0;
        saveWater();
    }

    public void updateGoal(int newGoal) {
        if (newGoal > 0) {
            dailyGoal = newGoal;
            saveGoal();
        }
    }

    private void saveWater() {
        waterPref.edit().putInt("today_water", currentWater).apply();
    }

    private void saveGoal() {
        waterPref.edit().putInt("daily_goal", dailyGoal).apply();
    }

    public int getCurrentWater() { return currentWater; }
    public int getDailyGoal() { return dailyGoal; }
}