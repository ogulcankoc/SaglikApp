package com.example.saglikapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final int ALARM_REQUEST_CODE = 100;
    private static final int ALARM_DELAY_MINUTES = 5;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.d(TAG, "Telefon yeniden başladı, alarmlar kuruluyor...");

            // Kullanıcı verilerini kontrol et
            SharedPreferences sharedPref = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);

            if (sharedPref.contains("name")) {
                String wakeUpTime = sharedPref.getString("wakeUpTime", "08:00");
                String bedTime = sharedPref.getString("bedTime", "23:00");

                // İlk alarmı kur
                scheduleFirstAlarm(context, wakeUpTime, bedTime);

                Log.i(TAG, "✅ Su hatırlatıcı alarmı yeniden kuruldu");
            } else {
                Log.w(TAG, "Kayıtlı kullanıcı bulunamadı, alarm kurulmadı");
            }
        }
    }

    private void scheduleFirstAlarm(Context context, String wakeUpTimeStr, String bedTimeStr) {
        Calendar now = Calendar.getInstance();
        Calendar alarmTime = Calendar.getInstance();

        try {
            // Uyanma saatini parse et
            String[] wakeParts = wakeUpTimeStr.split(":");
            int wakeHour = Integer.parseInt(wakeParts[0].trim());
            int wakeMinute = Integer.parseInt(wakeParts[1].trim());

            // Yatma saatini parse et
            String[] bedParts = bedTimeStr.split(":");
            int bedHour = Integer.parseInt(bedParts[0].trim());
            int bedMinute = Integer.parseInt(bedParts[1].trim());

            // Bugünün uyanma zamanı
            Calendar todayWakeTime = Calendar.getInstance();
            todayWakeTime.set(Calendar.HOUR_OF_DAY, wakeHour);
            todayWakeTime.set(Calendar.MINUTE, wakeMinute);
            todayWakeTime.set(Calendar.SECOND, 0);
            todayWakeTime.set(Calendar.MILLISECOND, 0);
            todayWakeTime.add(Calendar.MINUTE, ALARM_DELAY_MINUTES);

            // Bugünün yatma zamanı
            Calendar todayBedTime = Calendar.getInstance();
            todayBedTime.set(Calendar.HOUR_OF_DAY, bedHour);
            todayBedTime.set(Calendar.MINUTE, bedMinute);
            todayBedTime.set(Calendar.SECOND, 0);
            todayBedTime.set(Calendar.MILLISECOND, 0);

            if (bedHour < wakeHour) {
                todayBedTime.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Alarm zamanını belirle
            if (now.before(todayWakeTime)) {
                alarmTime = todayWakeTime;
            } else if (now.before(todayBedTime)) {
                alarmTime = Calendar.getInstance();
                alarmTime.add(Calendar.HOUR_OF_DAY, 2);
            } else {
                Calendar tomorrowWakeTime = (Calendar) todayWakeTime.clone();
                tomorrowWakeTime.add(Calendar.DAY_OF_YEAR, 1);
                alarmTime = tomorrowWakeTime;
            }

            // Alarmı kur
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

            alarmManager.cancel(pendingIntent);

            long alarmTimeMillis = alarmTime.getTimeInMillis();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTimeMillis,
                            pendingIntent
                    );
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTimeMillis,
                            pendingIntent
                    );
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                );
            }

        } catch (Exception e) {
            Log.e(TAG, "Alarm kurulurken hata: " + e.getMessage(), e);
        }
    }
}