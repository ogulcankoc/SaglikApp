package com.example.saglikapp.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "heart_rate_logs")
public class HeartRateLog {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private int bpm;
    private long timestamp;

    public HeartRateLog(int bpm, long timestamp) {
        this.bpm = bpm;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBpm() { return bpm; }
    public void setBpm(int bpm) { this.bpm = bpm; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
