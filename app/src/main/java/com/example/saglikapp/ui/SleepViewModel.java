package com.example.saglikapp.ui;

import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class SleepViewModel extends ViewModel {

    private Calendar now;
    private ArrayList<Calendar> sleepCycles;

    public SleepViewModel() {
        // ViewModel ilk oluşturulduğunda saatleri hesapla
        calculateCycles();
    }

    // Zaman ve döngü hesaplama (Activity'den buraya taşıdık)
    public void calculateCycles() {
        now = Calendar.getInstance();
        sleepCycles = new ArrayList<>();

        // Kestirme (25 dk)
        Calendar nap = (Calendar) now.clone();
        nap.add(Calendar.MINUTE, 25);
        sleepCycles.add(nap);

        // Uykuya dalma süresi (15 dk eklendikten sonra döngüler başlar)
        Calendar base = (Calendar) now.clone();
        base.add(Calendar.MINUTE, 15);

        // 6 adet uyku döngüsü (Her biri 1.5 saat = 90 dk)
        for (int i = 1; i <= 6; i++) {
            Calendar c = (Calendar) base.clone();
            c.add(Calendar.MINUTE, i * 90);
            sleepCycles.add(c);
        }
    }

    public Calendar getNow() {
        return now;
    }

    // İstenilen sıradaki alarm zamanını döndürür
    public Calendar getCycleTime(int index) {
        if (sleepCycles != null && index >= 0 && index < sleepCycles.size()) {
            return sleepCycles.get(index);
        }
        return null;
    }

    // Saati UI'da göstermek için (HH:mm) formatına çevirir
    public String getFormattedTime(Calendar c) {
        if (c == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(c.getTime());
    }
}