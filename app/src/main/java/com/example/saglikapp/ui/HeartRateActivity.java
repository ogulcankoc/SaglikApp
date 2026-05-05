package com.example.saglikapp.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.saglikapp.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class HeartRateActivity extends AppCompatActivity {

    private static final String TAG = "HeartRateActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView previewView;
    private TextView textBPM;
    private ProgressBar progressBar;
    private Button btnStartStop;
    private LineChart chart;

    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private boolean isMeasuring = false;

    // Rolling Average Algorithm variables from user example
    private static final AtomicBoolean processing = new AtomicBoolean(false);
    private int averageIndex = 0;
    private static final int averageArraySize = 4;
    private final int[] averageArray = new int[averageArraySize];

    public enum TYPE {
        GREEN, RED
    }
    private TYPE currentType = TYPE.GREEN;

    private int beatsIndex = 0;
    private static final int beatsArraySize = 3;
    private final int[] beatsArray = new int[beatsArraySize];
    private double beats = 0;
    private long startTimeMillis = 0;
    private final int measurementDurationSec = 30;

    // Chart data
    private final List<Entry> chartEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate);

        previewView = findViewById(R.id.previewView);
        textBPM = findViewById(R.id.textBPM);
        progressBar = findViewById(R.id.progressBar);
        btnStartStop = findViewById(R.id.btnStartStop);
        chart = findViewById(R.id.chart);

        setupChart();

        btnStartStop.setOnClickListener(v -> {
            if (isMeasuring) {
                stopMeasurement();
            } else {
                if (allPermissionsGranted()) {
                    startMeasurement();
                } else {
                    ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
                }
            }
        });

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(false);
        chart.getAxisLeft().setDrawLabels(false);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);

        LineDataSet set = new LineDataSet(new ArrayList<>(), "Pulse");
        set.setColor(Color.RED);
        set.setDrawCircles(false);
        set.setLineWidth(2f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        
        chart.setData(new LineData(set));
        chart.invalidate();
    }

    private void startMeasurement() {
        isMeasuring = true;
        btnStartStop.setText("Durdur");
        beats = 0;
        beatsIndex = 0;
        averageIndex = 0;
        for (int i = 0; i < averageArraySize; i++) averageArray[i] = 0;
        for (int i = 0; i < beatsArraySize; i++) beatsArray[i] = 0;
        
        chartEntries.clear();
        startTimeMillis = System.currentTimeMillis();
        progressBar.setProgress(0);
        textBPM.setText("--");

        // Clear chart data properly
        LineData data = chart.getData();
        if (data != null) {
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
            if (set != null) {
                set.clear();
                data.notifyDataChanged();
                chart.setData(data);
                chart.invalidate();
            }
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera provider failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void stopMeasurement() {
        isMeasuring = false;
        btnStartStop.setText("Ölçümü Başlat");
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (camera != null) {
            camera.getCameraControl().enableTorch(false);
        }
        processing.set(false);
    }

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            camera.getCameraControl().enableTorch(true);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        if (!isMeasuring) {
            image.close();
            return;
        }

        if (!processing.compareAndSet(false, true)) {
            image.close();
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int imgAvg = decodeYUV420SPtoRedAvg(image);

        if (imgAvg == 0 || imgAvg == 255) {
            processing.set(false);
            image.close();
            return;
        }

        // Rolling Average Algorithm logic from user's example
        int averageArrayAvg = 0;
        int averageArrayCnt = 0;
        for (int i = 0; i < averageArray.length; i++) {
            if (averageArray[i] > 0) {
                averageArrayAvg += averageArray[i];
                averageArrayCnt++;
            }
        }

        int rollingAverage = (averageArrayCnt > 0) ? (averageArrayAvg / averageArrayCnt) : 0;
        TYPE newType = currentType;
        if (imgAvg < rollingAverage) {
            newType = TYPE.RED;
            if (newType != currentType) {
                beats++;
            }
        } else if (imgAvg > rollingAverage) {
            newType = TYPE.GREEN;
        }

        if (averageIndex == averageArraySize) averageIndex = 0;
        averageArray[averageIndex] = imgAvg;
        averageIndex++;

        if (newType != currentType) {
            currentType = newType;
        }

        long currentTime = System.currentTimeMillis();
        long totalTimeInSecs = (currentTime - startTimeMillis) / 1000;
        int progress = (int) (totalTimeInSecs * 100 / measurementDurationSec);

        runOnUiThread(() -> {
            progressBar.setProgress(progress);
            updateChart(imgAvg);
            
            if (totalTimeInSecs >= 10) {
                double bps = (beats / (double)totalTimeInSecs);
                int dpm = (int) (bps * 60d);
                
                if (dpm >= 30 && dpm <= 180) {
                    if (beatsIndex == beatsArraySize) beatsIndex = 0;
                    beatsArray[beatsIndex] = dpm;
                    beatsIndex++;

                    int beatsArrayAvg = 0;
                    int beatsArrayCnt = 0;
                    for (int i = 0; i < beatsArray.length; i++) {
                        if (beatsArray[i] > 0) {
                            beatsArrayAvg += beatsArray[i];
                            beatsArrayCnt++;
                        }
                    }
                    int beatsAvg = (beatsArrayAvg / beatsArrayCnt);
                    textBPM.setText(String.valueOf(beatsAvg));
                }

                if (totalTimeInSecs >= measurementDurationSec) {
                    Toast.makeText(HeartRateActivity.this, "Ölçüm Tamamlandı", Toast.LENGTH_SHORT).show();
                    stopMeasurement();
                }
            }
        });

        processing.set(false);
        image.close();
    }

    private int decodeYUV420SPtoRedAvg(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        int width = image.getWidth();
        int height = image.getHeight();
        
        long sum = 0;
        int frameSize = width * height;
        
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) nv21[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & nv21[uvp++]) - 128;
                    uvp++; // Skip U channel as we only need V for Red
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                if (r < 0) r = 0; else if (r > 262143) r = 262143;

                int pixelRed = (r >> 10) & 0xff;
                sum += pixelRed;
            }
        }
        return (int) (sum / frameSize);
    }

    private void updateChart(int val) {
        chartEntries.add(new Entry(chartEntries.size(), val));
        if (chartEntries.size() > 100) {
            chartEntries.remove(0);
            for (int i = 0; i < chartEntries.size(); i++) {
                chartEntries.get(i).setX(i);
            }
        }
        
        LineData data = chart.getData();
        if (data != null) {
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
            if (set != null) {
                // Ensure thread safety and avoid synchronization issues
                set.setValues(new ArrayList<>(chartEntries));
                data.notifyDataChanged();
                // MPAndroidChart 3.1.0 logic for updating data
                chart.setData(data);
                chart.invalidate();
            }
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startMeasurement();
            } else {
                Toast.makeText(this, "Kamera izni verilmedi.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}