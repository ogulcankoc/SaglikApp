package com.example.saglikapp

import android.content.Context
import com.google.ai.edge.litertlm.*
import java.io.File

class LlmService private constructor(context: Context) {

    companion object {
        init {
            try {
                System.loadLibrary("litertlm_jni")
                android.util.Log.d("LlmService", "Native kütüphane yüklendi")
            } catch (e: UnsatisfiedLinkError) {
                android.util.Log.e("LlmService", "Native kütüphane yüklenemedi: ${e.message}")
            }
        }

        @Volatile
        private var instance: LlmService? = null

        @JvmStatic  // BU SATIRI EKLE
        fun getInstance(context: Context): LlmService {
            return instance ?: synchronized(this) {
                instance ?: LlmService(context).also { instance = it }
            }
        }
    }

    private val appContext = context.applicationContext
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var isModelLoaded = false
    private var isInitializing = false
    private val pendingListeners = mutableListOf<OnModelLoadedListener>()

    private val modelFilename = "gemma-4-E2B-it.litertlm"
    private var systemInstruction = "Sen bu uygulamanın resmi akıllı sağlık asistanısın. " +
            "Görevin kullanıcının verilerine göre kısa tavsiyeler vermektir. Türkçe cevap ver."

    fun initializeModel(customInstruction: String, listener: OnModelLoadedListener) {
        this.systemInstruction = customInstruction
        if (isModelLoaded) {
            // Eğer model zaten yüklüyse, yeni talimatla yeni bir konuşma başlatmamız gerekebilir
            // Ancak şu anki yapıda Conversation singleton gibi davranıyor.
            // Basitlik adına, her initialize çağrısında conversation'ı yenileyebiliriz.
            try {
                conversation?.close()
                val convConfig = ConversationConfig(
                    systemInstruction = Contents.of(systemInstruction)
                )
                conversation = engine?.createConversation(convConfig)
                listener.onSuccess()
            } catch (e: Exception) {
                listener.onError("Talimat güncelleme hatası: ${e.message}")
            }
            return
        }
        synchronized(pendingListeners) {
            pendingListeners.add(listener)
            if (isInitializing) return
            isInitializing = true
        }

        Thread {
            try {
                val modelFile = File(appContext.filesDir, modelFilename)
                android.util.Log.d("LlmService", "Model yolu: ${modelFile.absolutePath}")
                android.util.Log.d("LlmService", "Model mevcut: ${modelFile.exists()}")
                android.util.Log.d("LlmService", "Model boyutu: ${modelFile.length()}")

                if (!modelFile.exists()) {
                    notifyError("Model dosyası bulunamadı. Lütfen Ayarlar'dan indirin.")
                    return@Thread
                }

                val cacheDir = File(appContext.cacheDir, "llm_cache").also { it.mkdirs() }

                val engineConfig = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = cacheDir.absolutePath
                )

                android.util.Log.d("LlmService", "Engine başlatılıyor...")
                engine = Engine(engineConfig).also { it.initialize() }
                android.util.Log.d("LlmService", "Engine hazır")

                val convConfig = ConversationConfig(
                    systemInstruction = Contents.of(systemInstruction)
                )

                conversation = engine!!.createConversation(convConfig)
                isModelLoaded = true
                notifySuccess()

            } catch (e: Exception) {
                android.util.Log.e("LlmService", "Hata: ${e.message}", e)
                notifyError("Yükleme hatası: ${e.message}")
            }
        }.start()
    }

    fun generateStreamingResponse(userMessage: String, listener: OnStreamingResponseListener) {
        if (!isModelLoaded || conversation == null) {
            listener.onError("Yapay zeka hazır değil.")
            return
        }
        try {
            val message = Message.Companion.user(Contents.of(userMessage))

            conversation!!.sendMessageAsync(
                message = message,
                callback = object : MessageCallback {
                    override fun onMessage(message: Message) {
                        listener.onTokenReceived(message.toString())
                    }
                    override fun onDone() { listener.onComplete() }
                    override fun onError(t: Throwable) { listener.onError(t.message ?: "Bilinmeyen hata") }
                }
            )
        } catch (e: Exception) {
            listener.onError(e.message ?: "Bilinmeyen hata")
        }
    }

    fun generateResponse(userMessage: String, listener: OnResponseListener) {
        if (!isModelLoaded || conversation == null) {
            listener.onError("Yapay zeka hazır değil.")
            return
        }
        Thread {
            try {
                val message = Message.Companion.user(Contents.of(userMessage))
                val response = conversation!!.sendMessage(message)
                listener.onSuccess(response.toString())
            } catch (e: Exception) {
                listener.onError(e.message ?: "Bilinmeyen hata")
            }
        }.start()
    }

    fun close() {
        conversation?.close()
        engine?.close()
        conversation = null
        engine = null
        isModelLoaded = false
        instance = null
    }

    private fun notifySuccess() {
        val listeners = synchronized(pendingListeners) {
            pendingListeners.toList().also {
                pendingListeners.clear()
                isInitializing = false
            }
        }
        listeners.forEach { it.onSuccess() }
    }

    private fun notifyError(error: String) {
        val listeners = synchronized(pendingListeners) {
            pendingListeners.toList().also {
                pendingListeners.clear()
                isInitializing = false
            }
        }
        listeners.forEach { it.onError(error) }
    }

    interface OnModelLoadedListener { fun onSuccess(); fun onError(error: String) }
    interface OnResponseListener { fun onSuccess(response: String); fun onError(error: String) }
    interface OnStreamingResponseListener { fun onTokenReceived(token: String); fun onComplete(); fun onError(error: String) }
}