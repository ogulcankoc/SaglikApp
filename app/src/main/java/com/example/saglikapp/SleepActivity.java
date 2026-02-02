    package com.example.saglikapp;

    import android.content.Intent;
    import android.os.Bundle;
    import android.provider.AlarmClock;
    import android.widget.Button;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.appcompat.app.AppCompatActivity;

    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.Locale;

    public class SleepActivity extends AppCompatActivity {

        private TextView textCurrentTime;
        private Button btnNap, btnCycle1, btnCycle2, btnCycle3, btnCycle4, btnCycle5, btnCycle6;
        private Calendar now;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_sleep);

            textCurrentTime = findViewById(R.id.textCurrentTime);
            btnNap = findViewById(R.id.btnNap);
            btnCycle1 = findViewById(R.id.btnCycle1);
            btnCycle2 = findViewById(R.id.btnCycle2);
            btnCycle3 = findViewById(R.id.btnCycle3);
            btnCycle4 = findViewById(R.id.btnCycle4);
            btnCycle5 = findViewById(R.id.btnCycle5);
            btnCycle6 = findViewById(R.id.btnCycle6);

            now = Calendar.getInstance();
            updateTimeDisplay();

            ArrayList<Calendar> times = calculateSleepCycles();

            btnNap.setText(formatTime(times.get(0)) + "\nKestirme 25 dk");
            btnCycle1.setText(formatTime(times.get(1)) + "\n1 döngü 1s 45dk");
            btnCycle2.setText(formatTime(times.get(2)) + "\n2 döngü 3s 15dk");
            btnCycle3.setText(formatTime(times.get(3)) + "\n3 döngü 4s 45dk");
            btnCycle4.setText(formatTime(times.get(4)) + "\n4 döngü 6s 15dk");
            btnCycle5.setText(formatTime(times.get(5)) + "\n5 döngü 7s 45dk");
            btnCycle6.setText(formatTime(times.get(6)) + "\n6 döngü 9s 15dk");

            setAlarmOnClick(btnNap, times.get(0), "Kestirme");
            setAlarmOnClick(btnCycle1, times.get(1), "1 uyku döngüsü");
            setAlarmOnClick(btnCycle2, times.get(2), "2 uyku döngüsü");
            setAlarmOnClick(btnCycle3, times.get(3), "3 uyku döngüsü");
            setAlarmOnClick(btnCycle4, times.get(4), "4 uyku döngüsü");
            setAlarmOnClick(btnCycle5, times.get(5), "5 uyku döngüsü");
            setAlarmOnClick(btnCycle6, times.get(6), "6 uyku döngüsü");
        }

        private void updateTimeDisplay() {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            textCurrentTime.setText("Şu anda saat: " + sdf.format(now.getTime()));
        }

        private ArrayList<Calendar> calculateSleepCycles() {
            ArrayList<Calendar> times = new ArrayList<>();

            Calendar nap = (Calendar) now.clone();
            nap.add(Calendar.MINUTE, 25);
            times.add(nap);

            Calendar base = (Calendar) now.clone();
            base.add(Calendar.MINUTE, 15);

            for (int i = 1; i <= 6; i++) {
                Calendar c = (Calendar) base.clone();
                c.add(Calendar.MINUTE, i * 90);
                times.add(c);
            }

            return times;
        }

        private String formatTime(Calendar c) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(c.getTime());
        }

        private void setAlarmOnClick(Button btn, Calendar time, String message) {
            btn.setOnClickListener(v -> setAlarm(time, message));
        }

        private void setAlarm(Calendar time, String message) {
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            intent.putExtra(AlarmClock.EXTRA_HOUR, time.get(Calendar.HOUR_OF_DAY));
            intent.putExtra(AlarmClock.EXTRA_MINUTES, time.get(Calendar.MINUTE));
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, message);

            try {
                startActivity(intent);
            } catch (Exception e) {
                // Basit fallback - alarm listesini aç
                try {
                    Intent showAlarms = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                    startActivity(showAlarms);
                    Toast.makeText(this,
                            "Alarm listesi açıldı. " + formatTime(time) + " saatine alarm kurun.",
                            Toast.LENGTH_LONG).show();
                } catch (Exception e2) {
                    Toast.makeText(this,
                            "Alarm kurulamadı. Saat: " + formatTime(time),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }