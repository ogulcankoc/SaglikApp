package com.example.saglikapp.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.saglikapp.data.WaterDao;
import com.example.saglikapp.data.WaterDatabase;
import com.example.saglikapp.data.WaterLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WaterViewModel extends AndroidViewModel {

    private final SharedPreferences userPref;
    private final SharedPreferences waterPref;

    // Room için gerekli değişkenler
    private final WaterDao waterDao;
    private final String todayDate;
    // Veritabanı işlemlerini arka planda yapmak için Executor
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private int currentWater = 0;
    private int dailyGoal = 2500;

    public WaterViewModel(@NonNull Application application) {
        super(application);
        userPref = application.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        waterPref = application.getSharedPreferences("WaterData", Context.MODE_PRIVATE);

        // Room Veritabanını Başlat
        WaterDatabase db = WaterDatabase.getInstance(application);
        waterDao = db.waterDao();

        // Bugünün tarihini belirle
        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 1. Önce gün kontrolü yap ve gerekirse sıfırla
        checkAndResetDailyWater();

        // 2. Hedefi hesapla
        calculateAndSetGoal();
    }

    // GÜN KONTROLÜ: Eğer yeni bir güne girildiyse su miktarını sıfırla
    private void checkAndResetDailyWater() {
        String lastSavedDate = waterPref.getString("last_saved_date", "");

        if (!todayDate.equals(lastSavedDate)) {
            // Yeni gün! Suyu sıfırla ve tarihi güncelle
            currentWater = 0;
            waterPref.edit()
                    .putInt("today_water", 0)
                    .putString("last_saved_date", todayDate)
                    .apply();

            // Yeni gün başladığında Room'a 0 ml ile bir kayıt aç (Opsiyonel ama grafik için iyi olur)
            saveToRoom();
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
        // SharedPreferences Güncelleme
        currentWater += amount;
        saveWater();

        // Room Database Güncelleme (Arka planda)
        saveToRoom();
    }

    public void resetWater() {
        currentWater = 0;
        saveWater();
        saveToRoom();
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

    // Room'a veriyi asenkron olarak kaydeden yardımcı metod
    private void saveToRoom() {
        executorService.execute(() -> {
            WaterLog log = new WaterLog(todayDate, currentWater);
            waterDao.insertOrUpdate(log);
        });
    }

    public int getCurrentWater() { return currentWater; }
    public int getDailyGoal() { return dailyGoal; }
}