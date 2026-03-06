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
        private static final int ALARM_INTERVAL_HOURS   = 2;

        public Calendar calculateFirstAlarmTime(String wakeUpTimeStr, String bedTimeStr) {
            try {
                String[] w = wakeUpTimeStr.split(":");
                String[] b = bedTimeStr.split(":");

                Calendar now    = Calendar.getInstance();
                Calendar wakeAt = Calendar.getInstance();
                Calendar bedAt  = Calendar.getInstance();

                wakeAt.set(Calendar.HOUR_OF_DAY, Integer.parseInt(w[0].trim()));
                wakeAt.set(Calendar.MINUTE,      Integer.parseInt(w[1].trim()));
                wakeAt.set(Calendar.SECOND,      0);
                wakeAt.set(Calendar.MILLISECOND, 0);

                bedAt.set(Calendar.HOUR_OF_DAY, Integer.parseInt(b[0].trim()));
                bedAt.set(Calendar.MINUTE,      Integer.parseInt(b[1].trim()));
                bedAt.set(Calendar.SECOND,      0);
                bedAt.set(Calendar.MILLISECOND, 0);

                // Gece kuşu: yatış uyanıştan önceyse ertesi güne taşı
                if (!bedAt.after(wakeAt)) bedAt.add(Calendar.DAY_OF_YEAR, 1);

                // Dünkü döngü hâlâ aktif mi? (ör: şu an 01:00, yatış 02:00)
                Calendar wPrev = (Calendar) wakeAt.clone(); wPrev.add(Calendar.DAY_OF_YEAR, -1);
                Calendar bPrev = (Calendar) bedAt.clone();  bPrev.add(Calendar.DAY_OF_YEAR, -1);
                if (!now.before(wPrev) && now.before(bPrev)) {
                    wakeAt = wPrev;
                    bedAt  = bPrev;
                }

                Calendar firstAlarm = (Calendar) wakeAt.clone();
                firstAlarm.add(Calendar.MINUTE, ALARM_BOUNDARY_MINUTES);

                Calendar lastAlarm = (Calendar) bedAt.clone();
                lastAlarm.add(Calendar.MINUTE, -ALARM_BOUNDARY_MINUTES);

                if (now.before(firstAlarm)) return firstAlarm;

                if (!now.before(lastAlarm)) {
                    firstAlarm.add(Calendar.DAY_OF_YEAR, 1);
                    return firstAlarm;
                }

                // 2 saatlik periyot
                long passed = (now.getTimeInMillis() - firstAlarm.getTimeInMillis())
                        / ((long) ALARM_INTERVAL_HOURS * 60 * 60 * 1000);
                Calendar next = (Calendar) firstAlarm.clone();
                next.add(Calendar.HOUR_OF_DAY, (int)((passed + 1) * ALARM_INTERVAL_HOURS));

                return next.before(lastAlarm) ? next : lastAlarm;

            } catch (Exception e) {
                return null;
            }
        }
    }
}