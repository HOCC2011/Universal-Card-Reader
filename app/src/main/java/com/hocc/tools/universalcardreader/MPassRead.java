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

public class MPassRead extends AppCompatActivity {
    private NfcAdapter nfcAdapter;
    private TextView detailed_info;
    private TextView balance;
    private ImageView back;
    StringBuilder detailed_info_string;
    StringBuilder balance_string;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mpass_read);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        detailed_info = findViewById(R.id.DetailedInfo);
        back = findViewById(R.id.back);
        back.setOnClickListener(View -> {
            Intent intent = new Intent(MPassRead.this, MainActivity.class);
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
        balance_string = new StringBuilder("");
        balance.setText("-");
        IsoDep isoDep = IsoDep.get(tag);

        if (isoDep == null) {
            detailed_info.setText("This is not a new Macau Pass card.");
            return;
        }

        try {
            isoDep.connect();

            // Select MPass AID (Required for TU hybrid card)  41 4D 54 4A 41 56 41 43 41 52 44 11
            byte[] selectAID = new byte[]{
                    (byte) 0x00, (byte) 0xA4, 0x04, 0x00, 0x0B, 0x41, 0x4D, 0x54, 0x4A, 0x41, 0x56, 0x41, 0x43, 0x41, 0x52, 0x44, 0x11
            };
            isoDep.transceive(selectAID);

            // Select MPass AID (Actual App)  C4 C3 C5 CD A8 C7 AE B0 FC
            byte[] selectAID2 = new byte[]{
                    (byte) 0x00, (byte) 0xA4, 0x04, 0x00,
                    0x0A, (byte) 0xB0, (byte) 0xC4, (byte)0xC3, (byte)0xC5, (byte)0xCD, (byte)0xA8, (byte)0xC7, (byte)0xAE, (byte)0xB0, (byte)0xFC
            };

            byte[] MPassResponse = isoDep.transceive(selectAID2);
            Log.d("Debug", bytesToHex(MPassResponse));
            detailed_info_string.append("MPass Application Raw Data:\n").append(bytesToHex(MPassResponse));

            if (bytesToHex(MPassResponse).contains("90 00")) {
                // Extract data after the sequence
                String cardNumber = bytesToHex(MPassResponse).replace(" ", "").substring(116, bytesToHex(MPassResponse).replace(" ", "").length() - 14);
                detailed_info_string.append("\n\nCard Number:\n").append(cardNumber);

                byte[] MPassCommand1 = new byte[]{
                        0x00, (byte) 0xB0, (byte) 0x95, 0x0C, 0x1C
                };
                byte[] MPassCommand1Response = isoDep.transceive(MPassCommand1);
                detailed_info_string.append("\n\nMPass Command1 Raw Data:\n").append(bytesToHex(MPassCommand1Response));

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

                    balance_string.append(value / 10.0 - 10.0);
                    detailed_info_string.append("\n\nBalance: $").append(value / 10.0 - 10.0).append(" (Interpreted == Original - 1)");

                    balance.setText("$" + balance_string);
                    detailed_info.setText(detailed_info_string);
                }

                byte[] MPassCommand2 = new byte[]{
                        0x00, (byte) 0xB0, (byte) 0x95, 0x00, 0x00
                };
                byte[] MPassCommand2Response = isoDep.transceive(MPassCommand2);
                detailed_info_string.append("\n\nMPass Command2 Raw Data:\n").append(bytesToHex(MPassCommand2Response));

                byte[] MPassCommand3 = new byte[]{
                        0x00, (byte) 0xB2, (byte) 0x04, (byte) 0xBC, 0x00
                };
                byte[] MPassCommand3Response = isoDep.transceive(MPassCommand3);
                detailed_info_string.append("\n\nMPass Command3 Raw Data:\n").append(bytesToHex(MPassCommand3Response));

                byte[] MPassCommand4 = new byte[]{
                        0x00, (byte) 0xB2, (byte) 0x01, (byte) 0xC4, 0x00
                };
                byte[] MPassCommand4Response = isoDep.transceive(MPassCommand4);
                detailed_info_string.append("\n\nMPass Command4 Raw Data:\n").append(bytesToHex(MPassCommand4Response));

                byte[] MPassCommand5 = new byte[]{
                        0x00, (byte) 0xB2, (byte) 0x01, (byte) 0xCC, 0x00
                };
                byte[] MPassCommand5Response = isoDep.transceive(MPassCommand5);
                detailed_info_string.append("\n\nMPass Command5 Raw Data:\n").append(bytesToHex(MPassCommand5Response));

                byte[] MPassCommand6 = new byte[]{
                        0x00, (byte) 0xB2, (byte) 0x02, (byte) 0xCC, 0x00
                };
                byte[] MPassCommand6Response = isoDep.transceive(MPassCommand6);
                detailed_info_string.append("\n\nMPass Command6 Raw Data:\n").append(bytesToHex(MPassCommand6Response));

                byte[] MPassCommand7 = new byte[]{
                        0x00, (byte) 0xB0, (byte) 0x9C, (byte) 0x00, 0x00
                };
                byte[] MPassCommand7Response = isoDep.transceive(MPassCommand7);
                detailed_info_string.append("\n\nMPass Command7 Raw Data:\n").append(bytesToHex(MPassCommand7Response));

                detailed_info.setText(detailed_info_string);
            } else {
                detailed_info_string.append("\n\nThis is not a Macau Pass card.");
                detailed_info.setText(detailed_info_string);
            }

            isoDep.close();
        } catch(IOException e) {
            Log.e("Error", "Error reading card", e);
            detailed_info_string.append("\n\nError communicating with card: ").append(e.getMessage());
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