package com.example.saglikapp;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPref;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    // Alarm sabitleri
    private static final int ALARM_DELAY_MINUTES = 5;
    private static final int FALLBACK_DELAY_HOURS = 2;
    private static final int ALARM_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getSharedPreferences("UserData", MODE_PRIVATE);

        // 📌 Daha önce kayıtlı kullanıcı var mı kontrol et
        if (sharedPref.contains("name")) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // 1. BİLDİRİM İZNİ İSTE (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }

        // 2. PİL TASARRUFU ENGELİNİ KALDIR
        checkBatteryOptimization();

        EditText editName = findViewById(R.id.editName);
        EditText editAge = findViewById(R.id.editAge);
        EditText editWeight = findViewById(R.id.editWeight);
        EditText editHeight = findViewById(R.id.editHeight);
        EditText editWakeUpTime = findViewById(R.id.editWakeUpTime);
        EditText editBedTime = findViewById(R.id.editBedTime);
        RadioGroup radioGender = findViewById(R.id.radioGender);
        Button btnStart = findViewById(R.id.btnStart);

        editWakeUpTime.setOnClickListener(v -> showTimePicker(editWakeUpTime));
        editBedTime.setOnClickListener(v -> showTimePicker(editBedTime));

        btnStart.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String age = editAge.getText().toString().trim();
            String weight = editWeight.getText().toString().trim();
            String height = editHeight.getText().toString().trim();
            String wakeUp = editWakeUpTime.getText().toString().trim();
            String bedTime = editBedTime.getText().toString().trim();

            int selectedGenderId = radioGender.getCheckedRadioButtonId();
            String gender = "";
            if (selectedGenderId != -1) {
                RadioButton selectedGender = findViewById(selectedGenderId);
                gender = selectedGender.getText().toString();
            }

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(age) ||
                    TextUtils.isEmpty(weight) || TextUtils.isEmpty(height) ||
                    TextUtils.isEmpty(gender) || TextUtils.isEmpty(wakeUp) ||
                    TextUtils.isEmpty(bedTime)) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("name", name);
            editor.putString("age", age);
            editor.putString("weight", weight);
            editor.putString("height", height);
            editor.putString("gender", gender);
            editor.putString("wakeUpTime", wakeUp);
            editor.putString("bedTime", bedTime);
            editor.apply();

            // Su hatırlatıcısı alarmını kur
            scheduleFirstAlarm(wakeUp, bedTime);

            Toast.makeText(this, "Hoşgeldiniz! Su hatırlatıcısı kuruldu.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // --- PİL OPTİMİZASYONU KONTROLÜ ---
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String packageName = getPackageName();

            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                new AlertDialog.Builder(this)
                        .setTitle("Pil Kısıtlaması")
                        .setMessage("Bildirimlerin zamanında gelmesi için pil kısıtlamasını 'Kısıtlama Yok' olarak seçmelisiniz.")
                        .setPositiveButton("Ayarları Aç", (dialog, which) -> {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + packageName));
                            startActivity(intent);
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            }
        }
    }

    // --- İLK ALARMI KURAN FONKSİYON (TAM DÜZELTİLMİŞ VERSİYON) ---
    private void scheduleFirstAlarm(String wakeUpTimeStr, String bedTimeStr) {
        Calendar now = Calendar.getInstance();
        Calendar alarmTime = Calendar.getInstance();

        try {
            // 1. Uyanma Saatini Parse Et ve Validate Et
            String[] wakeParts = wakeUpTimeStr.split(":");
            if (wakeParts.length != 2) {
                Log.e(TAG, "Geçersiz uyanma saati formatı: " + wakeUpTimeStr);
                Toast.makeText(this, "Geçersiz uyanma saati formatı", Toast.LENGTH_SHORT).show();
                return;
            }

            int wakeHour = Integer.parseInt(wakeParts[0].trim());
            int wakeMinute = Integer.parseInt(wakeParts[1].trim());

            if (wakeHour < 0 || wakeHour > 23 || wakeMinute < 0 || wakeMinute > 59) {
                Log.e(TAG, "Uyanma saati aralık dışında: " + wakeHour + ":" + wakeMinute);
                Toast.makeText(this, "Uyanma saati geçersiz", Toast.LENGTH_SHORT).show();
                return;
            }

            // 2. Yatma Saatini Parse Et ve Validate Et
            String[] bedParts = bedTimeStr.split(":");
            if (bedParts.length != 2) {
                Log.e(TAG, "Geçersiz yatma saati formatı: " + bedTimeStr);
                Toast.makeText(this, "Geçersiz yatma saati formatı", Toast.LENGTH_SHORT).show();
                return;
            }

            int bedHour = Integer.parseInt(bedParts[0].trim());
            int bedMinute = Integer.parseInt(bedParts[1].trim());

            if (bedHour < 0 || bedHour > 23 || bedMinute < 0 || bedMinute > 59) {
                Log.e(TAG, "Yatma saati aralık dışında: " + bedHour + ":" + bedMinute);
                Toast.makeText(this, "Yatma saati geçersiz", Toast.LENGTH_SHORT).show();
                return;
            }

            // 3. Bugünün Uyanma Zamanını Ayarla (Uyanma + 5 dakika)
            Calendar todayWakeTime = Calendar.getInstance();
            todayWakeTime.set(Calendar.HOUR_OF_DAY, wakeHour);
            todayWakeTime.set(Calendar.MINUTE, wakeMinute);
            todayWakeTime.set(Calendar.SECOND, 0);
            todayWakeTime.set(Calendar.MILLISECOND, 0);
            todayWakeTime.add(Calendar.MINUTE, ALARM_DELAY_MINUTES);

            // 4. Bugünün Yatma Zamanını Ayarla
            Calendar todayBedTime = Calendar.getInstance();
            todayBedTime.set(Calendar.HOUR_OF_DAY, bedHour);
            todayBedTime.set(Calendar.MINUTE, bedMinute);
            todayBedTime.set(Calendar.SECOND, 0);
            todayBedTime.set(Calendar.MILLISECOND, 0);

            // 5. Yatma saati gece yarısını geçiyorsa bir gün ekle
            // Örnek: Yatma 23:00, Uyanma 07:00 ise yatma yarına taşınmalı
            if (bedHour < wakeHour) {
                todayBedTime.add(Calendar.DAY_OF_YEAR, 1);
            }

            // 6. Alarm Zamanını Belirle
            if (now.before(todayWakeTime)) {
                // DURUM 1: Henüz uyanma saati (+ 5dk) gelmedi
                // Alarm = Bugün uyanma + 5dk
                alarmTime = todayWakeTime;
                Log.d(TAG, "Durum: Uyanma saati henüz gelmedi");

            } else if (now.before(todayBedTime)) {
                // DURUM 2: Uyanma geçti AMA yatma saati henüz gelmedi
                // Yani kullanıcı şu an uyanık
                // Alarm = Şimdi + 2 saat
                alarmTime = Calendar.getInstance();
                alarmTime.add(Calendar.HOUR_OF_DAY, FALLBACK_DELAY_HOURS);
                Log.d(TAG, "Durum: Uyanma geçti, yatma gelmedi (uyanık)");

            } else {
                // DURUM 3: Hem uyanma hem de yatma saati geçti
                // Yani kullanıcı muhtemelen uyuyor veya gün bitti
                // Alarm = Yarın uyanma + 5dk
                Calendar tomorrowWakeTime = (Calendar) todayWakeTime.clone();
                tomorrowWakeTime.add(Calendar.DAY_OF_YEAR, 1);
                alarmTime = tomorrowWakeTime;
                Log.d(TAG, "Durum: Gün bitti, yarın sabah alarm kurulacak");
            }

            // 7. AlarmManager ve Intent Hazırlığı
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager alınamadı");
                Toast.makeText(this, "Alarm sistemi kullanılamıyor", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("type", "water");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    ALARM_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // 8. Eski Alarmı İptal Et
            alarmManager.cancel(pendingIntent);

            // 9. Yeni Alarm Kur (Android Sürümüne Göre)
            long alarmTimeMillis = alarmTime.getTimeInMillis();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTimeMillis,
                            pendingIntent
                    );
                    Log.d(TAG, "Android 12+ Exact alarm kuruldu");
                } else {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTimeMillis,
                            pendingIntent
                    );
                    Log.w(TAG, "Android 12+ Exact alarm yetkisi yok, yaklaşık alarm kuruldu");
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6-11
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                );
                Log.d(TAG, "Android 6-11 Exact alarm kuruldu");
            } else {
                // Android 5 ve altı
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmTimeMillis,
                        pendingIntent
                );
                Log.d(TAG, "Android 5- Exact alarm kuruldu");
            }

            // 10. Kullanıcıya Bildir
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM EEEE HH:mm", new Locale("tr"));
            String formattedTime = sdf.format(alarmTime.getTime());
            Log.i(TAG, "✅ Alarm başarıyla kuruldu: " + formattedTime);

            // Opsiyonel: Toast ile kullanıcıya göster
             Toast.makeText(this, "İlk alarm: " + formattedTime, Toast.LENGTH_LONG).show();

        } catch (NumberFormatException e) {
            Log.e(TAG, "Saat parse hatası: " + e.getMessage(), e);
            Toast.makeText(this, "Saat formatı hatalı", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Alarm kurulurken beklenmeyen hata: " + e.getMessage(), e);
            Toast.makeText(this, "Alarm kurulamadı", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimePicker(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    targetEditText.setText(formattedTime);
                }, hour, minute, true);

        timePickerDialog.show();
    }
}