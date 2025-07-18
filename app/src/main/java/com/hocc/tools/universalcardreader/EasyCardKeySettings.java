package com.hocc.tools.universalcardreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EasyCardKeySettings extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    ImageView back;
    TextView HelpText;
    TextView UID;
    TextView SaveKey;
    EditText Sector2KeyInput;
    String TagUID;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        setContentView(R.layout.activity_easy_card_key_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        HelpText = findViewById(R.id.HelpText);
        UID = findViewById(R.id.TagId);
        Sector2KeyInput = findViewById(R.id.Sector2KeyInput);
        SaveKey = findViewById(R.id.SaveKey);
        SaveKey.setOnClickListener(View -> {
            if (Sector2KeyInput.getText().toString().replace(" ", "").length() == 12) {
                SharedPreferences prefs = getSharedPreferences("Sector2Keys", MODE_PRIVATE);
                prefs.edit().putString(String.valueOf(TagUID), Sector2KeyInput.getText().toString().replace(" ", "")).apply();
                Toast.makeText(this, "Successfully saved the card's key.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Key length should be 6 bytes long (12 characters).", Toast.LENGTH_LONG).show();
            }
        });
        back = findViewById(R.id.back);
        back.setOnClickListener(View -> {
            Intent intent = new Intent(EasyCardKeySettings.this, EasyCardRead.class);
            startActivity(intent);
        });
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            Log.d("Error", "NFC not available or not enabled.");
        }
    }

    private void onCardTap(Tag tag) {
        HelpText.setVisibility(View.VISIBLE);
        HelpText.setText("Please tap card first.");
        UID.setVisibility(View.GONE);
        SaveKey.setVisibility(View.GONE);
        Sector2KeyInput.setVisibility(View.GONE);
        TagUID = bytesToHex(tag.getId()).replace(" ", "");
        if (TagUID.length() == 8){
            HelpText.setVisibility(View.GONE);
            UID.setVisibility(View.VISIBLE);
            UID.setText("UID: " + TagUID);
            SaveKey.setVisibility(View.VISIBLE);
            Sector2KeyInput.setVisibility(View.VISIBLE);
        } else {
            HelpText.setVisibility(View.VISIBLE);
            HelpText.setText("Error reading UID of the card, please try again.");
            UID.setVisibility(View.GONE);
            SaveKey.setVisibility(View.GONE);
            Sector2KeyInput.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Enable Reader Mode when activity is resumed
        if (nfcAdapter != null) {
            Bundle options = new Bundle();
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            nfcAdapter.enableReaderMode(
                    this,
                    new NfcAdapter.ReaderCallback() {
                        @Override
                        public void onTagDiscovered(Tag tag) {
                            // This callback will be triggered when a tag is detected
                            runOnUiThread(() -> onCardTap(tag));
                        }
                    },
                    NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    options
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);
        }
    }

    // Convert hex string to byte array
    public static byte[] hexStringToByteArray(String s) {
        if (s == null) throw new IllegalArgumentException("Hex string is null");
        int len = s.length();
        if (len % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int high = Character.digit(s.charAt(i), 16);
            int low = Character.digit(s.charAt(i + 1), 16);

            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Invalid hex character in: " + s);
            }

            data[i / 2] = (byte) ((high << 4) + low);
        }
        return data;
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}