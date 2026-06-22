package com.example.saglikapp;

import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<MessageModel> messageList;
    private EditText messageEditText;
    private ImageButton sendButton;
    private LlmManager llmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_chat);

        hideSystemUI();

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(android.R.id.content),
                (v, insets) -> {
                    int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                    int navHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    v.setPadding(0, 0, 0, Math.max(imeHeight, navHeight));
                    return insets;
                }
        );

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList,this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        chatRecyclerView.addOnLayoutChangeListener((v, left, top, right, bottom,
                                                    oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                chatRecyclerView.postDelayed(() -> {
                    if (!messageList.isEmpty()) {
                        chatRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                }, 100);
            }
        });

        llmManager = LlmManager.getInstance(this);
        sendButton.setEnabled(false);

        Toast.makeText(this, "Yapay zeka motoru yükleniyor...", Toast.LENGTH_LONG).show();

        llmManager.initialize(new LlmService.OnModelLoadedListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;

                    sendButton.setEnabled(true);
                    String mod = llmManager.getCurrentMode() == LlmManager.Mode.LOCAL
                            ? "Yerel model aktif 📱" : "Bulut model aktif ☁️";
                    Toast.makeText(ChatActivity.this, mod, Toast.LENGTH_SHORT).show();

                    // Mood verisi gelmiş mi kontrol et
                    String moodEmoji = getIntent().getStringExtra("mood_emoji");
                    String moodText = getIntent().getStringExtra("mood_text");
                    String prefilledMessage = getIntent().getStringExtra("user_message");

                    if (moodEmoji != null && prefilledMessage != null) {
                        // Kullanıcı sanki yazmış gibi mesajı gönder
                        messageList.add(new MessageModel(prefilledMessage, true));
                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                        chatRecyclerView.scrollToPosition(messageList.size() - 1);
                        
                        // AI yanıtını tetikle
                        processMoodMessage(moodEmoji, moodText, prefilledMessage);
                    } else {
                        messageList.add(new MessageModel(
                                "Merhaba! Ben senin kişisel sağlık asistanınım. Bugün sana nasıl yardımcı olabilirim?",
                                false
                        ));
                        chatAdapter.notifyItemInserted(messageList.size() - 1);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        Toast.makeText(ChatActivity.this, "Hata: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        sendButton.setOnClickListener(v -> sendMessage());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    private void sendMessage() {
        String userMessage = messageEditText.getText().toString().trim();
        if (userMessage.isEmpty()) return;

        messageList.add(new MessageModel(userMessage, true));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
        messageEditText.setText("");

        generateAiResponse(userMessage);
    }

    private void processMoodMessage(String emoji, String moodText, String prefilledMessage) {
        // Mood için özelleştirilmiş bir AI isteği atalım
        String promptWithMood = "Kullanıcı şu an kendini [" + moodText + " " + emoji + "] hissediyor. " +
                "Kullanıcının bu mesajına yanıt ver: \"" + prefilledMessage + "\". " +
                "Yanıtında kullanıcının sağlık verilerini (su, nabız vb.) de göz önünde bulundur ve mutlaka 'Hızlı bir aksiyon' önerisi (nefes egzersizi, su artışı, kısa yürüyüş vb.) yap.";
        
        generateAiResponse(promptWithMood);
    }

    private void generateAiResponse(String messageToSend) {
        MessageModel botMessage = new MessageModel("⏳ Düşünüyor...", false);
        messageList.add(botMessage);
        int botMessagePosition = messageList.size() - 1;
        chatAdapter.notifyItemInserted(botMessagePosition);

        sendButton.setEnabled(false);
        sendButton.setAlpha(0.5f);

        final boolean[] firstToken = {true};

        llmManager.generateStreamingResponse(messageToSend, new LlmService.OnStreamingResponseListener() {
            @Override
            public void onTokenReceived(String token) {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;

                    if (firstToken[0]) {
                        botMessage.setText(token);
                        firstToken[0] = false;
                    } else {
                        botMessage.setText(botMessage.getText() + token);
                    }
                    chatAdapter.notifyItemChanged(botMessagePosition);
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);
                });
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    sendButton.setEnabled(true);
                    sendButton.setAlpha(1.0f);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    botMessage.setText("⚠️ " + error);
                    chatAdapter.notifyItemChanged(botMessagePosition);
                    sendButton.setEnabled(true);
                    sendButton.setAlpha(1.0f);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ekran kapatıldığında arka plandaki modelin hafızasını sıfırla
        if (llmManager != null) {
            llmManager.clearHistory();
        }
    }
}