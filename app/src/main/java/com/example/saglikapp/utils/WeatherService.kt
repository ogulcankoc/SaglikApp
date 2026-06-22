package com.example.saglikapp.utils

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WeatherService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val apiKey = "b85578076a656bf1e0953daf1128a378"

    interface WeatherCallback {
        fun onSuccess(currentWeather: String, tomorrowWeather: String)
        fun onError(error: String)
    }

    fun fetchWeather(city: String, callback: WeatherCallback) {
        Thread {
            try {
                // 1. ANLIK HAVA DURUMU
                val currentUrl = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$apiKey&units=metric&lang=tr"
                val currentRequest = Request.Builder().url(currentUrl).build()
                
                var currentInfo = ""
                client.newCall(currentRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "")
                        val temp = json.getJSONObject("main").getDouble("temp")
                        val desc = json.getJSONArray("weather").getJSONObject(0).getString("description")
                        currentInfo = "Bugün: %.1f°C, %s".format(temp, desc)
                    }
                }

                // 2. 5 GÜNLÜK TAHMİN (Yarını hesaplamak için)
                val forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=$apiKey&units=metric&lang=tr"
                val forecastRequest = Request.Builder().url(forecastUrl).build()
                
                var tomorrowInfo = "Yarın bilgisi alınamadı"
                client.newCall(forecastRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = JSONObject(response.body?.string() ?: "")
                        val list = json.getJSONArray("list")
                        
                        // Yarının tarihini belirle (Bugün + 1 gün)
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                        val tomorrowDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

                        var minTemp = Double.MAX_VALUE
                        var maxTemp = Double.MIN_VALUE
                        var dominantDesc = ""
                        var count = 0

                        for (i in 0 until list.length()) {
                            val item = list.getJSONObject(i)
                            val dtTxt = item.getString("dt_txt") // Format: "yyyy-MM-dd HH:mm:ss"
                            
                            if (dtTxt.startsWith(tomorrowDateStr)) {
                                val temp = item.getJSONObject("main").getDouble("temp")
                                if (temp < minTemp) minTemp = temp
                                if (temp > maxTemp) maxTemp = temp
                                
                                // Öğle saatindeki (12:00) açıklamayı genel durum olarak alalım
                                if (dtTxt.contains("12:00:00")) {
                                    dominantDesc = item.getJSONArray("weather").getJSONObject(0).getString("description")
                                }
                                count++
                            }
                        }

                        if (count > 0) {
                            if (dominantDesc.isEmpty()) {
                                dominantDesc = list.getJSONObject(8).getJSONArray("weather").getJSONObject(0).getString("description")
                            }
                            tomorrowInfo = "Yarın Genel: En Düşük %.1f°C, En Yüksek %.1f°C, %s".format(minTemp, maxTemp, dominantDesc)
                        }
                    }
                }

                if (currentInfo.isNotEmpty()) {
                    callback.onSuccess(currentInfo, tomorrowInfo)
                } else {
                    callback.onError("Hava durumu verisi çekilemedi")
                }

            } catch (e: Exception) {
                callback.onError("Hata: ${e.message}")
            }
        }.start()
    }
}
