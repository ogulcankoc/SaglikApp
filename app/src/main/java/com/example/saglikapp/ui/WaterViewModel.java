package com.example.saglikapp.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class WaterViewModel extends AndroidViewModel {

    // Veri saklama araçlarımız ve değişkenlerimiz
    private final SharedPreferences userPref;
    private final SharedPreferences waterPref;

    private int currentWater = 0;
    private int dailyGoal = 2500;

    // AndroidViewModel kullandığımız için constructor (yapıcı metod) Application context'i ister
    public WaterViewModel(@NonNull Application application) {
        super(application);
        // SharedPreferences tanımlamalarını Activity'den buraya aldık
        userPref = application.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        waterPref = application.getSharedPreferences("WaterData", Context.MODE_PRIVATE);

        // Uygulama açıldığında kayıtlı verileri yükle
        currentWater = waterPref.getInt("today_water", 0);
        calculateAndSetGoal();
    }

    // İş mantığı (Business Logic): Hedef hesaplama
    private void calculateAndSetGoal() {
        int savedGoal = waterPref.getInt("daily_goal", 0);
        if (savedGoal != 0) {
            dailyGoal = savedGoal;
        } else {
            String weightStr = userPref.getString("weight", "0");
            try {
                int weight = Integer.parseInt(weightStr);
                if (weight > 0) dailyGoal = weight * 33; // Kilo bazlı harika hesaplama mantığın
                else dailyGoal = 2500;
            } catch (NumberFormatException e) {
                dailyGoal = 2500;
            }
            saveGoal();
        }
    }

    // Su ekleme işlemi
    public void addWater(int amount) {
        currentWater += amount;
        saveWater();
    }

    // Suyu sıfırlama işlemi
    public void resetWater() {
        currentWater = 0;
        saveWater();
    }

    // Yeni hedef belirleme işlemi
    public void updateGoal(int newGoal) {
        dailyGoal = newGoal;
        saveGoal();
    }

    // Arka plan veri kayıt işlemleri
    private void saveWater() {
        waterPref.edit().putInt("today_water", currentWater).apply();
    }

    private void saveGoal() {
        waterPref.edit().putInt("daily_goal", dailyGoal).apply();
    }

    // Activity'nin (UI) arayüzü güncellemek için kullanacağı Getter metodları
    public int getCurrentWater() { return currentWater; }
    public int getDailyGoal() { return dailyGoal; }
}