package com.example.saglikapp.receiver;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.saglikapp.ui.WaterActivity;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "SaglikAppChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String alarmType = intent.getStringExtra("type");

        if ("water".equals(alarmType)) {
            // --- GÜVENLİK KONTROLÜ ---
            // Eğer sistem alarmı geciktirdiyse ve şu an uyku vaktindeysek bildirim gösterme
            if (isCurrentTimeInSleepRange(context)) {
                Log.d("AlarmReceiver", "Uyku saatindeyiz, bildirim iptal edildi. Bir sonraki sabah için planlanıyor.");
                scheduleNextAlarm(context);
                return;
            }

            showWaterNotification(context);
            scheduleNextAlarm(context);
        } else {
            String message = intent.getStringExtra("message");
            if (message == null) message = "Uyanma Vakti!";
            showSleepNotification(context, message);
        }
    }

    private boolean isCurrentTimeInSleepRange(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String wakeUpStr = sharedPref.getString("wakeUpTime", "08:00");
        String bedTimeStr = sharedPref.getString("bedTime", "23:00");

        Calendar now = Calendar.getInstance();
        Calendar wakeUp = getCalendarFromTime(wakeUpStr);
        Calendar bedTime = getCalendarFromTime(bedTimeStr);

        if (bedTime.before(wakeUp)) bedTime.add(Calendar.DAY_OF_YEAR, 1);

        // Eğer şu an uyanma vaktinden önceyse VEYA yatma vaktinden sonraysa uykudayızdır.
        return now.before(wakeUp) || now.after(bedTime);
    }

    private void showWaterNotification(Context context) {
        Intent tapIntent = new Intent(context, WaterActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle("Su İçme Zamanı! 💧")
                .setContentText("Bir bardak su içip hedefine yaklaş.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notify(context, 1001, builder);
    }

    private void showSleepNotification(Context context, String message) {
        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Uyku Alarmı 🌙")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notify(context, 1002, builder);
    }

    private void scheduleNextAlarm(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String wakeUpStr = sharedPref.getString("wakeUpTime", "08:00");
        String bedTimeStr = sharedPref.getString("bedTime", "23:00");

        Calendar now = Calendar.getInstance();
        Calendar wakeUpTime = getCalendarFromTime(wakeUpStr);
        Calendar bedTime = getCalendarFromTime(bedTimeStr);

        // Gece kuşu ayarı
        if (bedTime.before(wakeUpTime)) {
            bedTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Eğer şu an dünkü döngünün içindeysek (gece 01:00 ama yatış 02:00 gibi)
        Calendar yesterdayWake = (Calendar) wakeUpTime.clone(); yesterdayWake.add(Calendar.DAY_OF_YEAR, -1);
        Calendar yesterdayBed = (Calendar) bedTime.clone(); yesterdayBed.add(Calendar.DAY_OF_YEAR, -1);

        if (now.after(yesterdayWake) && now.before(yesterdayBed)) {
            wakeUpTime = yesterdayWake;
            bedTime = yesterdayBed;
        } else if (now.after(bedTime)) {
            // Normal gün aşımı
            wakeUpTime.add(Calendar.DAY_OF_YEAR, 1);
            bedTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        Calendar lastCallTime = (Calendar) bedTime.clone();
        lastCallTime.add(Calendar.MINUTE, -15);

        Calendar nextAlarmTime;

        if (!now.before(lastCallTime)) {
            // Gün bitti, yarın sabah uyanış + 5dk
            nextAlarmTime = (Calendar) wakeUpTime.clone();
            if (!nextAlarmTime.after(now)) {
                nextAlarmTime.add(Calendar.DAY_OF_YEAR, 1);
            }
            nextAlarmTime.add(Calendar.MINUTE, 5);
        } else {
            // Gün içindeyiz
            Calendar potentialNextTime = (Calendar) now.clone();
            potentialNextTime.add(Calendar.HOUR_OF_DAY, 2);

            if (potentialNextTime.after(lastCallTime)) {
                nextAlarmTime = lastCallTime;
            } else {
                nextAlarmTime = potentialNextTime;
            }
        }

        setAlarm(context, nextAlarmTime.getTimeInMillis());
    }

    private void setAlarm(Context context, long timeInMillis) {
        // --- GEÇMİŞ ZAMAN KORUMASI ---
        // Eğer hesaplanan zaman şu andan küçükse (veya çok yakınsa),
        // Android alarmı hemen tetikler. Bunu engellemek için en az 10 saniye sonraya kuruyoruz.
        long currentTime = System.currentTimeMillis();
        if (timeInMillis <= currentTime) {
            timeInMillis = currentTime + 10000;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("type", "water");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
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
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
        }
    }

    private Calendar getCalendarFromTime(String timeStr) {
        Calendar cal = Calendar.getInstance();
        try {
            String[] parts = timeStr.split(":");
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0].trim()));
            cal.set(Calendar.MINUTE, Integer.parseInt(parts[1].trim()));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {
            Log.e("AlarmReceiver", "Zaman formatı hatası: " + timeStr);
        }
        return cal;
    }

    private void notify(Context context, int id, NotificationCompat.Builder builder) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(id, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Sağlık Bildirimleri", NotificationManager.IMPORTANCE_HIGH
            );
            context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}