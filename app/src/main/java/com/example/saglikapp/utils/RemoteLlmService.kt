package com.example.saglikapp

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RemoteLlmService {

    companion object {
        private const val BASE_URL = "https://pete-commonly-excluding-tests.trycloudflare.com"
        private const val MODEL_NAME = "gemma-local"
        
        @Volatile private var systemPrompt =
                    "Sen bu uygulamanın akıllı sağlık asistanısın. " +
                    "ZORUNLU KURALLAR: " +
                    "1) Maksimum 2-3 kısa cümle ile cevap ver. " +
                    "2) Liste veya madde kullanma. " +
                    "3) Gereksiz giriş ve kapanış cümlesi ekleme. " +
                    "4) Sadece sorulan konuya odaklan. " +
                    "Türkçe cevap ver."

        @Volatile private var instance: RemoteLlmService? = null

        @JvmStatic
        fun getInstance(): RemoteLlmService {
            return instance ?: synchronized(this) {
                instance ?: RemoteLlmService().also { instance = it }
            }
        }

        @JvmStatic
        fun setSystemPrompt(prompt: String) {
            systemPrompt = prompt
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun checkConnection(listener: LlmService.OnModelLoadedListener) {
        Thread {
            try {
                val request = Request.Builder().url("$BASE_URL/api/tags").get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) listener.onSuccess()
                    else listener.onError("Sunucuya ulaşılamadı. Bilgisayarın açık mı?")
                }
            } catch (e: Exception) {
                Log.e("RemoteLlmService", "Bağlantı hatası: ${e.message}")
                listener.onError("Bağlantı kurulamadı: Cloudflare tunnel çalışıyor mu?")
            }
        }.start()
    }

    // Normal yanıt (HomeFragment için)
    fun generateResponse(userMessage: String, listener: LlmService.OnResponseListener) {
        Thread {
            try {
                val body = buildBody(userMessage, false)
                val request = Request.Builder()
                    .url("$BASE_URL/api/chat")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val text = response.body?.string() ?: ""
                    val content = JSONObject(text).getJSONObject("message").getString("content")
                    listener.onSuccess(content)
                }
            } catch (e: Exception) {
                listener.onError("Hata: ${e.message}")
            }
        }.start()
    }

    // Gerçek canlı streaming (ChatActivity için)
    fun generateStreamingResponse(userMessage: String, listener: LlmService.OnStreamingResponseListener) {
        Thread {
            try {
                val body = buildBody(userMessage, true)
                val request = Request.Builder()
                    .url("$BASE_URL/api/chat")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val source = response.body?.source()
                        ?: throw Exception("Boş yanıt")

                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.isBlank()) continue

                        val json = JSONObject(line)
                        val token = json.optJSONObject("message")
                            ?.optString("content", "") ?: ""
                        val done = json.optBoolean("done", false)

                        if (token.isNotEmpty()) listener.onTokenReceived(token)
                        if (done) {
                            listener.onComplete()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoteLlmService", "Stream hatası: ${e.message}")
                listener.onError("Stream hatası: ${e.message}")
            }
        }.start()
    }

    private fun buildBody(userMessage: String, stream: Boolean): String {
        return JSONObject().apply {
            put("model", MODEL_NAME)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
                put(JSONObject().apply { put("role", "user"); put("content", userMessage) })
            })
            put("stream", stream)
            put("think", false)
            put("options", JSONObject().apply {
                put("num_predict", 350)
            })
        }.toString()
    }
}