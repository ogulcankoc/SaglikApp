package com.example.saglikapp.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "water_logs")
public class WaterLog {
    @PrimaryKey
    @NonNull
    private String date; // Format: "yyyy-MM-dd" (Örn: 2024-03-20)

    private int totalAmount; // O gün içilen toplam su miktarı

    public WaterLog(@NonNull String date, int totalAmount) {
        this.date = date;
        this.totalAmount = totalAmount;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    public void setDate(@NonNull String date) {
        this.date = date;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(int totalAmount) {
        this.totalAmount = totalAmount;
    }
}
