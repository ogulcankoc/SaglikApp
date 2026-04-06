package com.example.saglikapp.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.Calendar;

public class ProfileViewModel extends AndroidViewModel {

    private final SharedPreferences sharedPref;
    private final SharedPreferences waterPref;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        sharedPref = application.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        waterPref  = application.getSharedPreferences("WaterData", Context.MODE_PRIVATE);
    }

    public String getUserData(String key) {
        return sharedPref.getString(key, "");
    }

    public String getGender() {
        return sharedPref.getString("gender", "");
    }

    public boolean validateInputs(String name, String age, String weight, String height, String gender, String wakeUp, String bedTime) {
        return !TextUtils.isEmpty(name) && !TextUtils.isEmpty(age) &&
                !TextUtils.isEmpty(weight) && !TextUtils.isEmpty(height) &&
                !TextUtils.isEmpty(gender) && !TextUtils.isEmpty(wakeUp) &&
                !TextUtils.isEmpty(bedTime);
    }

    public void saveProfileData(String name, String age, String weight, String height, String gender, String wakeUp, String bedTime) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("name", name);
        editor.putString("age", age);
        editor.putString("weight", weight);
        editor.putString("height", height);
        editor.putString("gender", gender);
        editor.putString("wakeUpTime", wakeUp);
        editor.putString("bedTime", bedTime);
        editor.apply();

        // Kilo veya hedef değiştiği için mevcut su hedefini sıfırlıyoruz
        waterPref.edit().remove("daily_goal").apply();
    }

    public static class AlarmCalculator {

        private static final int ALARM_BOUNDARY_MINUTES = 5;
        private static final int ALARM_INTERVAL_HOURS   = 1;

        public Calendar calculateFirstAlarmTime(String wakeUpTimeStr, String bedTimeStr) {
            try {
                String[] w = wakeUpTimeStr.split(":");
                String[] b = bedTimeStr.split(":");

                Calendar now = Calendar.getInstance();
                // Saniye ve milisaniyeyi sıfırlayarak net karşılaştırma sağlıyoruz
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.MILLISECOND, 0);

                Calendar wakeAt = (Calendar) now.clone();
                wakeAt.set(Calendar.HOUR_OF_DAY, Integer.parseInt(w[0].trim()));
                wakeAt.set(Calendar.MINUTE,      Integer.parseInt(w[1].trim()));

                Calendar bedAt = (Calendar) now.clone();
                bedAt.set(Calendar.HOUR_OF_DAY, Integer.parseInt(b[0].trim()));
                bedAt.set(Calendar.MINUTE,      Integer.parseInt(b[1].trim()));

                // Gece kuşu ayarı: Yatış uyanıştan önceyse (örn: 08:00 uyanış, 02:00 yatış)
                if (!bedAt.after(wakeAt)) {
                    bedAt.add(Calendar.DAY_OF_YEAR, 1);
                }

                // Döngü kontrolü: Şu an dünkü aktif periyodun içinde miyiz?
                Calendar wPrev = (Calendar) wakeAt.clone(); wPrev.add(Calendar.DAY_OF_YEAR, -1);
                Calendar bPrev = (Calendar) bedAt.clone();  bPrev.add(Calendar.DAY_OF_YEAR, -1);

                if (now.after(wPrev) && now.before(bPrev)) {
                    wakeAt = wPrev;
                    bedAt = bPrev;
                }

                Calendar firstAlarm = (Calendar) wakeAt.clone();
                firstAlarm.add(Calendar.MINUTE, ALARM_BOUNDARY_MINUTES);

                Calendar lastAlarm = (Calendar) bedAt.clone();
                lastAlarm.add(Calendar.MINUTE, -ALARM_BOUNDARY_MINUTES);

                // DURUM 1: Henüz uyanma vaktine gelmedik (Sabah ilk alarmı kur)
                if (now.before(firstAlarm)) {
                    return firstAlarm;
                }

                // DURUM 2: Yatış vaktine çok yaklaştık veya geçtik (Yarın sabahı kur)
                Calendar safetyNow = (Calendar) now.clone();
                safetyNow.add(Calendar.MINUTE, 1); // 1 dakikalık güvenlik payı

                if (safetyNow.after(lastAlarm)) {
                    Calendar tomorrowFirst = (Calendar) wakeAt.clone();
                    tomorrowFirst.add(Calendar.DAY_OF_YEAR, 1);
                    tomorrowFirst.add(Calendar.MINUTE, ALARM_BOUNDARY_MINUTES);
                    return tomorrowFirst;
                }

                // DURUM 3: Gün içindeyiz, bir sonraki 2 saatlik dilimi bul
                Calendar nextAlarm = (Calendar) firstAlarm.clone();
                while (!nextAlarm.after(now)) {
                    nextAlarm.add(Calendar.HOUR_OF_DAY, ALARM_INTERVAL_HOURS);
                }

                // Eğer hesaplanan zaman yatış sınırını aşıyorsa, günün son alarmını kur
                if (nextAlarm.after(lastAlarm)) {
                    return lastAlarm;
                }

                return nextAlarm;

            } catch (Exception e) {
                return null;
            }
        }
    }
}