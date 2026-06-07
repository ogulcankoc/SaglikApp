package com.example.saglikapp.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.saglikapp.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {

    private ProgressBar pbDownload;
    private TextView tvModelStatus;
    private Button btnDownloadModel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Thread downloadThread;

    private static final String MODEL_NAME = "gemma-4-E2B-it.litertlm";
    private static final String MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Switch switchDarkMode = findViewById(R.id.switchDarkMode);
        EditText editWaterInterval = findViewById(R.id.editWaterInterval);
        Button btnSaveWaterInterval = findViewById(R.id.btnSaveWaterInterval);
        Button btnGoProfile = findViewById(R.id.btnGoProfile);
        EditText editNap = findViewById(R.id.editNapDuration);
        EditText editFallAsleep = findViewById(R.id.editFallAsleepDuration);
        EditText editCycle = findViewById(R.id.editCycleDuration);
        Button btnSaveSleep = findViewById(R.id.btnSaveSleepSettings);
        tvModelStatus = findViewById(R.id.tvModelStatus);
        pbDownload = findViewById(R.id.pbDownload);
        btnDownloadModel = findViewById(R.id.btnDownloadModel);

        SharedPreferences prefs = getSharedPreferences("UserData", MODE_PRIVATE);

        // Koyu Mod
        switchDarkMode.setChecked(prefs.getBoolean("darkMode", false));
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("darkMode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Su Bildirim Aralığı (Dakika)
        int savedInterval = prefs.getInt("waterIntervalMinutes", 120);
        editWaterInterval.setText(String.valueOf(savedInterval));
        btnSaveWaterInterval.setOnClickListener(v -> {
            try {
                int minutes = Integer.parseInt(editWaterInterval.getText().toString());
                if (minutes >= 5 && minutes <= 120) {
                    prefs.edit().putInt("waterIntervalMinutes", minutes).apply();
                    Toast.makeText(this, "Aralık " + minutes + " dk olarak ayarlandı.", Toast.LENGTH_SHORT).show();
                    
                    Intent intent = new Intent(SettingsActivity.this, com.example.saglikapp.receiver.AlarmReceiver.class);
                    intent.putExtra("type", "reschedule");
                    sendBroadcast(intent);
                } else {
                    Toast.makeText(this, "5-120 dk arası bir değer girin.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Lütfen sayı girin.", Toast.LENGTH_SHORT).show();
            }
        });

        // Uyku Ayarları
        editNap.setText(String.valueOf(prefs.getInt("napDuration", 25)));
        editFallAsleep.setText(String.valueOf(prefs.getInt("fallAsleepDuration", 15)));
        editCycle.setText(String.valueOf(prefs.getInt("cycleDuration", 90)));
        btnSaveSleep.setOnClickListener(v -> {
            try {
                int n = Integer.parseInt(editNap.getText().toString());
                int f = Integer.parseInt(editFallAsleep.getText().toString());
                int c = Integer.parseInt(editCycle.getText().toString());
                if (n > 0 && f >= 0 && c > 0) {
                    prefs.edit()
                            .putInt("napDuration", n)
                            .putInt("fallAsleepDuration", f)
                            .putInt("cycleDuration", c)
                            .apply();
                    Toast.makeText(this, "Uyku ayarları kaydedildi!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Hata", Toast.LENGTH_SHORT).show();
            }
        });

        btnGoProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        checkModelStatus();
        btnDownloadModel.setOnClickListener(v -> startModelDownload());
    }

    private void checkModelStatus() {
        File internalModel = new File(getFilesDir(), MODEL_NAME);
        long expectedMinSize = 2_400_000_000L;

        if (internalModel.exists() && internalModel.length() > expectedMinSize) {
            tvModelStatus.setText("Durum: Model yüklü ✅ (" +
                    String.format("%.1f GB", internalModel.length() / 1_000_000_000f) + ")");
            btnDownloadModel.setText("Modeli Yeniden İndir");
            pbDownload.setVisibility(View.GONE);
        } else {
            if (internalModel.exists()) {
                internalModel.delete();
                tvModelStatus.setText("Durum: Model bozuk, yeniden indirilmeli ❌");
            } else {
                tvModelStatus.setText("Durum: Model eksik ❌");
            }
            btnDownloadModel.setText("Modeli İndir (2.5 GB)");
        }
    }

    private void startModelDownload() {
        File targetFile = new File(getFilesDir(), MODEL_NAME);
        if (targetFile.exists()) targetFile.delete();

        btnDownloadModel.setEnabled(false);
        pbDownload.setVisibility(View.VISIBLE);
        pbDownload.setProgress(0);
        tvModelStatus.setText("Durum: İndiriliyor...");

        downloadThread = new Thread(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(MODEL_URL)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();

            try {
                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        tvModelStatus.setText("Hata: Sunucu yanıt vermedi (" + response.code() + ")");
                        btnDownloadModel.setEnabled(true);
                        pbDownload.setVisibility(View.GONE);
                    });
                    return;
                }

                long totalBytes = response.body().contentLength();
                long[] downloadedBytes = {0};
                byte[] buffer = new byte[8192];
                int read;

                try (InputStream inputStream = response.body().byteStream();
                     FileOutputStream outputStream = new FileOutputStream(targetFile)) {

                    while ((read = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, read);
                        downloadedBytes[0] += read;

                        if (totalBytes > 0) {
                            int progress = (int) ((downloadedBytes[0] * 100L) / totalBytes);
                            String mb = String.format("%.1f / %.1f MB",
                                    downloadedBytes[0] / 1_000_000f,
                                    totalBytes / 1_000_000f);
                            handler.post(() -> {
                                pbDownload.setProgress(progress);
                                tvModelStatus.setText("İndiriliyor: " + mb + " (" + progress + "%)");
                            });
                        }
                    }
                }

                if (targetFile.length() > 2_400_000_000L) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Model başarıyla indirildi!", Toast.LENGTH_LONG).show();
                        checkModelStatus();
                        btnDownloadModel.setEnabled(true);
                    });
                } else {
                    targetFile.delete();
                    runOnUiThread(() -> {
                        tvModelStatus.setText("Hata: İndirme tamamlanamadı, tekrar deneyin ❌");
                        btnDownloadModel.setEnabled(true);
                        pbDownload.setVisibility(View.GONE);
                    });
                }

            } catch (Exception e) {
                targetFile.delete();
                runOnUiThread(() -> {
                    tvModelStatus.setText("Hata: " + e.getMessage());
                    btnDownloadModel.setEnabled(true);
                    pbDownload.setVisibility(View.GONE);
                });
            }
        });
        downloadThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (downloadThread != null && downloadThread.isAlive()) {
            downloadThread.interrupt();
        }
    }
}