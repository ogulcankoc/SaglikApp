package com.example.saglikapp.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.R;
import com.example.saglikapp.receiver.AlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private ProfileViewModel viewModel;
    private EditText editName, editAge, editWeight, editHeight, editWakeUpTime, editBedTime;
    private RadioGroup radioGender;

    private static final int ALARM_REQUEST_CODE = 100;
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        editName = findViewById(R.id.editProfileName);
        editAge = findViewById(R.id.editProfileAge);
        editWeight = findViewById(R.id.editProfileWeight);
        editHeight = findViewById(R.id.editProfileHeight);
        editWakeUpTime = findViewById(R.id.editWakeUpTime);
        editBedTime = findViewById(R.id.editBedTime);
        radioGender = findViewById(R.id.radioProfileGender);
        Button btnSave = findViewById(R.id.btnSaveProfile);

        // Mevcut verileri yükle
        editName.setText(viewModel.getUserData("name"));
        editAge.setText(viewModel.getUserData("age"));
        editWeight.setText(viewModel.getUserData("weight"));
        editHeight.setText(viewModel.getUserData("height"));
        editWakeUpTime.setText(viewModel.getUserData("wakeUpTime"));
        editBedTime.setText(viewModel.getUserData("bedTime"));

        String gender = viewModel.getGender();
        if ("Erkek".equals(gender)) {
            ((RadioButton) findViewById(R.id.radioProfileMale)).setChecked(true);
        } else if ("Kadın".equals(gender)) {
            ((RadioButton) findViewById(R.id.radioProfileFemale)).setChecked(true);
        }

        editWakeUpTime.setOnClickListener(v -> showTimePicker(editWakeUpTime));
        editBedTime.setOnClickListener(v -> showTimePicker(editBedTime));

        btnSave.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            String newAge = editAge.getText().toString().trim();
            String newWeight = editWeight.getText().toString().trim();
            String newHeight = editHeight.getText().toString().trim();
            String newWakeUp = editWakeUpTime.getText().toString().trim();
            String newBedTime = editBedTime.getText().toString().trim();

            int selectedGenderId = radioGender.getCheckedRadioButtonId();
            String newGender = selectedGenderId != -1 ? ((RadioButton) findViewById(selectedGenderId)).getText().toString() : "";

            if (!viewModel.validateInputs(newName, newAge, newWeight, newHeight, newGender, newWakeUp, newBedTime)) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verileri kaydet
            viewModel.saveProfileData(newName, newAge, newWeight, newHeight, newGender, newWakeUp, newBedTime);

            // Yeni alarm zamanını hesapla
            Calendar alarmTime = new MainViewModel.AlarmCalculator()
                    .calculateFirstAlarmTime(this, newWakeUp, newBedTime);

            if (alarmTime != null) {
                scheduleFirstAlarm(alarmTime);

                Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Saat formatı hatalı, alarm kurulamadı.", Toast.LENGTH_SHORT).show();
            }
        });
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

        // Eski alarmları temizle
        alarmManager.cancel(pendingIntent);

        long alarmTimeMillis = alarmTime.getTimeInMillis();

        // GEÇMİŞ ZAMAN KORUMASI: Eğer hesaplanan zaman şu andan küçükse 10 saniye sonraya kur
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
            Log.e(TAG, "Exact alarm izni yok, normal alarm kuruluyor.");
            alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTimeMillis, pendingIntent);
        }

        // Kullanıcıya bilgi mesajı
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", new Locale("tr"));
        String time = timeFormat.format(alarmTime.getTime());

        Calendar now = Calendar.getInstance();
        boolean isToday = now.get(Calendar.DAY_OF_YEAR) == alarmTime.get(Calendar.DAY_OF_YEAR);
        String message;

        if (isToday) {
            message = "Bilgiler güncellendi. Su hatırlatıcısı bugün saat " + time + "'de başlayacak.";
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM", new Locale("tr"));
            String date = dateFormat.format(alarmTime.getTime());
            message = "Bilgiler güncellendi. Su hatırlatıcısı " + date + " saat " + time + "'de başlayacak.";
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.i(TAG, "✅ " + message);
    }

    private void showTimePicker(EditText targetEditText) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    // Locale.US kullanarak rakamların her dilde 0-9 formatında olmasını sağlıyoruz
                    String formattedTime = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);
                    targetEditText.setText(formattedTime);
                }, hour, minute, true);

        timePickerDialog.show();
    }
}