package com.example.saglikapp.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.util.Calendar;

public class MainViewModel extends AndroidViewModel {

    private final SharedPreferences sharedPref;

    public MainViewModel(@NonNull Application application) {
        super(application);
        sharedPref = application.getSharedPreferences("UserData", Context.MODE_PRIVATE);
    }

    public boolean isUserRegistered() {
        return sharedPref.contains("name");
    }

    public boolean validateInputs(String name, String age, String weight, String height, String gender, String wakeUp, String bedTime) {
        return !TextUtils.isEmpty(name) && !TextUtils.isEmpty(age) &&
                !TextUtils.isEmpty(weight) && !TextUtils.isEmpty(height) &&
                !TextUtils.isEmpty(gender) && !TextUtils.isEmpty(wakeUp) &&
                !TextUtils.isEmpty(bedTime);
    }

    public void saveUserData(String name, String age, String weight, String height, String gender, String wakeUp, String bedTime) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("name", name);
        editor.putString("age", age);
        editor.putString("weight", weight);
        editor.putString("height", height);
        editor.putString("gender", gender);
        editor.putString("wakeUpTime", wakeUp);
        editor.putString("bedTime", bedTime);
        editor.apply();
    }

    public static class AlarmCalculator {

        private static final int ALARM_BOUNDARY_MINUTES = 15;

        public Calendar calculateFirstAlarmTime(Context context, String wakeUpTimeStr, String bedTimeStr) {
            try {
                // Ayarlardan bildirim aralığını oku (Dakika cinsinden)
                SharedPreferences prefs = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
                int waterIntervalMins = prefs.getInt("waterIntervalMinutes", 120);

                String[] w = wakeUpTimeStr.split(":");
                String[] b = bedTimeStr.split(":");

                Calendar now = Calendar.getInstance();
                now.set(Calendar.SECOND, 0);
                now.set(Calendar.MILLISECOND, 0);

                Calendar wakeAt = (Calendar) now.clone();
                wakeAt.set(Calendar.HOUR_OF_DAY, Integer.parseInt(w[0].trim()));
                wakeAt.set(Calendar.MINUTE,      Integer.parseInt(w[1].trim()));

                Calendar bedAt = (Calendar) now.clone();
                bedAt.set(Calendar.HOUR_OF_DAY, Integer.parseInt(b[0].trim()));
                bedAt.set(Calendar.MINUTE,      Integer.parseInt(b[1].trim()));

                if (!bedAt.after(wakeAt)) {
                    bedAt.add(Calendar.DAY_OF_YEAR, 1);
                }

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

                if (now.before(firstAlarm)) {
                    return firstAlarm;
                }

                Calendar safetyNow = (Calendar) now.clone();
                safetyNow.add(Calendar.MINUTE, 1);

                if (safetyNow.after(lastAlarm)) {
                    Calendar tomorrowFirst = (Calendar) wakeAt.clone();
                    tomorrowFirst.add(Calendar.DAY_OF_YEAR, 1);
                    tomorrowFirst.add(Calendar.MINUTE, ALARM_BOUNDARY_MINUTES);
                    return tomorrowFirst;
                }

                Calendar nextAlarm = (Calendar) firstAlarm.clone();
                while (!nextAlarm.after(now)) {
                        nextAlarm.add(Calendar.MINUTE, waterIntervalMins);
                }

                if (nextAlarm.after(lastAlarm)) {
                    return lastAlarm;
                }

                return nextAlarm;

            } catch (Exception e) {
                return null;
            }
        }
    }
    public String getName() { return sharedPref.getString("name", ""); }
    public String getAge() { return sharedPref.getString("age", ""); }
    public String getWeight() { return sharedPref.getString("weight", ""); }
    public String getHeight() { return sharedPref.getString("height", ""); }
    public String getGender() { return sharedPref.getString("gender", ""); }
    public String getWakeUpTime() { return sharedPref.getString("wakeUpTime", ""); }
    public String getBedTime() { return sharedPref.getString("bedTime", ""); }
}