package com.example.saglikapp.ui;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.saglikapp.R;

import java.util.Locale;

public class ProfileFragment extends Fragment {

    private MainViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        EditText editName = view.findViewById(R.id.editProfileName);
        EditText editAge = view.findViewById(R.id.editProfileAge);
        EditText editWeight = view.findViewById(R.id.editProfileWeight);
        EditText editHeight = view.findViewById(R.id.editProfileHeight);
        EditText editWakeUpTime = view.findViewById(R.id.editWakeUpTime);
        EditText editBedTime = view.findViewById(R.id.editBedTime);
        RadioGroup radioGroupGender = view.findViewById(R.id.radioProfileGender);
        RadioButton radioMale = view.findViewById(R.id.radioProfileMale);
        RadioButton radioFemale = view.findViewById(R.id.radioProfileFemale);
        Button btnSave = view.findViewById(R.id.btnSaveProfile);

        // --- VERİLERİ BURADA YÜKLÜYORUZ ---
        editName.setText(viewModel.getName());
        editAge.setText(viewModel.getAge());
        editWeight.setText(viewModel.getWeight());
        editHeight.setText(viewModel.getHeight());
        editWakeUpTime.setText(viewModel.getWakeUpTime());
        editBedTime.setText(viewModel.getBedTime());

        if ("Erkek".equals(viewModel.getGender())) {
            radioMale.setChecked(true);
        } else if ("Kadın".equals(viewModel.getGender())) {
            radioFemale.setChecked(true);
        }
        // ----------------------------------

        editWakeUpTime.setOnClickListener(v -> showTimePicker(editWakeUpTime));
        editBedTime.setOnClickListener(v -> showTimePicker(editBedTime));

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString();
            String age = editAge.getText().toString();
            String weight = editWeight.getText().toString();
            String height = editHeight.getText().toString();
            String wakeUp = editWakeUpTime.getText().toString();
            String bedTime = editBedTime.getText().toString();

            int selectedGenderId = radioGroupGender.getCheckedRadioButtonId();
            String gender = (selectedGenderId == R.id.radioProfileMale) ? "Erkek" : "Kadın";

            if (viewModel.validateInputs(name, age, weight, height, gender, wakeUp, bedTime)) {
                viewModel.saveUserData(name, age, weight, height, gender, wakeUp, bedTime);
                Toast.makeText(getContext(), "Profil Güncellendi!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showTimePicker(EditText targetEditText) {
        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, selectedHour, selectedMinute) -> {
                    String formattedTime = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);
                    targetEditText.setText(formattedTime);
                }, 8, 0, true);
        timePickerDialog.show();
    }
}