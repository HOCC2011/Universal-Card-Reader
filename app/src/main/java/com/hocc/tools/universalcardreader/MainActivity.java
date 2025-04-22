package com.hocc.tools.universalcardreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
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
    LinearLayout Octopus;
    LinearLayout Tunion;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
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
    }
}
