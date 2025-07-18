package com.hocc.tools.universalcardreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
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
    private ImageView keySettings;
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
        keySettings = findViewById(R.id.keySettings);
        keySettings.setOnClickListener(View -> {
            Intent intent = new Intent(EasyCardRead.this, EasyCardKeySettings.class);
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
            detailed_info_string.append("Card isn't new card with IsoDep.");
            detailed_info.setText(detailed_info_string);
            ReadClassicCard(tag);
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

                byte[] balanceBytes = Arrays.copyOfRange(ReadAllDataResponse, 39, 41);
                Log.d("Balance Bytes", bytesToHex(balanceBytes));
                if (balanceBytes == null || balanceBytes.length < 2) {
                    Log.d("Error", "Balance byte array must be at least 2 bytes");
                }

                // Little endian: low byte first
                balance_string = String.valueOf((balanceBytes[1] & 0xFF) << 8 | (balanceBytes[0] & 0xFF));
                detailed_info_string.append("\n\nBalance: $").append(balance_string);


                balance.setText("$" + balance_string);
                detailed_info.setText(detailed_info_string);
            } else {
                detailed_info_string.append("\n\nThis is not an EasyCard card.");
                detailed_info.setText(detailed_info_string);
            }

            isoDep.close();
        } catch(IOException e) {
            Log.e("Error", "Error reading card", e);
            detailed_info_string.append("\n\nError communicating with card: ").append(e.getMessage());
        }
    }
    public void ReadClassicCard (Tag tag){
        MifareClassic mifareClassic = MifareClassic.get(tag);
        if (mifareClassic == null) {
            detailed_info_string.append("\n\nThis card is not an EasyCard");
            detailed_info.setText(detailed_info_string);
        }

        try {
            mifareClassic.connect();

            Log.d("UID", bytesToHex(tag.getId()).replace(" ", ""));
            SharedPreferences prefs = getSharedPreferences("Sector2Keys", MODE_PRIVATE);
            byte[] Sector2Key = hexStringToByteArray(prefs.getString(bytesToHex(tag.getId()).replace(" ", ""), "FFFFFFFFFFFF"));
            boolean auth = mifareClassic.authenticateSectorWithKeyA(2, Sector2Key);
            if (bytesToHex(Sector2Key).replace(" ", "").equals("FFFFFFFFFFFF")){
                detailed_info_string.append("\n\nKeyA of sector 2 for this card is default key, please change it by pressing the key icon on the top right corner.");
                detailed_info.setText(detailed_info_string);
            } else {
                detailed_info_string.append("\n\nKeyA of sector 2: " + bytesToHex(Sector2Key));
                detailed_info.setText(detailed_info_string);
            }
            if (!auth) {
                detailed_info_string.append("\n\nAuthentication failed.");
                detailed_info.setText(detailed_info_string);
            } else {
                byte[] Sector2Data = mifareClassic.readBlock(mifareClassic.sectorToBlock(2) + 1);
                Log.d("Block data:", bytesToHex(Sector2Data));
                detailed_info_string.append("\n\nSector 2 Block 1 Raw Data:\n").append(bytesToHex(Sector2Data));
                detailed_info.setText(detailed_info_string);

                byte[] balanceBytes = Arrays.copyOfRange(Sector2Data, 0, 2);
                Log.d("Balance Bytes", bytesToHex(balanceBytes));
                if (balanceBytes == null || balanceBytes.length < 2) {
                    Log.d("Error", "Balance byte array must be at least 2 bytes");
                }
                // Little endian: low byte first
                balance_string = String.valueOf((balanceBytes[1] & 0xFF) << 8 | (balanceBytes[0] & 0xFF));
                detailed_info_string.append("\n\nBalance: $").append(balance_string);

                balance.setText("$" + balance_string);
                detailed_info.setText(detailed_info_string);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                mifareClassic.close();
            } catch (IOException ignored) {}
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