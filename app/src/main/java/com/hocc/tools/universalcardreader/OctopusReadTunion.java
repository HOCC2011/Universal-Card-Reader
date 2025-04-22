package com.hocc.tools.universalcardreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class OctopusReadTunion extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private TextView detailed_info;
    private TextView ExchangeRateText;
    private TextView balance;
    private ImageView back;
    private TextView hint;
    String FelicaDetailedInfo;
    StringBuilder detailed_info_string;
    StringBuilder balance_string;
    Double OctopusBalance;
    Double RmbOctopusBalance;
    Double ExchangeRate;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_octopus_read_tunion);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String OctopusBalanceString = extras.getString("OctopusBalance");
            FelicaDetailedInfo = extras.getString("DetailedInfo");
            Log.d("IntentData_String", String.valueOf(OctopusBalanceString));
            if (OctopusBalanceString != null){
                OctopusBalance = Double.parseDouble(OctopusBalanceString);
                Log.d("IntentData_Double", String.valueOf(OctopusBalance));
            }
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        detailed_info = findViewById(R.id.DetailedInfo);
        ExchangeRateText = findViewById(R.id.exchange_rate);
        hint = findViewById(R.id.NFC_hint);
        back = findViewById(R.id.back);
        back.setOnClickListener(View -> {
            Intent intent = new Intent(OctopusReadTunion.this, OctopusRead.class);
            startActivity(intent);
        });
        balance = findViewById(R.id.balance);
        balance.setText("$" + OctopusBalance);
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            detailed_info.setText("NFC not available or not enabled.");
        }
    }

    private void onCardTap(Tag tag) {
        Log.d("Debug", "Card Tapped!");
        Toast.makeText(this, "Reading Card...", Toast.LENGTH_LONG).show();
        hint.setVisibility(View.GONE);
        detailed_info_string = new StringBuilder("");
        detailed_info_string.append("Felica Data:\n").append(FelicaDetailedInfo);
        ExchangeRateText.setText("1 : -");
        balance_string = new StringBuilder("");
        IsoDep isoDep = IsoDep.get(tag);

        if (isoDep == null) {
            detailed_info.setText("This is not a T-Union card.");
            return;
        }

        try {
            isoDep.connect();

            // SELECT PPSE AID: 2PAY.SYS.DDF01
            byte[] ppseSelectCommand = new byte[]{
                    (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00,
                    (byte) 0x0E,
                    (byte) '2', (byte) 'P', (byte) 'A', (byte) 'Y',
                    (byte) '.', (byte) 'S', (byte) 'Y', (byte) 'S',
                    (byte) '.', (byte) 'D', (byte) 'D', (byte) 'F',
                    (byte) '0', (byte) '1',
                    (byte) 0x00
            };

            byte[] response = isoDep.transceive(ppseSelectCommand);
            String ppseRawData = bytesToHex(response);
            String ppseString = new String(response);
            detailed_info_string.append("\n\nT-Union Data:").append("\n\nPPSE Raw Data:\n").append(ppseRawData);

            if (ppseString.contains("MOT_T_EP")) {
                detailed_info_string.append("\n\nThis is a T-Union card.");

                // Select MOT_T_EP AID
                byte[] selectAID = new byte[]{
                        (byte) 0x00, (byte) 0xA4, 0x04, 0x00,
                        0x08,
                        (byte) 0xA0, 0x00, 0x00, 0x06, 0x32, 0x01, 0x01, 0x05,
                        0x00
                };


                byte[] motEpResponse = isoDep.transceive(selectAID);
                Log.d("Debug", bytesToHex(motEpResponse));
                detailed_info_string.append("\n\nMOT_T_EP Raw Data:\n").append(bytesToHex(motEpResponse));

                // Extract data after the sequence
                byte[] rawCardNumber = extractDataAfterSequence(bytesToHex(motEpResponse), "FF FF FF FF 02 01");
                String cardNumber = null;

                if (rawCardNumber != null) {
                    cardNumber = formatCardNumber(bytesToHex(rawCardNumber).replace(" ", ""));
                    detailed_info_string.append("\n\nCard Number:\n").append(cardNumber);
                } else {
                    detailed_info_string.append("\n\nCan't read the card's number.");
                }

                byte[] getBalance = new byte[]{(byte) 0x80, (byte) 0x5C, 0x00, 0x02, 0x04};
                byte[] balanceResponse = isoDep.transceive(getBalance);
                detailed_info_string.append("\n\nBalance Raw Data:\n").append(bytesToHex(balanceResponse));
                double value = 0.0;

                // Parse balance if valid
                if (balanceResponse.length >= 6 &&
                        balanceResponse[balanceResponse.length - 2] == (byte) 0x90 &&
                        balanceResponse[balanceResponse.length - 1] == (byte) 0x00) {

                    value = ((balanceResponse[0] & 0xFF) << 24) |
                            ((balanceResponse[1] & 0xFF) << 16) |
                            ((balanceResponse[2] & 0xFF) << 8) |
                            (balanceResponse[3] & 0xFF);
                    if(cardNumber.contains("8500")) {
                        RmbOctopusBalance = value / 100.0;
                        ExchangeRate = Math.floor(RmbOctopusBalance / OctopusBalance * 1000) / 1000;
                        ExchangeRateText.setText("1 : " + String.valueOf(ExchangeRate) + " - " + String.valueOf(ExchangeRate + 0.001));
                        Log.d("RMB Balance", String.valueOf(RmbOctopusBalance));
                        Log.d("Exchange Rate", String.valueOf(ExchangeRate));
                        detailed_info_string.append("\n\nBalance (RMB): ¥").append(value / 100.0);
                    } else {
                        detailed_info_string.append("\n\nThis is not a T-Union Octopus.\n\nBalance (RMB): ¥").append(value / 100.0);
                        Toast.makeText(this, "This is not a T-Union Octopus.", Toast.LENGTH_LONG).show();
                    }
                }

            } else if (ppseRawData.trim().equals("6A 82")) {
                detailed_info_string.append("\n\nThis card doesn't contain a Proximity Payment System Environment.")
                        .append("\n\nThis is not a T-Union card.");
            } else {
                detailed_info_string.append("\n\nThis is not a T-Union card.");
            }

            isoDep.close();
        } catch (IOException e) {
            Log.e("Error", "Error reading card", e);
            detailed_info_string.append("\n\nError communicating with card: ").append(e.getMessage());
        }

        detailed_info.setText(detailed_info_string.toString());
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

    // Extract 10 bytes after the sequence
    public static byte[] extractDataAfterSequence(String hexData, String sequence) {
        byte[] dataBytes = hexStringToByteArray(hexData);
        byte[] sequenceBytes = hexStringToByteArray(sequence);

        int seqIndex = findSequenceIndex(dataBytes, sequenceBytes);

        if (seqIndex == -1) {
            Log.e("HexDataExtractor", "Sequence not found.");
            return null;
        }

        // Extract the next 10 bytes after the sequence
        int startIndex = seqIndex + sequenceBytes.length;
        int endIndex = startIndex + 10;

        if (endIndex > dataBytes.length) {
            Log.e("HexDataExtractor", "Not enough data after the sequence.");
            return null;
        }

        byte[] extractedData = new byte[10];
        System.arraycopy(dataBytes, startIndex, extractedData, 0, 10);
        return extractedData;
    }

    // Find the index of the sequence in the data
    private static int findSequenceIndex(byte[] dataBytes, byte[] sequenceBytes) {
        for (int i = 0; i <= dataBytes.length - sequenceBytes.length; i++) {
            boolean match = true;
            for (int j = 0; j < sequenceBytes.length; j++) {
                if (dataBytes[i + j] != sequenceBytes[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1; // Sequence not found
    }

    private static String formatCardNumber(String cardNumber) {
        // Remove the first character
        cardNumber = cardNumber.substring(1);  // Remove the first character

        // Format the remaining card number in XXXX XXXX XXXX XXXX XXX
        StringBuilder formatted = new StringBuilder();
        int index = 0;
        for (int i = 0; i < cardNumber.length(); i++) {
            formatted.append(cardNumber.charAt(i));
            index++;
            if (index == 4 && i != cardNumber.length() - 1) {
                formatted.append(" ");  // Add space every 4 characters
                index = 0;
            }
        }
        return formatted.toString();
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