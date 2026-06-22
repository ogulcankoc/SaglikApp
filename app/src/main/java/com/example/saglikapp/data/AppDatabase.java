package com.example.saglikapp.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


@Database(entities = {WaterLog.class, HeartRateLog.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    // DAO'lara erişim için soyut metodlar
    public abstract WaterDao waterDao();
    public abstract HeartRateDao heartRateDao();
    


    // Veritabanı nesnesini güvenli bir şekilde oluşturur (Singleton)
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "saglik_database"
                    )
                    .fallbackToDestructiveMigration() // Veritabanı şeması değiştiğinde eski verileri silip yenisini kurar
                    .build();
                }
            }
        }
        return instance;
    }
}