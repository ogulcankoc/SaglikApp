package com.example.saglikapp.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HeartRateDao {
    @Insert
    void insert(HeartRateLog log);

    @Query("SELECT * FROM heart_rate_logs ORDER BY timestamp DESC LIMIT 10")
    List<HeartRateLog> getLastTenReadings();

    @Query("SELECT * FROM heart_rate_logs ORDER BY timestamp DESC")
    List<HeartRateLog> getAllReadings();
}
