package com.example.saglikapp.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.R;

import java.util.Locale;

public class BmiFragment extends Fragment {

    private BmiViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bmi, container, false);

        viewModel = new ViewModelProvider(this).get(BmiViewModel.class);

        if (getContext() == null) return view;
        SharedPreferences prefs = getContext().getSharedPreferences("UserData", Context.MODE_PRIVATE);

        String name     = prefs.getString("name", "Kullanıcı");
        String gender   = prefs.getString("gender", "Erkek");
        String heightS  = prefs.getString("height", "0");
        String weightS  = prefs.getString("weight", "0");
        String ageS     = prefs.getString("age", "25");

        double heightCm, weightKg;
        int age;
        try {
            heightCm = Double.parseDouble(heightS);
            weightKg = Double.parseDouble(weightS);
            age      = Integer.parseInt(ageS);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Kayıtlı kullanıcı bilgileri hatalı.", Toast.LENGTH_SHORT).show();
            return view;
        }

        if (heightCm <= 0 || weightKg <= 0) {
            Toast.makeText(getContext(), "Boy ve kilo bilgisi eksik.", Toast.LENGTH_SHORT).show();
            return view;
        }

        viewModel.calculateBmi(heightCm, weightKg, age, gender);

        TextView tvSummary    = view.findViewById(R.id.tvSummary);
        TextView tvBmi        = view.findViewById(R.id.tvBmi);
        TextView tvIdeal      = view.findViewById(R.id.tvIdeal);
        TextView tvCategory   = view.findViewById(R.id.tvCategory);
        TextView tvWeightDiff = view.findViewById(R.id.tvWeightDiff);
        TextView tvBmr        = view.findViewById(R.id.tvBmr);
        TextView tvAdvice     = view.findViewById(R.id.tvAdvice);
        SeekBar bmiSeekBar    = view.findViewById(R.id.bmiSeekBar);

        tvSummary.setText(String.format(Locale.getDefault(),
                "%s (%s, %d Yaş)\nBoy: %.0f cm   |   Kilo: %.1f kg",
                name, gender, age, heightCm, weightKg));

        tvBmi.setText(String.format(Locale.getDefault(), "VKİ: %.1f", viewModel.getBmi()));

        tvIdeal.setText(String.format(Locale.getDefault(),
                "İdeal kilo aralığı: %.1f – %.1f kg", viewModel.getIdealMin(), viewModel.getIdealMax()));

        tvCategory.setText("Durum: " + viewModel.getCategory());

        if (viewModel.getWeightDiff() > 0) {
            String action = viewModel.getBmi() < 18.5 ? "almanız" : "vermeniz";
            tvWeightDiff.setText(String.format(Locale.getDefault(),
                    "İdeal kilo için yaklaşık %.1f kg %s gerekiyor.", viewModel.getWeightDiff(), action));
        } else {
            tvWeightDiff.setText("Şu an ideal kilonuzdasınız.");
        }

        tvBmr.setText(String.format(Locale.getDefault(), "Günlük Bazal Metabolizma Hızı: %.0f kcal", viewModel.getBmr()));
        tvAdvice.setText("Öneri: " + viewModel.getAdvice());

        double currentBmi = viewModel.getBmi();
        int progress = (int) currentBmi;
        if (progress > 40) progress = 40;
        bmiSeekBar.setProgress(progress);

        return view;
    }
}