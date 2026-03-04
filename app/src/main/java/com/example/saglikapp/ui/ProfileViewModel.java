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
    private static final int ALARM_DELAY_MINUTES = 5;
    private static final int FALLBACK_DELAY_HOURS = 2;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        sharedPref = application.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        waterPref = application.getSharedPreferences("WaterData", Context.MODE_PRIVATE);
    }

    // --- MEVCUT VERİLERİ OKUMA METOTLARI ---
    public String getUserData(String key) {
        return sharedPref.getString(key, "");
    }

    public String getGender() {
        return sharedPref.getString("gender", "");
    }

    // --- VERİ DOĞRULAMA ---
    public boolean validateInputs(String name, String age, String weight, String height, String gender, String wakeUp, String bedTime) {
        return !TextUtils.isEmpty(name) && !TextUtils.isEmpty(age) &&
                !TextUtils.isEmpty(weight) && !TextUtils.isEmpty(height) &&
                !TextUtils.isEmpty(gender) && !TextUtils.isEmpty(wakeUp) &&
                !TextUtils.isEmpty(bedTime);
    }

    // --- VERİLERİ KAYDETME VE SU HEDEFİNİ SIFIRLAMA ---
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

        // Kilo veya hedef değiştiği için mevcut su hedefini siliyoruz
        waterPref.edit().remove("daily_goal").apply();
    }

    // --- ALARM ZAMANI HESAPLAMA (MainViewModel ile aynı mantık) ---
    public Calendar calculateFirstAlarmTime(String wakeUpTimeStr, String bedTimeStr) {
        Calendar now = Calendar.getInstance();
        Calendar alarmTime = null;

        try {
            String[] wakeParts = wakeUpTimeStr.split(":");
            if (wakeParts.length != 2) return null;

            int wakeHour = Integer.parseInt(wakeParts[0].trim());
            int wakeMinute = Integer.parseInt(wakeParts[1].trim());

            if (wakeHour < 0 || wakeHour > 23 || wakeMinute < 0 || wakeMinute > 59) return null;

            String[] bedParts = bedTimeStr.split(":");
            if (bedParts.length != 2) return null;

            int bedHour = Integer.parseInt(bedParts[0].trim());
            int bedMinute = Integer.parseInt(bedParts[1].trim());

            if (bedHour < 0 || bedHour > 23 || bedMinute < 0 || bedMinute > 59) return null;

            Calendar todayWakeTime = Calendar.getInstance();
            todayWakeTime.set(Calendar.HOUR_OF_DAY, wakeHour);
            todayWakeTime.set(Calendar.MINUTE, wakeMinute);
            todayWakeTime.set(Calendar.SECOND, 0);
            todayWakeTime.set(Calendar.MILLISECOND, 0);
            todayWakeTime.add(Calendar.MINUTE, ALARM_DELAY_MINUTES);

            Calendar todayBedTime = Calendar.getInstance();
            todayBedTime.set(Calendar.HOUR_OF_DAY, bedHour);
            todayBedTime.set(Calendar.MINUTE, bedMinute);
            todayBedTime.set(Calendar.SECOND, 0);
            todayBedTime.set(Calendar.MILLISECOND, 0);

            if (bedHour < wakeHour) {
                todayBedTime.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (now.before(todayWakeTime)) {
                alarmTime = todayWakeTime;
            } else if (now.before(todayBedTime)) {
                alarmTime = Calendar.getInstance();
                alarmTime.add(Calendar.HOUR_OF_DAY, FALLBACK_DELAY_HOURS);
            } else {
                Calendar tomorrowWakeTime = (Calendar) todayWakeTime.clone();
                tomorrowWakeTime.add(Calendar.DAY_OF_YEAR, 1);
                alarmTime = tomorrowWakeTime;
            }
            return alarmTime;

        } catch (Exception e) {
            return null;
        }
    }
}