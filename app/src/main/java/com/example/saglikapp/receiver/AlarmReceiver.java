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

import com.example.saglikapp.R;
import com.example.saglikapp.ui.WelcomeActivity;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "SaglikAppChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String alarmType = intent.getStringExtra("type");

        if ("water".equals(alarmType)) {
            if (isCurrentTimeInSleepRange(context) || isWaterGoalReached(context)) {
                Log.d("AlarmReceiver", "Uyku saatindeyiz veya hedef tamamlandı. Bildirim gösterilmeyecek.");
                scheduleNextAlarm(context);
                return;
            }

            showWaterNotification(context);
            scheduleNextAlarm(context);

        } else if ("reschedule".equals(alarmType)) {
            Log.d("AlarmReceiver", "Kullanıcı su içti veya ayar değişti, alarm durumu güncelleniyor.");
            scheduleNextAlarm(context);

        } else {
            String message = intent.getStringExtra("message");
            if (message == null) message = "Uyanma Vakti!";
            showSleepNotification(context, message);
        }
    }

    private boolean isWaterGoalReached(Context context) {
        SharedPreferences waterPref = context.getSharedPreferences("WaterData", Context.MODE_PRIVATE);
        int currentWater = waterPref.getInt("today_water", 0);
        int dailyGoal = waterPref.getInt("daily_goal", 2500);

        return currentWater >= dailyGoal && dailyGoal > 0;
    }

    private boolean isCurrentTimeInSleepRange(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String wakeUpStr = sharedPref.getString("wakeUpTime", "08:00");
        String bedTimeStr = sharedPref.getString("bedTime", "23:00");

        Calendar now = Calendar.getInstance();
        Calendar wakeUp = getCalendarFromTime(wakeUpStr);
        Calendar bedTime = getCalendarFromTime(bedTimeStr);

        if (bedTime.before(wakeUp)) {
            bedTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        Calendar yesterdayWake = (Calendar) wakeUp.clone();
        yesterdayWake.add(Calendar.DAY_OF_YEAR, -1);

        Calendar yesterdayBed = (Calendar) bedTime.clone();
        yesterdayBed.add(Calendar.DAY_OF_YEAR, -1);

        if (now.after(yesterdayWake) && now.before(yesterdayBed)) {
            wakeUp = yesterdayWake;
            bedTime = yesterdayBed;
        }

        return now.before(wakeUp) || now.after(bedTime);
    }

    private void showWaterNotification(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String userName = sharedPref.getString("name", "Dostum");

        // Artık WelcomeActivity'ye yönlendiriyoruz (Sekmeli ana ekran)
        Intent tapIntent = new Intent(context, WelcomeActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.blue))
                .setContentTitle("Su Vakti, " + userName + "! \uD83D\uDCA7")
                .setContentText("Hedefine ulaşmak için bir bardak su içmeyi unutma.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notify(context, 1001, builder);
    }

    private void showSleepNotification(Context context, String message) {
        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Uyku Alarmı \uD83C\uDF19")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notify(context, 1002, builder);
    }

    private void scheduleNextAlarm(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String wakeUpStr = sharedPref.getString("wakeUpTime", "08:00");
        String bedTimeStr = sharedPref.getString("bedTime", "23:00");

        // Ayarlardan bildirim aralığını oku (Dakika cinsinden)
        int waterIntervalMins = sharedPref.getInt("waterIntervalMinutes", 120);

        Calendar now = Calendar.getInstance();
        Calendar wakeUpTime = getCalendarFromTime(wakeUpStr);
        Calendar bedTime = getCalendarFromTime(bedTimeStr);

        if (bedTime.before(wakeUpTime)) {
            bedTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        Calendar yesterdayWake = (Calendar) wakeUpTime.clone(); yesterdayWake.add(Calendar.DAY_OF_YEAR, -1);
        Calendar yesterdayBed = (Calendar) bedTime.clone(); yesterdayBed.add(Calendar.DAY_OF_YEAR, -1);

        if (now.after(yesterdayWake) && now.before(yesterdayBed)) {
            wakeUpTime = yesterdayWake;
            bedTime = yesterdayBed;
        } else if (now.after(bedTime)) {
            wakeUpTime.add(Calendar.DAY_OF_YEAR, 1);
            bedTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (isCurrentTimeInSleepRange(context) || isWaterGoalReached(context)) {
            Calendar nextAlarmTime = (Calendar) wakeUpTime.clone();
            if (!nextAlarmTime.after(now)) {
                nextAlarmTime.add(Calendar.DAY_OF_YEAR, 1);
            }
            nextAlarmTime.add(Calendar.MINUTE, 5);
            setAlarm(context, nextAlarmTime.getTimeInMillis());
            return;
        }

        Calendar lastCallTime = (Calendar) bedTime.clone();
        lastCallTime.add(Calendar.MINUTE, -15);

        Calendar nextAlarmTime;

        if (!now.before(lastCallTime)) {
            nextAlarmTime = (Calendar) wakeUpTime.clone();
            if (!nextAlarmTime.after(now)) {
                nextAlarmTime.add(Calendar.DAY_OF_YEAR, 1);
            }
            nextAlarmTime.add(Calendar.MINUTE, 5);
        } else {
            Calendar potentialNextTime = (Calendar) now.clone();
            potentialNextTime.add(Calendar.MINUTE, waterIntervalMins); // Dinamik aralık kullanıyoruz

            if (potentialNextTime.after(lastCallTime)) {
                nextAlarmTime = lastCallTime;
            } else {
                nextAlarmTime = potentialNextTime;
            }
        }

        setAlarm(context, nextAlarmTime.getTimeInMillis());
    }

    private void setAlarm(Context context, long timeInMillis) {
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