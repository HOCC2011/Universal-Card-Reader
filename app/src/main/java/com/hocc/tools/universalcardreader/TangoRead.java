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
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.Arrays;

public class TangoRead extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    TextView balance_text;
    ImageView back;
    StringBuilder DetailedInfoString;
    TextView DetailedInfo;
    String CardNo;
    String balanceString;
    int cardType = 0; // 0 = Mifare, 1 = HCE

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tango_read);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        DetailedInfo = findViewById(R.id.DetailedInfo);
        back = findViewById(R.id.back);
        back.setOnClickListener(View -> {
            Intent intent = new Intent(TangoRead.this, MainActivity.class);
            startActivity(intent);
        });

        balance_text = findViewById(R.id.balance);
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            DetailedInfo.setText("NFC is not available or enabled on this device.");
        }
    }
    private void onCardTap(Tag tag) {
        Log.d("Debug", "Card Tapped!");
        balance_text.setText("-");
        Toast.makeText(this, "Reading Card...", Toast.LENGTH_LONG).show();
        DetailedInfoString = new StringBuilder("");
        IsoDep isoDep = IsoDep.get(tag);

        if (isoDep == null) {
            DetailedInfo.setText("This is not an IsoDep card");
            return;
        }

        try {
            isoDep.connect();

            byte[] DesfireSelect = new byte[]{(byte) 0x5A, (byte) 0x86, 0x42, 0x00}; // 3-byte AID (DESFire style)
            byte[] HCESelect = new byte[]{(byte) 0x00, (byte) 0xA4, 0x04, 0x00, (byte) 0x05, (byte) 0xF0, 0x00, (byte) 0x86, 0x42, 0x00}; // 5-byte AID (Android HCE style)

            byte[] responseApplication1 = isoDep.transceive(DesfireSelect);
            String Application1RawData = bytesToHex(responseApplication1);
            // DESFire card detected
            if (Application1RawData.equals("00")) { //To be changed
                DetailedInfoString.append("Application 1 Reply Code:\n").append(Application1RawData).append("\n\nDESFire card detected!");
                DetailedInfo.setText(DetailedInfoString.toString());
                Log.d("NFC", "DESFire card detected!");
                cardType = 0;
                DESFireReadFile1(isoDep);
            } else { // Else check for HCE Card
                responseApplication1 = isoDep.transceive(HCESelect);
                Application1RawData = bytesToHex(responseApplication1);
                Log.d("TAG", Application1RawData);
                if (Application1RawData.endsWith("8C")) { // HCE card detected
                    DetailedInfoString.append("Application 1 Raw Data:\n").append(Application1RawData).append("\n\nHCE card detected!");
                    DetailedInfo.setText(DetailedInfoString.toString());
                    Log.d("NFC", "HCE card detected!");
                    cardType = 1;
                    DetailedInfo.setText(DetailedInfoString.toString());
                    HCECardReadApplication1WithoutFetching(responseApplication1);
                    HCECardReadFinish(isoDep);
                } else { // Can't open applications in either way
                    DetailedInfoString.append("This is not a Tango card");
                    DetailedInfo.setText(DetailedInfoString.toString());
                }
            }
        } catch (IOException e) {
            Log.e("Error", "Error reading card", e);
            DetailedInfoString.append("\n\nError communicating with card: ").append(e.getMessage());
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

    /*------------Data Processing Methods------------ */
    private void DESFireReadFile1 (IsoDep isoDep) {
        try {
            byte[] ReadFile1Command = new byte[] {
                    (byte) 0xBD,
                    (byte) 0x01,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x09, (byte) 0x00, (byte) 0x00
            };
            byte[] ReadFile1Response = isoDep.transceive(ReadFile1Command);
            Log.d("ReadFile1Response", bytesToHex(ReadFile1Response));
            DetailedInfoString.append("\n\nRead File1 Response:\n").append(bytesToHex(ReadFile1Response));
            balanceString = bytesToHex(Arrays.copyOfRange(ReadFile1Response, 1, 4)).replace(" ", "");
            DetailedInfoString.append("\n\nCard No.:\n" + bytesToHex(Arrays.copyOfRange(ReadFile1Response, 4, ReadFile1Response.length)).replace(" ", ""));
            DetailedInfo.setText(DetailedInfoString.toString());
            if (balanceString.equals("86A000")) {
                Log.d("Account type: ", "Online (Tango Account)");
                DetailedInfoString.append("\n\nAccount type: Online (Tango Account)");
                DetailedInfo.setText(DetailedInfoString.toString());
            } else if (balanceString.equals("86AB00")) {
                Log.d("Account type: ", "Online (Other Payment Method)");
                DetailedInfoString.append("\n\nAccount type: Online (Other Payment Method)");
                DetailedInfo.setText(DetailedInfoString.toString());
            } else {
                DetailedInfoString.append("\n\nBalance: $" + CalculateBalance(balanceString));
                DetailedInfo.setText(DetailedInfoString.toString());
                balance_text.setText("$" + CalculateBalance(balanceString));
            }
        } catch (IOException e) {
            Log.e("Error", "Error reading card", e);
            DetailedInfoString.append("\n\nError communicating with card: ").append(e.getMessage());
        }
    }
    private void HCECardReadApplication1WithoutFetching (byte[] responseApplication1) {
        String balanceStringFromCard = bytesToHex(Arrays.copyOfRange(responseApplication1, 1, 4)).replace(" ", "");
        DetailedInfoString.append("\n\nCard No.: " + bytesToHex(Arrays.copyOfRange(responseApplication1, 4, 10)).replace(" ", ""));
        DetailedInfo.setText(DetailedInfoString.toString());
        if (balanceStringFromCard.equals("86A000")) {
            Log.d("Account type: ", "Online (Tango Account)");
            DetailedInfoString.append("\n\nAccount type: Online (Tango Account)");
            DetailedInfo.setText(DetailedInfoString.toString());
        } else if (balanceStringFromCard.equals("86AB00")) {
            Log.d("Account type: ", "Online (Other Payment Method)");
            DetailedInfoString.append("\n\nAccount type: Online (Other Payment Method)");
            DetailedInfo.setText(DetailedInfoString.toString());
        } else {
            DetailedInfoString.append("\n\nBalance: $" + CalculateBalance(balanceStringFromCard));
            DetailedInfo.setText(DetailedInfoString.toString());
            balance_text.setText("$" + CalculateBalance(balanceStringFromCard));
        }
    }
    private  void HCECardReadFinish(IsoDep isoDep) {
        try {
            byte[] header = new byte[]{(byte) 0x86, 0x75};
            isoDep.transceive(header);
            DetailedInfoString.append("\n\nSuccessfully transceived the end command!");
            DetailedInfo.setText(DetailedInfoString.toString());
        } catch (IOException e) {
            Log.e("Error", "Error reading card", e);
            DetailedInfoString.append("\n\nError transceiving the end command. ").append(e.getMessage());
        }
    }
    public double CalculateBalance (String balanceString){
        if (balanceString.startsWith("86A")){
            Log.d("Error", "The balance string looks like it's coming from a card with an online account.");
            return 0.0;
        } else if (balanceString.startsWith("F")) { //Negative balance
            balanceString = balanceString.substring(2, balanceString.length());
            String dollars = balanceString.substring(0, balanceString.length() - 2);
            String cents = balanceString.substring(balanceString.length() - 2);
            return Math.round(Float.parseFloat(dollars + "." + cents) * 10.0) / 10.0 * -1;
        } else { // Normal balance
            if (balanceString.length() < 3) {
                balanceString = String.format("%03d", Integer.parseInt(balanceString));
            }
            String dollars = balanceString.substring(0, balanceString.length() - 2);
            String cents = balanceString.substring(balanceString.length() - 2);
            return Math.round(Float.parseFloat(dollars + "." + cents) * 10.0) / 10.0;
        }
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