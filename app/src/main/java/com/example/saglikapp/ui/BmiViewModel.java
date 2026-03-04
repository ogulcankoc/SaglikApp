package com.example.saglikapp.ui;

import androidx.lifecycle.ViewModel;

public class BmiViewModel extends ViewModel {

    // Hesaplanan sonuçları tutacağımız değişkenler
    private double bmi;
    private double idealMin;
    private double idealMax;
    private String category;

    // Activity'nin çağıracağı hesaplama metodu
    public void calculateBmi(double heightCm, double weightKg) {
        double hM = heightCm / 100.0;
        this.bmi = weightKg / (hM * hM);
        this.idealMin = 18.5 * hM * hM;
        this.idealMax = 24.9 * hM * hM;

        if (this.bmi < 18.5) {
            this.category = "Zayıf";
        } else if (this.bmi < 25.0) {
            this.category = "Normal";
        } else if (this.bmi < 30.0) {
            this.category = "Fazla kilolu";
        } else {
            this.category = "Obez";
        }
    }

    // Activity'nin sonuçları okuyabilmesi için Getter metodları
    public double getBmi() { return bmi; }
    public double getIdealMin() { return idealMin; }
    public double getIdealMax() { return idealMax; }
    public String getCategory() { return category; }
}