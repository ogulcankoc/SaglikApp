package com.example.saglikapp.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

// entities kısmına oluşturduğumuz WaterLog sınıfını ekliyoruz
@Database(entities = {WaterLog.class}, version = 1, exportSchema = false)
public abstract class WaterDatabase extends RoomDatabase {

    private static volatile WaterDatabase instance;

    // DAO'ya erişim için soyut metod
    public abstract WaterDao waterDao();

    // Veritabanı nesnesini güvenli bir şekilde oluşturur (Singleton)
    public static WaterDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (WaterDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            WaterDatabase.class,
                            "water_database"
                    ).build();
                }
            }
        }
        return instance;
    }
}