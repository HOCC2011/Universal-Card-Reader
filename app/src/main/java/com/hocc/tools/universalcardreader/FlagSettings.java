package com.hocc.tools.universalcardreader;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FlagSettings extends AppCompatActivity {

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_flag_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Switch TangoSwitch = findViewById(R.id.TangoSwitchView);
        Switch MPassSwitch = findViewById(R.id.MPassSwitchView);
        Switch EasyCardSwitch = findViewById(R.id.EasyCardSwitchView);
        // Load saved state
        SharedPreferences prefs = getSharedPreferences("Features", MODE_PRIVATE);
        boolean switchState = prefs.getBoolean("Tango", false); // default is off
        TangoSwitch.setChecked(switchState);
        boolean switchStateMPass = prefs.getBoolean("MPass", false); // default is off
        MPassSwitch.setChecked(switchStateMPass);
        boolean switchStateEasyCard = prefs.getBoolean("EasyCard", false); // default is off
        EasyCardSwitch.setChecked(switchStateEasyCard);

        // Save new state when switch is toggled
        TangoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("Tango", isChecked);
            editor.apply();
        });
        // Save new state when switch is toggled
        MPassSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("MPass", isChecked);
            editor.apply();
        });
        // Save new state when switch is toggled
        EasyCardSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("EasyCard", isChecked);
            editor.apply();
        });
    }
}