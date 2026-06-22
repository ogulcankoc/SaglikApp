package com.example.saglikapp.ui;

import com.example.saglikapp.LlmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.ChatActivity;
import com.example.saglikapp.LlmService;
import com.example.saglikapp.R;
import com.example.saglikapp.utils.WeatherService;

import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class HomeFragment extends Fragment {

    private WelcomeViewModel welcomeViewModel;
    private WaterViewModel waterViewModel;
    private BmiViewModel bmiViewModel;
    private LlmManager llmManager;

    private TextView textWaterSummary;
    private TextView textHeartSummary;
    private TextView textAiAnalysis;
    private TextView textDailyQuote;
    private TextView textQuoteSource;
    private ProgressBar pbAnalysis;

    private final String[] backupQuotes = {
            "Bugün harika bir gün olacak, adım atmaya devam et!",
            "Küçük adımlar, büyük sonuçlar doğurur.",
            "Sağlığın en büyük servetindir, ona iyi bak.",
            "Vücudun senin tek evin, onu sev ve besle.",
            "Başarı, her gün tekrarlanan küçük disiplinlerin toplamıdır."
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        welcomeViewModel = new ViewModelProvider(this).get(WelcomeViewModel.class);
        bmiViewModel = new ViewModelProvider(this).get(BmiViewModel.class);
        
        if (getActivity() != null) {
            waterViewModel = new ViewModelProvider(getActivity()).get(WaterViewModel.class);
        } else {
            waterViewModel = new ViewModelProvider(this).get(WaterViewModel.class);
        }

        llmManager = LlmManager.getInstance(getContext());

        TextView textWelcome = view.findViewById(R.id.textWelcome);
        textWaterSummary = view.findViewById(R.id.textWaterSummary);
        textHeartSummary = view.findViewById(R.id.textHeartSummary);
        textAiAnalysis = view.findViewById(R.id.textAiAnalysis);
        textDailyQuote = view.findViewById(R.id.textDailyQuote);
        textQuoteSource = view.findViewById(R.id.textQuoteSource);
        pbAnalysis = view.findViewById(R.id.pbAnalysis);
        
        ImageButton btnSettings = view.findViewById(R.id.btnSettings);

        textWelcome.setText(welcomeViewModel.getWelcomeMessage());

        waterViewModel.getCurrentWaterLiveData().observe(getViewLifecycleOwner(), current -> {
            updateWaterText(current, waterViewModel.getDailyGoal());
        });

        waterViewModel.getDailyGoalLiveData().observe(getViewLifecycleOwner(), goal -> {
            updateWaterText(waterViewModel.getCurrentWater(), goal);
        });

        updateHeartSummary();
        showInitialQuote();

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), SettingsActivity.class));
        });

        view.findViewById(R.id.cardChat).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ChatActivity.class));
        });

        // Mood Butonları
        view.findViewById(R.id.btnMoodBad).setOnClickListener(v -> openChatWithMood("😞"));
        view.findViewById(R.id.btnMoodNeutral).setOnClickListener(v -> openChatWithMood("😐"));
        view.findViewById(R.id.btnMoodGood).setOnClickListener(v -> openChatWithMood("😊"));

        startSequentialAiTasks();

        return view;
    }

    private void openChatWithMood(String emoji) {
        String moodText = "";
        String userMessage = "";
        
        switch (emoji) {
            case "😞":
                moodText = "kötü";
                userMessage = "Kendimi biraz kötü hissediyorum, verilerime bakıp bir değerlendirme yapar mısın? Bana ne önerirsin?";
                break;
            case "😐":
                moodText = "nötr";
                userMessage = "Bugün biraz durgunum. Sağlık verilerime göre enerjimi toplamak için ne yapabilirim?";
                break;
            case "😊":
                moodText = "iyi";
                userMessage = "Bugün kendimi harika hissediyorum! Bu enerjimi korumak için bugün nelere odaklanmalıyım?";
                break;
        }

        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("mood_emoji", emoji);
        intent.putExtra("mood_text", moodText);
        intent.putExtra("user_message", userMessage);
        startActivity(intent);
    }

    private void showInitialQuote() {
        if (getContext() == null || textDailyQuote == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("AiData", Context.MODE_PRIVATE);
        String savedQuote = prefs.getString("daily_quote", "");
        
        if (!savedQuote.isEmpty()) {
            textDailyQuote.setText(savedQuote);
            if (textQuoteSource != null) textQuoteSource.setVisibility(View.VISIBLE);
        } else {
            textDailyQuote.setText(backupQuotes[new Random().nextInt(backupQuotes.length)]);
            if (textQuoteSource != null) textQuoteSource.setVisibility(View.GONE);
        }
    }

    private void startSequentialAiTasks() {
        if (getContext() == null || textAiAnalysis == null) return;
        
        pbAnalysis.setVisibility(View.VISIBLE);
        textAiAnalysis.setText("Veriler hazırlanıyor...");

        if (!isNetworkAvailable()) {
            // İnternet yoksa direkt analize geç
            loadLlmAndPerformAnalysis("Hava durumu bilgisi şu an yok (çevrimdışı)");
            return;
        }

        SharedPreferences prefs = getContext().getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String city = prefs.getString("city", "Istanbul");
        
        WeatherService weatherService = new WeatherService(getContext());
        weatherService.fetchWeather(city, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(String currentWeather, String tomorrowWeather) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        String combinedWeather = currentWeather + " | " + tomorrowWeather;
                        loadLlmAndPerformAnalysis(combinedWeather);
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> loadLlmAndPerformAnalysis("Hava durumu bilgisi alınamadı"));
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        if (getContext() == null) return false;
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) 
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            android.net.NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }

    private void loadLlmAndPerformAnalysis(String weatherInfo) {
        if (getContext() == null) return;
        textAiAnalysis.setText("Analiz yapılıyor...");
        
        llmManager.initialize(new LlmService.OnModelLoadedListener() {
            @Override
            public void onSuccess() {
                performAnalysisThenQuote(weatherInfo);
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        pbAnalysis.setVisibility(View.GONE);
                        textAiAnalysis.setText("Yapay zeka asistanı şu an meşgul.");
                    });
                }
            }
        });
    }

    private void performAnalysisThenQuote(String weatherInfo) {
        if (getContext() == null) return;
        
        SharedPreferences prefs = getContext().getSharedPreferences("UserData", Context.MODE_PRIVATE);
        
        String name = prefs.getString("name", "Kullanıcı");
        String weight = prefs.getString("weight", "70");
        String height = prefs.getString("height", "170");
        String gender = prefs.getString("gender", "Erkek");
        String ageStr = prefs.getString("age", "25");
        String bedTime = prefs.getString("bedTime", "23:00");
        
        int water = waterViewModel.getCurrentWater();
        int waterGoal = waterViewModel.getDailyGoal();
        int waterLeft = Math.max(0, waterGoal - water);
        int lastBpm = prefs.getInt("lastBPM", 0);

        try {
            double h = Double.parseDouble(height);
            double w = Double.parseDouble(weight);
            int age = Integer.parseInt(ageStr);
            bmiViewModel.calculateBmi(h, w, age, gender);
        } catch (Exception e) {}
        
        double bmi = bmiViewModel.getBmi();
        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        String prompt = String.format(Locale.getDefault(),
                "Kullanıcı Bilgileri:\n" +
                "- İsim: %s\n" +
                "- Şu anki saat: %s\n" +
                "- Hava Durumu: %s\n" +
                "- Mevcut Su: %d ml / Hedef: %d ml (Kalan: %d ml)\n" +
                "- Son Kalp Ritmi: %d BPM\n" +
                "- VKİ (BMI): %.1f\n" +
                "- Planlanan Yatış Saati: %s\n\n" +
                "Görev: Kullanıcıya ismen hitap et. Motive edici ol. Hava durumunu, su miktarını ve sağlığını harmanla. " +
                "Güncel hava durumununa göre tavsiye ver daha sonra yarınki minimum/maksimum sıcaklıkları dikkate alarak yarınki hava durumuna göre kıyafet ve su tüketimi tavsiyesi ver. " +
                "Sayısal veriler içeren, kısa, öz ve net bir sağlık tavsiyesi ver ve kalp ritmini de yorumla. Yorumlarını çok uzatma mümkün oldukça kısa cümleler kullan. ",
                name, currentTime, weatherInfo, water, waterGoal, waterLeft, lastBpm, bmi, bedTime);

        llmManager.generateResponse(prompt, new LlmService.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        pbAnalysis.setVisibility(View.GONE);
                        String cleanResponse = response.replace("<|turn|>model\n", "").replace("<turn|>", "").trim();
                        textAiAnalysis.setText(cleanResponse);
                        refreshDailyQuoteIfNecessary();
                    });
                }
            }

            @Override
            public void onError(String error) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        pbAnalysis.setVisibility(View.GONE);
                        textAiAnalysis.setText("Analiz şu an yapılamıyor.");
                        refreshDailyQuoteIfNecessary();
                    });
                }
            }
        });
    }

    private void refreshDailyQuoteIfNecessary() {
        if (getContext() == null) return;
        SharedPreferences aiPrefs = getContext().getSharedPreferences("AiData", Context.MODE_PRIVATE);
        
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastDate = aiPrefs.getString("quote_date", "");

        if (today.equals(lastDate)) return;

        String quotePrompt = "Kullanıcıya çok kısa (maksimum 30 kelime), " +
                "ilham verici bir yaşam mottosu söyle. Sadece sözü yaz. Varsa sözü söyleyen kişinin ismini de ekle";

        llmManager.generateResponse(quotePrompt, new LlmService.OnResponseListener() {
            @Override
            public void onSuccess(String response) {
                String cleanQuote = response.replace("<|turn|>model\n", "").replace("<turn|>", "").trim();
                aiPrefs.edit()
                        .putString("daily_quote", cleanQuote)
                        .putString("quote_date", today)
                        .apply();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (textDailyQuote != null) textDailyQuote.setText(cleanQuote);
                        if (textQuoteSource != null) textQuoteSource.setVisibility(View.VISIBLE);
                    });
                }
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void updateWaterText(int current, int goal) {
        if (textWaterSummary != null) {
            textWaterSummary.setText(current + " / " + goal + " ml");
        }
    }

    private void updateHeartSummary() {
        if (getContext() == null || textHeartSummary == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("UserData", Context.MODE_PRIVATE);
        int lastBpm = prefs.getInt("lastBPM", 0);
        if (lastBpm > 0) {
            textHeartSummary.setText("Son ölçüm: " + lastBpm + " BPM");
        } else {
            textHeartSummary.setText("Henüz ölçüm yapılmadı");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateHeartSummary();
    }
}