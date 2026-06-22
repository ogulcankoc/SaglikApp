package com.example.saglikapp

import android.content.Context
import android.util.Log
import com.example.saglikapp.data.AppDatabase
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class LlmManager private constructor(private val context: Context) {

    private var cachedPrompt: String? = null

    private fun buildSystemPrompt(callback: (String) -> Unit) {
        val prefs = context.getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val name = prefs.getString("name", "Kullanıcı")
        val weightStr = prefs.getString("weight", "70")
        val heightStr = prefs.getString("height", "170")
        val gender = prefs.getString("gender", "Erkek")
        val ageStr = prefs.getString("age", "25")

        // VKİ Hesaplama
        var bmi = 0.0
        try {
            val w = weightStr?.toDouble() ?: 70.0
            val h = (heightStr?.toDouble() ?: 170.0) / 100.0
            bmi = w / (h * h)
        } catch (e: Exception) {}

        Executors.newSingleThreadExecutor().execute {
            val db = AppDatabase.getInstance(context)
            
            // Son 3 günlük su verisi
            val waterLogs = db.waterDao().lastFourteenDays.take(3)
            val waterInfo = waterLogs.joinToString(", ") { "${it.date}: ${it.totalAmount}ml" }

            // Son 5 kalp ölçümü
            val heartLogs = db.heartRateDao().lastTenReadings.take(5)
            val heartInfo = heartLogs.joinToString(", ") { "${it.bpm} BPM" }

            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val currentTime = sdf.format(Date())

            val prompt = """
                Sen akıllı bir sağlık asistanısın.
                Kullanıcı Bilgileri: İsim: $name, Yaş: $ageStr, Boy: $heightStr cm, Kilo: $weightStr kg, Cinsiyet: $gender, VKİ: ${"%.1f".format(bmi)}.
                Güncel Zaman: $currentTime
                
                Sağlık Geçmişi:
                - Son 3 Günlük Su Tüketimi: $waterInfo
                - Son 5 Kalp Ritmi Ölçümü: $heartInfo
                
                Görevin kullanıcının verilerine göre kısa, öz ve net sağlık tavsiyeleri vermektir. Motive edici ol.
                
                ÖNEMLİ KURALLAR:
                - Yanıtlarını KISA tut.
                - Teknik terimlerden kaçın, halk dilinde ama bilimsel konuş.
                - Doğal bir sohbetteymiş gibi Türkçe konuş. 
            """.trimIndent()
            
            callback(prompt)
        }
    }

    companion object {
        private const val LOCAL_MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

        @Volatile
        private var instance: LlmManager? = null

        @JvmStatic
        fun getInstance(context: Context): LlmManager {
            return instance ?: synchronized(this) {
                instance ?: LlmManager(context.applicationContext).also { instance = it }
            }
        }
    }

    enum class Mode { LOCAL, REMOTE }

    private var currentMode: Mode = Mode.REMOTE
    private var isInitialized = false
    private var isInitializing = false

    fun getCurrentMode(): Mode = currentMode

    fun isLocalModelAvailable(): Boolean {
        val file = java.io.File(context.filesDir, LOCAL_MODEL_FILENAME)
        return file.exists() && file.length() > 0
    }

    // Kullanıcı verileri değiştiğinde çağrılacak
    fun updateSystemPrompt() {
        buildSystemPrompt { newPrompt ->
            if (currentMode == Mode.REMOTE) {
                RemoteLlmService.setSystemPrompt(newPrompt)
                RemoteLlmService.getInstance(context).clearChatHistory()
            } else if (currentMode == Mode.LOCAL && isInitialized) {
                LlmService.getInstance(context).updatePrompt(newPrompt)
            }
        }
    }

    // Sohbet ekranından çıkıldığında hafızayı temizler
    fun clearHistory() {
        if (currentMode == Mode.REMOTE) {
            RemoteLlmService.getInstance(context).clearChatHistory()
        } else if (currentMode == Mode.LOCAL && isInitialized) {
            LlmService.getInstance(context).clearHistory()
        }
    }

    fun initialize(listener: LlmService.OnModelLoadedListener) {
        if (isInitialized) {
            listener.onSuccess()
            return
        }
        if (isInitializing) {
            Log.w("LlmManager", "Model başlatma işlemi devam ediyor.")
            return
        }

        isInitializing = true
        
        buildSystemPrompt { prompt ->
            RemoteLlmService.setSystemPrompt(prompt)

            RemoteLlmService.getInstance(context).checkConnection(object : LlmService.OnModelLoadedListener {
                override fun onSuccess() {
                    currentMode = Mode.REMOTE
                    isInitialized = true
                    isInitializing = false
                    listener.onSuccess()
                }

                override fun onError(error: String) {
                    Log.e("LlmManager", "Remote bağlantı kurulamadı: ${error}. Yerel modele geçiliyor...")
                    if (isLocalModelAvailable()) {
                        currentMode = Mode.LOCAL
                        LlmService.getInstance(context).initializeModel(prompt, object : LlmService.OnModelLoadedListener {
                            override fun onSuccess() {
                                isInitialized = true
                                isInitializing = false
                                listener.onSuccess()
                            }
                            override fun onError(err: String) {
                                isInitializing = false
                                listener.onError("Yerel model yüklenemedi: ${err}")
                            }
                        })
                    } else {
                        isInitializing = false
                        listener.onError("İnternet bağlantısı yok ve yerel model bulunamadı.")
                    }
                }
            })
        }
    }

    fun generateResponse(userMessage: String, listener: LlmService.OnResponseListener) {
        if (!isInitialized) {
            listener.onError("Model henüz hazır değil.")
            return
        }
        if (currentMode == Mode.LOCAL) {
            LlmService.getInstance(context).generateResponse(userMessage, listener)
        } else {
            RemoteLlmService.getInstance(context).generateResponse(userMessage, listener)
        }
    }

    fun generateStreamingResponse(userMessage: String, listener: LlmService.OnStreamingResponseListener) {
        if (!isInitialized) {
            listener.onError("Model henüz hazır değil.")
            return
        }
        if (currentMode == Mode.LOCAL) {
            LlmService.getInstance(context).generateStreamingResponse(userMessage, listener)
        } else {
            RemoteLlmService.getInstance(context).generateStreamingResponse(userMessage, listener)
        }
    }

    fun close() {
        if (currentMode == Mode.LOCAL) {
            LlmService.getInstance(context).close()
        }
    }
}
