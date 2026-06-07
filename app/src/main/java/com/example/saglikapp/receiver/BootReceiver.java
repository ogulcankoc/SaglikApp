package com.example.saglikapp.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.example.saglikapp.ui.MainViewModel; // AlarmCalculator'ı kullanmak için

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final int ALARM_REQUEST_CODE = 100;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.d(TAG, "Telefon yeniden başladı, alarmlar kontrol ediliyor...");

            SharedPreferences sharedPref = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);

            if (sharedPref.contains("wakeUpTime") && sharedPref.contains("bedTime")) {
                String wakeUpTime = sharedPref.getString("wakeUpTime", "08:00");
                String bedTime = sharedPref.getString("bedTime", "23:00");

                // Diğer ekranlarla aynı hesaplama mantığını kullanıyoruz
                MainViewModel.AlarmCalculator calculator = new MainViewModel.AlarmCalculator();
                Calendar alarmTime = calculator.calculateFirstAlarmTime(context, wakeUpTime, bedTime);

                if (alarmTime != null) {
                    setAlarmSafely(context, alarmTime.getTimeInMillis());
                    Log.i(TAG, "✅ Alarmlar başarıyla restore edildi.");
                }
            }
        }
    }

    private void setAlarmSafely(Context context, long timeInMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.putExtra("type", "water");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Geçmiş zaman koruması
        if (timeInMillis <= System.currentTimeMillis()) {
            timeInMillis = System.currentTimeMillis() + 10000; // 10 saniye sonra
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Exact alarm izni yok, normal alarm kuruluyor.");
            alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }
}