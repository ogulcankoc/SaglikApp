package com.example.saglikapp

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.Collections

class RemoteLlmService private constructor(private val context: android.content.Context) {

    // Sohbet geçmişini tutan liste
    private val messageHistory = Collections.synchronizedList(mutableListOf<JSONObject>())

    companion object {
        private const val DEFAULT_URL = "https://openrouter.ai/api/v1"
        private const val MODEL_NAME = "deepseek/deepseek-v4-flash"
        //openai/gpt-oss-120b
        //google/gemma-4-26b-a4b-it
        //google/gemma-4-31b-it
        @Volatile private var systemPrompt = ""

        @Volatile private var instance: RemoteLlmService? = null

        @JvmStatic
        fun getInstance(context: android.content.Context): RemoteLlmService {
            return instance ?: synchronized(this) {
                instance ?: RemoteLlmService(context.applicationContext).also { instance = it }
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

    private fun getBaseUrl(): String {
        val prefs = context.getSharedPreferences("UserData", android.content.Context.MODE_PRIVATE)
        return prefs.getString("remote_llm_url", DEFAULT_URL) ?: DEFAULT_URL
    }

    private fun getApiKey(): String {
        val prefs = context.getSharedPreferences("UserData", android.content.Context.MODE_PRIVATE)
        val defaultApiKey = com.example.saglikapp.BuildConfig.OPENROUTER_API_KEY
        return prefs.getString("openrouter_api_key", defaultApiKey) ?: defaultApiKey
    }

    // Sohbeti sıfırla
    fun clearChatHistory() {
        messageHistory.clear()
    }

    fun checkConnection(listener: LlmService.OnModelLoadedListener) {
        Thread {
            try {
                val request = Request.Builder()
                    .url("${getBaseUrl()}/models")
                    .addHeader("Authorization", "Bearer ${getApiKey()}")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) listener.onSuccess()
                    else listener.onError("Bağlantı hatası: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e("RemoteLlmService", "Bağlantı hatası: ${e.message}")
                listener.onError("Bağlantı kurulamadı: ${e.message}")
            }
        }.start()
    }

    fun generateResponse(userMessage: String, listener: LlmService.OnResponseListener) {
        Thread {
            try {
                val body = buildBody(userMessage, false)
                val request = Request.Builder()
                    .url("${getBaseUrl()}/chat/completions")
                    .addHeader("Authorization", "Bearer ${getApiKey()}")
                    .addHeader("HTTP-Referer", "https://github.com/ogulcan-saglikapp")
                    .addHeader("X-Title", "SaglikApp")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        listener.onError("API Hatası: ${response.code}")
                        return@use
                    }
                    val text = response.body?.string() ?: ""
                    val json = JSONObject(text)
                    val content = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    // Asistanın cevabını hafızaya ekle
                    messageHistory.add(JSONObject().apply {
                        put("role", "assistant")
                        put("content", content)
                    })

                    listener.onSuccess(content)
                }
            } catch (e: Exception) {
                listener.onError("Hata: ${e.message}")
            }
        }.start()
    }

    fun generateStreamingResponse(userMessage: String, listener: LlmService.OnStreamingResponseListener) {
        Thread {
            try {
                val body = buildBody(userMessage, true)
                val request = Request.Builder()
                    .url("${getBaseUrl()}/chat/completions")
                    .addHeader("Authorization", "Bearer ${getApiKey()}")
                    .addHeader("HTTP-Referer", "https://github.com/ogulcan-saglikapp")
                    .addHeader("X-Title", "SaglikApp")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        listener.onError("API Hatası: ${response.code}")
                        return@use
                    }
                    val source = response.body?.source() ?: throw Exception("Boş yanıt")
                    val fullResponse = StringBuilder()

                    while (!source.exhausted()) {
                        var line = source.readUtf8Line() ?: break
                        if (line.startsWith("data: ")) line = line.substring(6)
                        if (line.isBlank() || line == "[DONE]") continue

                        try {
                            val json = JSONObject(line)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val token = delta?.optString("content", "") ?: ""
                                
                                if (token.isNotEmpty()) {
                                    fullResponse.append(token)
                                    listener.onTokenReceived(token)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("RemoteLlmService", "JSON parse hatası: ${e.message} - Satır: $line")
                        }
                    }

                    if (fullResponse.isNotEmpty()) {
                        // Asistanın tam cevabını hafızaya ekle
                        messageHistory.add(JSONObject().apply {
                            put("role", "assistant")
                            put("content", fullResponse.toString())
                        })
                        listener.onComplete()
                    }
                }
            } catch (e: Exception) {
                Log.e("RemoteLlmService", "Stream hatası: ${e.message}")
                listener.onError("Stream hatası: ${e.message}")
            }
        }.start()
    }

    private fun buildBody(userMessage: String, stream: Boolean): String {
        val messagesArray = JSONArray()

        if (systemPrompt.isNotEmpty()) {
            messagesArray.put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
        }

        synchronized(messageHistory) {
            for (msg in messageHistory) {
                messagesArray.put(msg)
            }
        }

        val currentUserMsg = JSONObject().apply { put("role", "user"); put("content", userMessage) }
        messagesArray.put(currentUserMsg)
        messageHistory.add(currentUserMsg)

        // Limitleme (Son 10 mesaj)
        synchronized(messageHistory) {
            if (messageHistory.size > 10) {
                messageHistory.removeAt(0)
            }
        }

        return JSONObject().apply {
            put("model", MODEL_NAME)
            put("messages", messagesArray)
            put("stream", stream)
            put("max_tokens", 2000) //
        }.toString()
    }
}
