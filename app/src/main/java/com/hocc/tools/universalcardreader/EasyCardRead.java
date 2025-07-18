package com.hocc.tools.universalcardreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.Arrays;

public class EasyCardRead extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private TextView detailed_info;
    private TextView balance;
    private ImageView back;
    StringBuilder detailed_info_string;
    String balance_string;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_easy_card_read);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        detailed_info = findViewById(R.id.DetailedInfo);
        back = findViewById(R.id.back);
        back.setOnClickListener(View -> {
            Intent intent = new Intent(EasyCardRead.this, MainActivity.class);
            startActivity(intent);
        });
        balance = findViewById(R.id.balance);
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            detailed_info.setText("NFC not available or not enabled.");
        }
    }

    private void onCardTap(Tag tag) {
        Log.d("Debug", "Card Tapped!");
        Toast.makeText(this, "Reading Card...", Toast.LENGTH_LONG).show();
        detailed_info_string = new StringBuilder("");
        balance.setText("-");
        IsoDep isoDep = IsoDep.get(tag);

        if (isoDep == null) {
            detailed_info.setText("Card isn't new card with IsoDep.");
        } else {
            ReadIsoDepCard(isoDep);
        }
    }

    public void ReadIsoDepCard (IsoDep isoDep){
        try {
            isoDep.connect();

            // Select EasyCard Read AID A0 00 00 03 22 10 07 01
            byte[] selectAID = new byte[]{
                    (byte) 0x00, (byte) 0xA4, 0x04, 0x00, 0x08, (byte) 0xA0, (byte) 0x00, (byte)0x00, (byte)0x03, (byte)0x22, (byte)0x10, (byte)0x07, (byte)0x01
            };

            byte[] EasyCardResponse = isoDep.transceive(selectAID);
            Log.d("Debug", bytesToHex(EasyCardResponse));
            detailed_info_string.append("EasyCard Application Raw Data:\n").append(bytesToHex(EasyCardResponse));
            detailed_info.setText(detailed_info_string);

            if (bytesToHex(EasyCardResponse).equals("90 00")) {
                byte[] ReadAllDataCommand = new byte[]{
                        (byte) 0x80, (byte) 0x30, (byte) 0x00, (byte) 0x00,
                        (byte) 0x08, (byte) 0x01, (byte) 0x02, (byte) 0x01,
                        (byte) 0x03, (byte) 0x02, (byte) 0x02, (byte) 0x02,
                        (byte) 0x01
                };
                byte[] ReadAllDataResponse = isoDep.transceive(ReadAllDataCommand);
                detailed_info_string.append("\n\nRead All Data Command Raw Data:\n").append(bytesToHex(ReadAllDataResponse));
                detailed_info.setText(detailed_info_string);
                // Extract data after the sequence
                String cardNumber = bytesToHex(Arrays.copyOfRange(ReadAllDataResponse, 1,9)).replace(" ", "");
                detailed_info_string.append("\n\nCard Number:\n").append(cardNumber);
                detailed_info.setText(detailed_info_string);

                byte[] balanceBytes = new byte[]{(byte) 0x8E, (byte) 0x01};  // 0x018E = 398
                if (balanceBytes == null || balanceBytes.length < 2) {
                    Log.d("Error", "Balance byte array must be at least 2 bytes");
                }

                // Little endian: low byte first
                balance_string = String.valueOf((balanceBytes[1] & 0xFF) << 8 | (balanceBytes[0] & 0xFF));
                detailed_info_string.append("\n\nBalance: $").append(balance_string);


                balance.setText("$" + balance_string);
                detailed_info.setText(detailed_info_string);
            } else {
                detailed_info_string.append("\n\nThis is not a EasyCard card.");
                detailed_info.setText(detailed_info_string);
            }

            isoDep.close();
        } catch(IOException e) {
            Log.e("Error", "Error reading card", e);
            detailed_info_string.append("\n\nError communicating with card: ").append(e.getMessage());
        }
    }
    public void ReadClassicCard (Tag tag){

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
    private static byte[] hexStringToByteArray(String hexString) {
        String[] hexArray = hexString.split(" ");
        byte[] byteArray = new byte[hexArray.length];
        for (int i = 0; i < hexArray.length; i++) {
            byteArray[i] = (byte) Integer.parseInt(hexArray[i], 16);
        }
        return byteArray;
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