package com.itachi1706.cheesecakecepasreadersample;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.itachi1706.cepaslib.base.util.ByteUtils;
import com.itachi1706.cepaslib.card.cepas.CEPASCard;
import com.itachi1706.cepaslib.card.cepas.CEPASPurse;
import com.itachi1706.cepaslib.card.cepas.raw.RawCEPASCard;
import com.itachi1706.cepaslib.card.cepas.CEPASTagReader;

import java.io.IOException;

public class CANScannerActivity extends Activity {

    private static final String TAG = "CANScannerActivity";
    
    private TextView tvStatus;
    private TextView tvCANBytes;
    private TextView tvCardSerial;
    private TextView tvCalculation;
    private TextView tvPurseInfo;
    private Button btnScan;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;
    private String[][] techLists;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_can_scanner);

        // Initialize views
        tvStatus = findViewById(R.id.tvStatus);
        tvCANBytes = findViewById(R.id.tvCANBytes);
        tvCardSerial = findViewById(R.id.tvCardSerial);
        tvCalculation = findViewById(R.id.tvCalculation);
        tvPurseInfo = findViewById(R.id.tvPurseInfo);
        btnScan = findViewById(R.id.btnScan);

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            tvStatus.setText("NFC not available on this device");
            btnScan.setEnabled(false);
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            tvStatus.setText("Please enable NFC");
            btnScan.setEnabled(false);
            return;
        }

        // Setup NFC intent filters
        setupNfcIntentFilters();

        btnScan.setOnClickListener(v -> {
            if (!isScanning) {
                tvStatus.setText("Ready to scan CEPAS Purse 3 - Place card near NFC reader");
                isScanning = true;
                btnScan.setText("Stop Scanning");
                // Enable foreground dispatch when button is pressed
                enableForegroundDispatch();
            } else {
                tvStatus.setText("Scanning stopped");
                isScanning = false;
                btnScan.setText("Scan CEPAS Purse 3");
                disableForegroundDispatch();
            }
        });

        // Check if we have a tag from the intent (in case app was launched by NFC)
        if (getIntent() != null && getIntent().getAction() != null) {
            handleNfcIntent(getIntent());
        }
    }

    private void setupNfcIntentFilters() {
        // Create a PendingIntent for NFC intents with proper flags
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);

        // Create intent filters for different NFC actions
        intentFilters = new IntentFilter[]{
            new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
            new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        };

        // Specify the technologies we want to handle
        techLists = new String[][]{
            new String[]{IsoDep.class.getName()}
        };
    }

    private void enableForegroundDispatch() {
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            try {
                nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, techLists);
                Log.d(TAG, "Foreground dispatch enabled");
            } catch (Exception e) {
                Log.e(TAG, "Error enabling foreground dispatch", e);
                tvStatus.setText("Error enabling NFC: " + e.getMessage());
            }
        }
    }

    private void disableForegroundDispatch() {
        if (nfcAdapter != null) {
            try {
                nfcAdapter.disableForegroundDispatch(this);
                Log.d(TAG, "Foreground dispatch disabled");
            } catch (Exception e) {
                Log.e(TAG, "Error disabling foreground dispatch", e);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        Log.d(TAG, "New intent received: " + intent.getAction());
        
        // Handle the NFC intent
        handleNfcIntent(intent);
    }

    private void handleNfcIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                Log.d(TAG, "Tag discovered: " + ByteUtils.getHexString(tag.getId()));
                scanCard(tag);
            } else {
                Log.e(TAG, "Tag is null");
                tvStatus.setText("Error: No tag data received");
            }
        }
    }

    private void scanCard(Tag tag) {
        tvStatus.setText("Scanning CEPAS Purse 3...");
        
        try {
            // Get tag ID
            byte[] tagId = tag.getId();
            Log.d(TAG, "Tag ID: " + ByteUtils.getHexString(tagId));
            
            // Create CEPAS tag reader
            CEPASTagReader tagReader = new CEPASTagReader(tagId, tag);
            
            // Read the raw CEPAS card
            RawCEPASCard rawCard = tagReader.readTag();
            Log.d(TAG, "Raw card read successfully");
            
            // Parse to get the CEPAS card
            CEPASCard cepasCard = rawCard.parse();
            Log.d(TAG, "CEPAS card parsed successfully");
            
            // Only scan for Purse 3 (CEPAS Purse 3)
            CEPASPurse purse = cepasCard.getPurse(3);
            
            if (purse != null && purse.isValid()) {
                Log.d(TAG, "CEPAS Purse 3 is valid");
                
                // Display purse information
                displayPurseInfo(purse);
                
                // Get CAN bytes from the purse
                if (purse.getCAN() != null) {
                    byte[] canBytes = purse.getCAN().bytes();
                    Log.d(TAG, "CAN bytes: " + ByteUtils.getHexString(canBytes));
                    extractAndConvertCAN(canBytes);
                } else {
                    Log.e(TAG, "CAN is null");
                    tvStatus.setText("Error: CAN data is null in Purse 3");
                    tvCANBytes.setText("CAN Bytes: Not available");
                    tvCardSerial.setText("Card Serial: Not available");
                    tvCalculation.setText("Calculation: Not available");
                }
            } else {
                Log.e(TAG, "CEPAS Purse 3 is invalid or null");
                tvStatus.setText("Error: CEPAS Purse 3 not found or invalid. This app only scans for Purse 3 data.");
                tvCANBytes.setText("CAN Bytes: Not available");
                tvCardSerial.setText("Card Serial: Not available");
                tvCalculation.setText("Calculation: Not available");
                tvPurseInfo.setText("Purse 3 Status: " + (purse != null ? "Invalid" : "Not found"));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error scanning card", e);
            tvStatus.setText("Error: " + e.getMessage());
            tvCANBytes.setText("CAN Bytes: Error occurred");
            tvCardSerial.setText("Card Serial: Error occurred");
            tvCalculation.setText("Calculation: Error occurred");
            tvPurseInfo.setText("Purse Info: Error occurred");
            e.printStackTrace();
        }
    }

    private void displayPurseInfo(CEPASPurse purse) {
        StringBuilder info = new StringBuilder();
        info.append("CEPAS Purse 3 Information:\n\n");
        info.append("CEPAS Version: ").append(purse.getCepasVersion()).append("\n");
        info.append("Purse Status: ").append(purse.getPurseStatus()).append("\n");
        info.append("Purse Balance: $").append(String.format("%.2f", purse.getPurseBalance() / 100.0)).append("\n");
        info.append("Auto Load Amount: $").append(String.format("%.2f", purse.getAutoLoadAmount() / 100.0)).append("\n");
        
        if (purse.getCSN() != null) {
            info.append("CSN: ").append(purse.getCSN().hex()).append("\n");
        }
        
        info.append("Purse Creation Date: ").append(new java.util.Date(purse.getPurseCreationDate() * 1000L)).append("\n");
        info.append("Purse Expiry Date: ").append(new java.util.Date(purse.getPurseExpiryDate() * 1000L)).append("\n");
        info.append("Logfile Record Count: ").append(purse.getLogfileRecordCount()).append("\n");
        
        tvPurseInfo.setText(info.toString());
    }

    private void extractAndConvertCAN(byte[] canBytes) {
        if (canBytes == null) {
            tvStatus.setText("Error: CAN bytes are null");
            return;
        }
        
        if (canBytes.length != 8) {
            tvStatus.setText("Error: CAN bytes should be 8 bytes, got " + canBytes.length);
            return;
        }

        // Display CAN bytes
        StringBuilder canHex = new StringBuilder();
        for (byte b : canBytes) {
            canHex.append(String.format("%02X ", b));
        }
        tvCANBytes.setText("CAN Bytes (8-15): " + canHex.toString().trim());

        // Convert to 64-bit integer using byteArrayToLong
        long cardSerial = ByteUtils.byteArrayToLong(canBytes);

        // Display result
        tvCardSerial.setText("Card Serial (64-bit): " + cardSerial);

        // Show detailed calculation
        showCalculation(canBytes, cardSerial);

        tvStatus.setText("CEPAS Purse 3 scan completed successfully!");
    }

    private void showCalculation(byte[] canBytes, long result) {
        StringBuilder calc = new StringBuilder();
        calc.append("CAN ID Calculation (byteArrayToLong method):\n\n");
        calc.append("Algorithm: (byte & 0xFF) << shift, where shift = (length - 1 - i) * 8\n\n");
        
        long total = 0;
        for (int i = 0; i < canBytes.length; i++) {
            int shift = (canBytes.length - 1 - i) * 8;
            long byteValue = (long) (canBytes[i] & 0x000000FF) << shift;
            total += byteValue;
            
            calc.append(String.format("Byte %d: 0x%02X (%d) << %d = %d\n", 
                i, canBytes[i] & 0xFF, canBytes[i] & 0xFF, shift, byteValue));
        }
        
        calc.append(String.format("\nTotal: %d\n", total));
        calc.append(String.format("Hex: 0x%016X\n", total));
        calc.append(String.format("Binary: %s", Long.toBinaryString(total)));
        
        tvCalculation.setText(calc.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        
        // If we're scanning, re-enable foreground dispatch
        if (isScanning) {
            enableForegroundDispatch();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        disableForegroundDispatch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
        disableForegroundDispatch();
    }
}
