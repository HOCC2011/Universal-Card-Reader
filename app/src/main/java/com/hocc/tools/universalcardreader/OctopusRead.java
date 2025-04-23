package com.hocc.tools.universalcardreader;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.nfc.NfcAdapter;
import android.nfc.tech.NfcF;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class OctopusRead extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    TextView FelicaDetailedInfo;
    TextView balance_text;
    TextView link;
    TextView info;
    TextView conven35;
    TextView conven50;
    ImageView back;
    double card_balance_ori = 0;
    double card_balance = 0;
    String balance_string;

    @SuppressLint({"MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_octopus_read);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        FelicaDetailedInfo = findViewById(R.id.FelicaDetailedInfo);
        info = findViewById(R.id.conven_info);
        link = findViewById(R.id.link);
        back = findViewById(R.id.back);
        back.setOnClickListener(View -> {
            Intent intent = new Intent(OctopusRead.this, MainActivity.class);
            startActivity(intent);
        });

        balance_text = findViewById(R.id.balance);
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            FelicaDetailedInfo.setText("NFC is not available or enabled on this device.");
        }

        conven35 = findViewById(R.id.conven35);
        conven35.setOnClickListener(View -> {
            conven35.setTextColor(getColor(R.color.bg_color));
            conven35.setBackground(getResources().getDrawable(R.drawable.background));
            conven50.setTextColor(getColor(R.color.fg_color));
            conven50.setBackground(getResources().getDrawable(R.drawable.background_unselected));
            balance_string = String.format("%.1f", card_balance_ori);
            balance_text.setText("$" + balance_string);
        });
        conven50 = findViewById(R.id.conven50);
        conven50.setOnClickListener(View -> {
            conven50.setTextColor(getColor(R.color.bg_color));
            conven50.setBackground(getResources().getDrawable(R.drawable.background));
            conven35.setTextColor(getColor(R.color.fg_color));
            conven35.setBackground(getResources().getDrawable(R.drawable.background_unselected));
            card_balance = card_balance_ori - 15;
            balance_string = String.format("%.1f", card_balance);
            balance_text.setText("$" + balance_string);
        });
    }

    @SuppressLint("ResourceAsColor")
    protected void onCardTap(Tag tag) {
        Log.d("Debug", "Card Tapped!");
        Toast.makeText(this, "Reading Card...", Toast.LENGTH_LONG).show();
        StringBuilder result = new StringBuilder("");

        if (hasTech(tag, NfcF.class.getName())) {
            try {
                NfcF nfcF = NfcF.get(tag);
                nfcF.connect();

                byte[] idm = tag.getId();
                byte[] pmm = nfcF.getManufacturer();
                String idmStr = bytesToHex(idm);
                String pmmStr = bytesToHex(pmm);

                String icCode = null;
                String icType = null;
                String romType = null;
                if (pmm.length >= 1) {
                    icCode = pmmStr.substring(0, 4);
                    icType = pmmStr.substring(2, 4);
                    romType = pmmStr.substring(0, 2);
                }

                // --- Request System Code Command ---
                byte[] reqSysCode = new byte[10];
                reqSysCode[0] = 0x0A;        // Length
                reqSysCode[1] = 0x0C;        // Command Code: Request System Code (0x0C)
                System.arraycopy(idm, 0, reqSysCode, 2, 8); // IDm (8 bytes)

                byte[] response = nfcF.transceive(reqSysCode);

                List<String> systemCodes = new ArrayList<>();
                if (response.length >= 11) {
                    int numSysCodes = response[10] & 0xFF;
                    for (int i = 0; i < numSysCodes; i++) {
                        int offset = 11 + (i * 2);
                        if (offset + 1 < response.length) {
                            int sc1 = response[offset] & 0xFF;
                            int sc2 = response[offset + 1] & 0xFF;
                            String sysCode = String.format("%02X%02X", sc1, sc2);
                            systemCodes.add(sysCode);
                        }
                    }
                }

                result.append("PMm: ").append(pmmStr);
                result.append("\nIDm: ").append(idmStr);
                result.append("\nIC Code: 0x").append(icCode);
                result.append("\nIC type: 0x").append(icType);
                result.append("\nROM Type: 0x").append(romType);
                for (int i = 0; i < systemCodes.size(); i++) {
                    result.append("\nSystem Code ").append(i + 1).append(": 0x").append(systemCodes.get(i));
                    if (systemCodes.get(i).equals("8008")) {
                        result.append(" Octopus");
                    }
                    if (systemCodes.get(i).equals("8005")) {
                        result.append(" ShenZhenTong");
                    }
                }

                // Check if card is Octopus
                if (systemCodes.get(0).equals("8008") || systemCodes.get(1).equals("8008")) {
                    // Build Polling command only for system code 0x8008
                    byte[] polling = new byte[] {
                            0x06, // length
                            0x00, // command
                            (byte)0x80, 0x08, // system code
                            0x01, // request code: system + communication performance
                            0x0F  // time slot
                    };
                    polling[0] = (byte) polling.length;
                    polling[1] = 0x00;

                    try {
                        byte[] pollingResp = nfcF.transceive(polling);
                        if (pollingResp == null || pollingResp.length < 18) {
                            balance_text.setText("Error");
                            Log.e("Error", "Failed to read from system code 0x8008.");
                        }

                        byte[] newIDm = new byte[8];
                        System.arraycopy(pollingResp, 2, newIDm, 0, 8);

                        // Now try reading service 0x0117
                        ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        bout.write(0); // length
                        bout.write(0x06); // Read Without Encryption
                        bout.write(newIDm); // IDm
                        bout.write(0x01); // Service count
                        bout.write(new byte[]{0x17, 0x01}); // Service 0x0117
                        bout.write(0x01); // Block count
                        bout.write(new byte[]{(byte)0x80, 0x00}); // Block 0
                        byte[] cmd = bout.toByteArray();
                        cmd[0] = (byte) cmd.length;

                        byte[] resp = nfcF.transceive(cmd);
                        if (resp != null && resp.length > 13) {
                            byte[] block = new byte[16];
                            System.arraycopy(resp, 13, block, 0, 16);
                            result.append("\nService 0x0117 (Octopus Balance): ").append("\n").append(bytesToHex(block)); // Raw data
                            // Try to get Octopus balance
                            int value = ((block[2] & 0xFF) << 8) | (block[3] & 0xFF);
                            double balance = (value - 350) / 10.0;
                            card_balance_ori = balance;
                            conven50.setTextColor(getColor(R.color.bg_color));
                            conven50.setBackground(getResources().getDrawable(R.drawable.background));
                            conven35.setTextColor(getColor(R.color.fg_color));
                            conven35.setBackground(getResources().getDrawable(R.drawable.background_unselected));


                        } else {
                            balance_text.setText("Error");
                            Log.e("Error", "Failed to read from system code 0x8008.");
                        }

                    } catch (IOException e) {
                        balance_text.setText("Error");
                        Log.e("Error", "Failed to read from system code 0x8008. IOException", e);
                    }
                } else {
                    // To-do
                }

                if (systemCodes.get(0).equals("8005") && systemCodes.get(1).equals("8008")) { // Octopus + ShenZhenTong Card
                    info.setText("This is a Octopus + ShenZhenTong card.");
                    link.setVisibility(View.GONE);
                    conven35.setVisibility(View.GONE);
                    conven50.setVisibility(View.GONE);
                    balance_string = String.format("%.1f", card_balance_ori);
                    balance_text.setText("$" + balance_string);
                } else if ("043B".equals(icCode) && card_balance_ori > 0) { //T-Union Card with enough balance
                    info.setText("This is a T-union Octopus card.");
                    link.setText("Click here to check the exchange rate.");
                    link.setVisibility(View.VISIBLE);
                    link.setTextColor(getColor(R.color.link));
                    link.setOnClickListener(View -> {
                        Intent intent = new Intent(OctopusRead.this, OctopusReadTunion.class);
                        intent.putExtra("OctopusBalance", String.valueOf(card_balance_ori));
                        intent.putExtra("DetailedInfo", String.valueOf(result));
                        startActivity(intent);
                    });
                    conven35.setVisibility(View.GONE);
                    conven50.setVisibility(View.GONE);
                    balance_string = String.format("%.1f", card_balance_ori);
                    balance_text.setText("$" + balance_string);
                } else if ("043B".equals(icCode)) { //T-Union Card with not enough balance
                    info.setText("This is a T-union Octopus card.\nUnable to check the exchange rate because the balance equals or lower than 0.");
                    link.setVisibility(View.GONE);
                    conven35.setVisibility(View.GONE);
                    conven50.setVisibility(View.GONE);
                    balance_string = String.format("%.1f", card_balance_ori);
                    balance_text.setText("$" + balance_string);
                } else { // Normal cards
                    info.setText("This card may have a $35/50 convenience limit. Please select your card's convenience limit above.");
                    link.setText(Html.fromHtml("<a href='https://www.google.com'>Click here to know more.</a>"));
                    link.setMovementMethod(LinkMovementMethod.getInstance());
                    link.setTextColor(getColor(R.color.link));
                    link.setVisibility(View.GONE); //TO-DO add link
                    conven35.setVisibility(View.VISIBLE);
                    conven50.setVisibility(View.VISIBLE);
                    card_balance = card_balance_ori - 15;
                    balance_string = String.format("%.1f", card_balance);
                    balance_text.setText("$" + balance_string);
                }

                FelicaDetailedInfo.setText("Detailed info:\n" + result.toString());

                nfcF.close();

            } catch (Exception e) {
                Log.e("Error", "Exception", e);
            }
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
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    onCardTap(tag);
                                }
                            });
                        }
                    },
                    NfcAdapter.FLAG_READER_NFC_F | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    options
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableReaderMode(this);  // Disable reader mode to stop NFC listening
        }
    }

    private boolean hasTech(Tag tag, String techName) {
        for (String tech : tag.getTechList()) {
            if (tech.equals(techName)) return true;
        }
        return false;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
