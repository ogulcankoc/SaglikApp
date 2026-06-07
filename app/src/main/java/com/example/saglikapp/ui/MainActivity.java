package com.example.saglikapp.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
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
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.R;
import com.example.saglikapp.receiver.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private static final int NOTIFICATION_PERMISSION_CODE = 100;
    private static final int ALARM_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        if (viewModel.isUserRegistered()) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
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
            String gender = selectedGenderId != -1 ? ((RadioButton) findViewById(selectedGenderId)).getText().toString() : "";

            if (!viewModel.validateInputs(name, age, weight, height, gender, wakeUp, bedTime)) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.saveUserData(name, age, weight, height, gender, wakeUp, bedTime);

            // ViewModel üzerinden güvenli zaman hesaplaması
            Calendar alarmTime = new MainViewModel.AlarmCalculator()
                    .calculateFirstAlarmTime(this, wakeUp, bedTime);

            if (alarmTime != null) {
                scheduleFirstAlarm(alarmTime);
                Toast.makeText(this, "Hoşgeldiniz! Su hatırlatıcısı kuruldu.", Toast.LENGTH_SHORT).show();

                startActivity(new Intent(this, WelcomeActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Saat formatı hatalı, alarm kurulamadı.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            String packageName = getPackageName();

            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                new AlertDialog.Builder(this)
                        .setTitle("Pil Kısıtlaması")
                        .setMessage("Bildirimlerin zamanında gelmesi için pil kısıtlamasını 'Kısıtlama Yok' olarak seçmelisiniz.")
                        .setPositiveButton("Ayarları Aç", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                intent.setData(Uri.parse("package:" + packageName));
                                startActivity(intent);
                            } catch (Exception e) {
                                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            }
        }
    }

    private void scheduleFirstAlarm(Calendar alarmTime) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("type", "water");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Önceki olası alarmları temizle
        alarmManager.cancel(pendingIntent);

        long alarmTimeMillis = alarmTime.getTimeInMillis();

        // Geçmiş zaman koruması: Eğer hesaplanan zaman şu andan küçükse 10 saniye sonraya kur
        if (alarmTimeMillis <= System.currentTimeMillis()) {
            alarmTimeMillis = System.currentTimeMillis() + 10000;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            // Android 14+ için tam zamanlı alarm izni yoksa normal alarm kur
            Log.e(TAG, "Exact alarm izni yok, normal alarm kuruluyor.");
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM HH:mm", new Locale("tr"));
        Log.i(TAG, "✅ İlk Alarm Planlandı: " + sdf.format(alarmTime.getTime()));
    }

    private void showTimePicker(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    // Locale.US kullanarak rakamların her dilde 0-9 formatında (ASCII) olmasını sağlıyoruz
                    String formattedTime = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);
                    targetEditText.setText(formattedTime);
                }, hour, minute, true);

        timePickerDialog.show();
    }
}