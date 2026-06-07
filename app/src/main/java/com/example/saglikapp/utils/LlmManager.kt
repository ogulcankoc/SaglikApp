package com.example.saglikapp

import android.content.Context
import android.util.Log

class LlmManager private constructor(private val context: Context) {


    private fun buildSystemPrompt(): String {
        val prefs = context.getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val name = prefs.getString("name", "Kullanıcı")
        val weight = prefs.getString("weight", "70")
        val height = prefs.getString("height", "170")
        val gender = prefs.getString("gender", "Erkek")
        val ageStr = prefs.getString("age", "25")

        return """
            Sen bu uygulamanın resmi akıllı sağlık asistanısın.
            Görevin kullanıcının bu verilerine göre kişiselleştirilmiş, bilimsel ve motive edici sağlık tavsiyeleri vermektir. 
            Kullanıcıyla doğal bir sohbetteymiş gibi konuş. Türkçe cevap ver.
        """.trimIndent()
    }
    companion object {
        private const val LOCAL_MODEL_FILENAME = "gemma-4-E2B-it.litertlm"

        @Volatile
        private var instance: LlmManager? = null

        @JvmStatic
        fun getInstance(context: Context): LlmManager {
            return instance ?: synchronized(this) {
                instance ?: LlmManager(context).also { instance = it }
            }
        }
    }

    // Hangi mod aktif?
    enum class Mode { LOCAL, REMOTE }

    val currentMode: Mode
        get() = if (isLocalModelAvailable()) Mode.LOCAL else Mode.REMOTE

    fun isLocalModelAvailable(): Boolean {
        val file = java.io.File(context.filesDir, LOCAL_MODEL_FILENAME)
        return file.exists() && file.length() > 0
    }

    // Başlatma — hangisi aktifse onu hazırlar
    fun initialize(listener: LlmService.OnModelLoadedListener) {
        val prompt = buildSystemPrompt()
        if (currentMode == Mode.LOCAL) {
            LlmService.getInstance(context).initializeModel(prompt, listener)
        } else {
            RemoteLlmService.setSystemPrompt(prompt)
            RemoteLlmService.getInstance().checkConnection(listener)
        }
    }

    // Normal yanıt
    fun generateResponse(
        userMessage: String,
        listener: LlmService.OnResponseListener
    ) {
        if (currentMode == Mode.LOCAL) {
            LlmService.getInstance(context).generateResponse(userMessage, listener)
        } else {
            RemoteLlmService.getInstance().generateResponse(userMessage, listener)
        }
    }

    // Streaming yanıt
    fun generateStreamingResponse(
        userMessage: String,
        listener: LlmService.OnStreamingResponseListener
    ) {
        if (currentMode == Mode.LOCAL) {
            LlmService.getInstance(context).generateStreamingResponse(userMessage, listener)
        } else {
            RemoteLlmService.getInstance().generateStreamingResponse(userMessage, listener)
        }
    }

    fun close() {
        LlmService.getInstance(context).close()
    }
}