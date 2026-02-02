package com.example.saglikapp;

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
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "SaglikAppChannel";

    @Override
    public void onReceive(Context context, Intent intent) {

        Toast.makeText(context, "Alarm Tetiklendi!", Toast.LENGTH_LONG).show();
        // Gelen alarmın türüne bakıyoruz
        String alarmType = intent.getStringExtra("type");

        if ("water".equals(alarmType)) {
            // --- SENARYO A: SU HATIRLATMA ---
            // 1. Bildirimi Göster
            showWaterNotification(context);
            // 2. Bir Sonraki Alarmı Hesapla (Akıllı Döngü)
            scheduleNextAlarm(context);
        } else {
            // --- SENARYO B: UYKU ALARMI (ESKİ SİSTEM) ---
            // Burası senin orijinal kodun gibi çalışır, uyku döngüsünü bozmaz.
            String message = intent.getStringExtra("message");
            if (message == null) message = "Uyanma Vakti!";

            showSleepNotification(context, message);
        }
    }

    // --- SU BİLDİRİMİ ---
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

    // --- UYKU BİLDİRİMİ (ESKİSİNİ KORUYORUZ) ---
    private void showSleepNotification(Context context, String message) {
        Toast.makeText(context, "Alarm: " + message, Toast.LENGTH_LONG).show();

        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Uyku Alarmı 🌙")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notify(context, 1002, builder);
    }

    // --- AKILLI HESAPLAMA (Sadece Su İçin) ---
    private void scheduleNextAlarm(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String wakeUpStr = sharedPref.getString("wakeUpTime", "08:00");
        String bedTimeStr = sharedPref.getString("bedTime", "23:00");

        Calendar now = Calendar.getInstance();

        // ✅ Bugünün uyanma ve yatma saatlerini doğru hesapla
        Calendar wakeUpTime = getCalendarFromTime(wakeUpStr);
        Calendar bedTime = getCalendarFromTime(bedTimeStr);

        // ✅ Eğer yatma saati uyanmadan önce ise (örn: Yatış 23:00, Uyanış 07:00)
        // Yatma saatini yarına taşı
        if (bedTime.before(wakeUpTime)) {
            bedTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        // ✅ Eğer şu an yatma saatinden sonraysa (örn: saat 01:00 ise)
        // Uyanma ve yatma saatlerini yarına taşı
        if (now.after(bedTime)) {
            wakeUpTime.add(Calendar.DAY_OF_YEAR, 1);
            bedTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Son Çağrı: Yatmadan 15 dakika önce
        Calendar lastCallTime = (Calendar) bedTime.clone();
        lastCallTime.add(Calendar.MINUTE, -15);

        Calendar nextAlarmTime;

        // SENARYO 1: Gün bitti mi? (Yatmadan önceki son 15 dk içindeysek veya geçtiysek)
        if (now.after(lastCallTime) || now.equals(lastCallTime)) {
            // ✅ Yarının uyanma saati + 5dk
            nextAlarmTime = (Calendar) wakeUpTime.clone();
            if (!wakeUpTime.after(now)) {
                nextAlarmTime.add(Calendar.DAY_OF_YEAR, 1);
            }
            nextAlarmTime.add(Calendar.MINUTE, 5);

            Log.d("AlarmReceiver", "Gün bitti, yarın sabah alarm kuruldu");
        } else {
            // SENARYO 2: Gün içindeyiz
            Calendar potentialNextTime = (Calendar) now.clone();
            potentialNextTime.add(Calendar.HOUR_OF_DAY, 2); // +2 Saat

            // Eğer +2 saat yatma vaktini geçiyorsa, son çağrıya (Yatış-15dk) kur
            if (potentialNextTime.after(lastCallTime)) {
                nextAlarmTime = lastCallTime;
                Log.d("AlarmReceiver", "Son alarm kuruldu (yatış-15dk)");
            } else {
                nextAlarmTime = potentialNextTime;
                Log.d("AlarmReceiver", "Normal 2 saatlik alarm kuruldu");
            }
        }

        // ✅ Debug log ekle
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", new Locale("tr"));
        Log.i("AlarmReceiver", "Sonraki alarm: " + sdf.format(nextAlarmTime.getTime()));

        setAlarm(context, nextAlarmTime.getTimeInMillis());
    }

    // --- GÜVENLİ ALARM KURMA (HATA DÜZELTİLDİ) ---
    private void setAlarm(Context context, long timeInMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("type", "water"); // Alarmın türünü SU olarak işaretliyoruz

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            try {
                // Android 12 (API 31) ve üzeri için "Tam Zamanlı Alarm" izni kontrolü
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                    } else {
                        // İzin yoksa çökmemesi için hassas olmayan alarm kur
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                    }
                }
                // Android 6 (API 23) ile Android 11 arası
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                }
                // Daha eski sürümler
                else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
                }
            } catch (SecurityException e) {
                // Olası bir güvenlik hatasında uygulama çökmesin diye standart kurulum
                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
            }
        }
    }

    private Calendar getCalendarFromTime(String timeStr) {
        Calendar cal = Calendar.getInstance();
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, minute);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } catch (Exception e) {}
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