package com.example.saglikapp.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.saglikapp.data.WaterDao;
import com.example.saglikapp.data.AppDatabase;
import com.example.saglikapp.data.WaterLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WaterViewModel extends AndroidViewModel {

    private final SharedPreferences userPref;
    private final SharedPreferences waterPref;

    private final WaterDao waterDao;
    private final String todayDate;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Integer> currentWater = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> dailyGoal = new MutableLiveData<>(2500);

    public WaterViewModel(@NonNull Application application) {
        super(application);
        userPref = application.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        waterPref = application.getSharedPreferences("WaterData", Context.MODE_PRIVATE);

        AppDatabase db = AppDatabase.getInstance(application);
        waterDao = db.waterDao();

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        checkAndResetDailyWater();
        loadGoal();
    }

    private void checkAndResetDailyWater() {
        String lastSavedDate = waterPref.getString("last_saved_date", "");

        if (!todayDate.equals(lastSavedDate)) {
            currentWater.setValue(0);
            waterPref.edit()
                    .putInt("today_water", 0)
                    .putString("last_saved_date", todayDate)
                    .apply();
            saveToRoom();
        } else {
            currentWater.setValue(waterPref.getInt("today_water", 0));
        }
    }

    private void loadGoal() {
        int savedGoal = waterPref.getInt("daily_goal", 0);

        if (savedGoal > 0) {
            dailyGoal.setValue(savedGoal);
        } else {
            String weightStr = userPref.getString("weight", "70");
            try {
                int weight = Integer.parseInt(weightStr);
                if (weight > 0) {
                    dailyGoal.setValue(weight * 33);
                } else {
                    dailyGoal.setValue(2500);
                }
            } catch (NumberFormatException e) {
                dailyGoal.setValue(2500);
            }
        }
    }

    public void addWater(int amount) {
        int newValue = (currentWater.getValue() != null ? currentWater.getValue() : 0) + amount;
        currentWater.setValue(newValue);
        saveWater();
        saveToRoom();
    }

    public void resetWater() {
        currentWater.setValue(0);
        saveWater();
        saveToRoom();
    }

    public void updateGoal(int newGoal) {
        if (newGoal > 0) {
            dailyGoal.setValue(newGoal);
            saveGoal();
        }
    }

    private void saveWater() {
        Integer water = currentWater.getValue();
        waterPref.edit().putInt("today_water", water != null ? water : 0).apply();
    }

    private void saveGoal() {
        Integer goal = dailyGoal.getValue();
        waterPref.edit().putInt("daily_goal", goal != null ? goal : 2500).apply();
    }

    private void saveToRoom() {
        executorService.execute(() -> {
            Integer water = currentWater.getValue();
            WaterLog log = new WaterLog(todayDate, water != null ? water : 0);
            waterDao.insertOrUpdate(log);
        });
    }

    public int getCurrentWater() { return currentWater.getValue() != null ? currentWater.getValue() : 0; }
    public int getDailyGoal() { return dailyGoal.getValue() != null ? dailyGoal.getValue() : 2500; }

    public LiveData<Integer> getCurrentWaterLiveData() { return currentWater; }
    public LiveData<Integer> getDailyGoalLiveData() { return dailyGoal; }
}