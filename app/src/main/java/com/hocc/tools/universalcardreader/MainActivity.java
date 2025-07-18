package com.hocc.tools.universalcardreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView Octopus;
    TextView Tunion;
    TextView MPass;
    TextView EasyCard;
    TextView Tango;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = getSharedPreferences("Features", MODE_PRIVATE);
        Octopus = findViewById(R.id.Octopus);
        Octopus.setOnClickListener(View -> {
            Intent intent = new Intent(MainActivity.this, OctopusRead.class);
            startActivity(intent);
        });
        Tunion = findViewById(R.id.Tunion);
        Tunion.setOnClickListener(View -> {
            Intent intent = new Intent(MainActivity.this, TunionRead.class);
            startActivity(intent);
        });
        MPass = findViewById(R.id.MPass);
        MPass.setOnClickListener(View -> {
            Intent intent = new Intent(MainActivity.this, MPassRead.class);
            startActivity(intent);
        });
        if (prefs.getBoolean("MPass", false) == true) {
            MPass.setVisibility(View.VISIBLE);
        } else {
            MPass.setVisibility(View.GONE);
        }
        EasyCard = findViewById(R.id.EasyCard);
        EasyCard.setOnClickListener(View -> {
            Intent intent = new Intent(MainActivity.this, EasyCardRead.class);
            startActivity(intent);
        });
        if (prefs.getBoolean("EasyCard", false) == true) {
            EasyCard.setVisibility(View.VISIBLE);
        } else {
            EasyCard.setVisibility(View.GONE);
        }
        Tango = findViewById(R.id.Tango);
        Tango.setOnClickListener(View -> {
            Intent intent = new Intent(MainActivity.this, TangoRead.class);
            startActivity(intent);
        });
        if (prefs.getBoolean("Tango", false) == true) {
            Tango.setVisibility(View.VISIBLE);
        } else {
            Tango.setVisibility(View.GONE);
        }
    }
}
