package com.example.saglikapp.ui;

import androidx.lifecycle.ViewModel;

public class BmiViewModel extends ViewModel {

    private double bmi;
    private double idealMin;
    private double idealMax;
    private String category;
    private double bmr;
    private String advice;
    private double weightDiff;

    public void calculateBmi(double heightCm, double weightKg, int age, String gender) {
        double hM = heightCm / 100.0;
        this.bmi = weightKg / (hM * hM);
        this.idealMin = 18.5 * hM * hM;
        this.idealMax = 24.9 * hM * hM;

        if (this.bmi < 18.5) {
            this.category = "Zayıf";
            this.weightDiff = idealMin - weightKg;
            this.advice = "Sağlıklı bir kiloya ulaşmak için besleyici gıdalarla kalori alımınızı artırabilirsiniz.";
        } else if (this.bmi < 25.0) {
            this.category = "Normal";
            this.weightDiff = 0;
            this.advice = "Harika! Mevcut kilonuzu korumak için dengeli beslenmeye devam edin.";
        } else if (this.bmi < 30.0) {
            this.category = "Fazla kilolu";
            this.weightDiff = weightKg - idealMax;
            this.advice = "Düzenli egzersiz ve porsiyon kontrolü ile ideal kilonuza ulaşabilirsiniz.";
        } else {
            this.category = "Obez";
            this.weightDiff = weightKg - idealMax;
            this.advice = "Sağlıklı bir yaşam için bir uzmandan destek alarak kilo vermeniz önerilir.";
        }

        // BMR Calculation (Harris-Benedict Equation)
        if ("Erkek".equalsIgnoreCase(gender)) {
            this.bmr = 88.362 + (13.397 * weightKg) + (4.799 * heightCm) - (5.677 * age);
        } else {
            this.bmr = 447.593 + (9.247 * weightKg) + (3.098 * heightCm) - (4.330 * age);
        }
    }

    public double getBmi() { return bmi; }
    public double getIdealMin() { return idealMin; }
    public double getIdealMax() { return idealMax; }
    public String getCategory() { return category; }
    public double getBmr() { return bmr; }
    public String getAdvice() { return advice; }
    public double getWeightDiff() { return weightDiff; }
}
