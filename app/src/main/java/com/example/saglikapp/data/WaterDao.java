package com.example.saglikapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WaterDao {

    // Eğer o tarihte veri varsa üzerine yazar (Update), yoksa yeni ekler (Insert)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(WaterLog log);

    // Grafik için son 7 günü tarihe göre sıralı getirir
    @Query("SELECT * FROM water_logs ORDER BY date DESC LIMIT 7")
    List<WaterLog> getLastSevenDays();

    @Query("SELECT * FROM water_logs ORDER BY date DESC LIMIT 14")
    List<WaterLog> getLastFourteenDays();

    // Belirli bir tarihteki veriyi getirir
    @Query("SELECT * FROM water_logs WHERE date = :date LIMIT 1")
    WaterLog getLogByDate(String date);
}