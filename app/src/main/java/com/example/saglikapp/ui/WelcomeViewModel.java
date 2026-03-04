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

    // İş mantığı (Business Logic): Hoş geldin mesajını hazırlayıp UI'a hazır metin sunar
    public String getWelcomeMessage() {
        String name = sharedPref.getString("name", "Kullanıcı");
        String age = sharedPref.getString("age", "-");
        String weight = sharedPref.getString("weight", "-");
        String height = sharedPref.getString("height", "-");
        String gender = sharedPref.getString("gender", "-");

        return name + " hoşgeldin\n\n"
                + "Yaş: " + age + "\n"
                + "Kilo: " + weight + " kg\n"
                + "Boy: " + height + " cm\n"
                + "Cinsiyet: " + gender;
    }
}