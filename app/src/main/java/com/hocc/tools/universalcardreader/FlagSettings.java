package com.hocc.tools.universalcardreader;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Switch;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class FlagSettings extends AppCompatActivity {

    private static final boolean ReadTango = false;

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
        // Load saved state
        SharedPreferences prefs = getSharedPreferences("Features", MODE_PRIVATE);
        boolean switchState = prefs.getBoolean("Tango", false); // default is off
        TangoSwitch.setChecked(switchState);

        // Save new state when switch is toggled
        TangoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("Tango", isChecked);
            editor.apply();
        });
    }
}