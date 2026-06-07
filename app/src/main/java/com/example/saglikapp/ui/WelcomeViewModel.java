package com.example.saglikapp.ui;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class WelcomeViewModel extends AndroidViewModel {

    // Veri saklama aracımız
    private final SharedPreferences sharedPref;

    public WelcomeViewModel(@NonNull Application application) {
        super(application);
        // SharedPreferences tanımlamasını Activity'den buraya taşıdık
        sharedPref = application.getSharedPreferences("UserData", Context.MODE_PRIVATE);
    }

    // İş mantığı (Business Logic): Sadece isim + hoşgeldin mesajı
    public String getWelcomeMessage() {
        String name = sharedPref.getString("name", "Kullanıcı");
        return name + " hoşgeldin";
    }
}