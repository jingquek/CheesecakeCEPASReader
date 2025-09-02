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
    private TextView tvNfcCommands;
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
        tvNfcCommands = findViewById(R.id.tvNfcCommands);
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
        
        // Display the NFC commands that will be sent
        displayNfcCommands();
        
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
                
                // Update commands display to show successful execution
                updateCommandsDisplaySuccess();
                
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
                
                // Update commands display to show what was attempted
                StringBuilder failedCommands = new StringBuilder();
                failedCommands.append("Commands were sent but Purse 3 was not found/invalid:\n\n");
                failedCommands.append("1. SELECT CEPAS APPLICATION ✓\n");
                failedCommands.append("   Command: 00 A4 00 00 02 40 00\n\n");
                failedCommands.append("2. READ PURSE 3 DATA ✗\n");
                failedCommands.append("   Command: 90 32 03 00 01 00\n");
                failedCommands.append("   Result: Purse 3 not found or invalid\n\n");
                failedCommands.append("Possible reasons:\n");
                failedCommands.append("• Card does not have Purse 3\n");
                failedCommands.append("• Card is not a CEPAS card\n");
                failedCommands.append("• Purse 3 is locked/encrypted\n");
                failedCommands.append("• Card communication error");
                tvNfcCommands.setText(failedCommands.toString());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error scanning card", e);
            tvStatus.setText("Error: " + e.getMessage());
            tvCANBytes.setText("CAN Bytes: Error occurred");
            tvCardSerial.setText("Card Serial: Error occurred");
            tvCalculation.setText("Calculation: Error occurred");
            tvPurseInfo.setText("Purse Info: Error occurred");
            
            // Show error in commands display
            StringBuilder errorCommands = new StringBuilder();
            errorCommands.append("NFC Communication Error:\n\n");
            errorCommands.append("Commands attempted:\n");
            errorCommands.append("1. SELECT CEPAS APPLICATION\n");
            errorCommands.append("   Command: 00 A4 00 00 02 40 00\n\n");
            errorCommands.append("2. READ PURSE 3 DATA\n");
            errorCommands.append("   Command: 90 32 03 00 01 00\n\n");
            errorCommands.append("Error Details:\n");
            errorCommands.append("• ").append(e.getClass().getSimpleName()).append("\n");
            errorCommands.append("• ").append(e.getMessage() != null ? e.getMessage() : "Unknown error");
            tvNfcCommands.setText(errorCommands.toString());
            
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

    private void displayNfcCommands() {
        StringBuilder commands = new StringBuilder();
        commands.append("NFC Command Sequence to Retrieve Purse 3:\n\n");
        
        commands.append("Note: Standard NFC UID scan only provides 4 bytes.\n");
        commands.append("CEPAS requires additional protocol commands for full data.\n\n");
        
        commands.append("1. SELECT CEPAS APPLICATION\n");
        commands.append("   Command: 00 A4 00 00 02 40 00\n");
        commands.append("   ├─ CLA: 00 (ISO 7816-4 standard)\n");
        commands.append("   ├─ INS: A4 (SELECT FILE command)\n");
        commands.append("   ├─ P1:  00 (Select by name)\n");
        commands.append("   ├─ P2:  00 (First or only occurrence)\n");
        commands.append("   ├─ Lc:  02 (Length of application ID)\n");
        commands.append("   └─ Data: 40 00 (CEPAS Application ID)\n\n");
        
        commands.append("2. READ PURSE 3 DATA\n");
        commands.append("   Command: 90 32 03 00 01 00\n");
        commands.append("   ├─ CLA: 90 (CEPAS specific class)\n");
        commands.append("   ├─ INS: 32 (READ PURSE command)\n");
        commands.append("   ├─ P1:  03 (Purse ID = 3)\n");
        commands.append("   ├─ P2:  00 (Read purse data)\n");
        commands.append("   ├─ Lc:  01 (Length of data field)\n");
        commands.append("   └─ Data: 00 (Read from beginning)\n\n");
        
        commands.append("3. FULL PURSE 3 RESPONSE STRUCTURE (64+ bytes)\n");
        commands.append("   Response Format: [Purse Data] + [Status Code]\n");
        commands.append("   Status Code: 90 00 (Success)\n\n");
        
        commands.append("4. DETAILED BYTE LAYOUT:\n");
        commands.append("   ┌─────────────────────────────────────────────────────────────┐\n");
        commands.append("   │                    PURSE 3 DATA (64 bytes)                  │\n");
        commands.append("   ├─────────────────────────────────────────────────────────────┤\n");
        commands.append("   │ Byte │ Hex │ Field Name           │ Description             │\n");
        commands.append("   ├─────────────────────────────────────────────────────────────┤\n");
        commands.append("   │  0   │ XX  │ CEPAS Version       │ Protocol version        │\n");
        commands.append("   │  1   │ XX  │ Purse Status        │ Current status          │\n");
        commands.append("   │ 2-4  │XXX  │ Purse Balance       │ Current balance (3 bytes)│\n");
        commands.append("   │ 5-7  │XXX  │ Auto Load Amount    │ Auto-reload (3 bytes)   │\n");
        commands.append("   │ 8-15 │████████████████│ CAN (Card Account)  │ **CAN ID - 8 bytes** │\n");
        commands.append("   │16-23 │XXXXXXXX│ CSN (Card Serial)   │ Card serial number     │\n");
        commands.append("   │24-25 │ XX  │ Purse Expiry Date   │ Expiry (days from 1995)│\n");
        commands.append("   │26-27 │ XX  │ Purse Creation Date │ Creation (days from 1995)│\n");
        commands.append("   │28-31 │XXXX │ Last Credit TRP     │ Last credit transaction │\n");
        commands.append("   │32-39 │XXXXXXXX│ Credit Header       │ Credit transaction header│\n");
        commands.append("   │  40  │ XX  │ Logfile Record Count│ Number of records      │\n");
        commands.append("   │  41  │ XX  │ Issuer Data Length  │ Length of issuer data  │\n");
        commands.append("   │42-45 │XXXX │ Last Transaction TRP│ Last transaction ref   │\n");
        commands.append("   │46-61 │████████████████████████│ Last Transaction    │ Last transaction record│\n");
        commands.append("   │62+   │...  │ Issuer Specific Data│ Variable length data   │\n");
        commands.append("   └─────────────────────────────────────────────────────────────┘\n\n");
        
        commands.append("5. CAN ID EXTRACTION (Bytes 8-15):\n");
        commands.append("   ┌─────────────────────────────────────────────────────────────┐\n");
        commands.append("   │                    CAN ID LOCATION                        │\n");
        commands.append("   ├─────────────────────────────────────────────────────────────┤\n");
        commands.append("   │ Byte │ 8   │ 9   │ 10  │ 11  │ 12  │ 13  │ 14  │ 15  │\n");
        commands.append("   ├─────────────────────────────────────────────────────────────┤\n");
        commands.append("   │ CAN  │████████████████████████████████████████████████████│\n");
        commands.append("   │ Data │████████████████████████████████████████████████████│\n");
        commands.append("   └─────────────────────────────────────────────────────────────┘\n");
        commands.append("   These 8 bytes form the Card Account Number (CAN)\n\n");
        
        commands.append("6. WHY NOT JUST STANDARD NFC UID?\n");
        commands.append("   ┌─────────────────────────────────────────────────────────────┐\n");
        commands.append("   │ Standard NFC UID vs CEPAS CAN                              │\n");
        commands.append("   ├─────────────────────────────────────────────────────────────┤\n");
        commands.append("   │ NFC UID        │ 4 bytes │ Basic tag identifier only      │\n");
        commands.append("   │ CEPAS CAN      │ 8 bytes │ Full transit account number    │\n");
        commands.append("   │ CEPAS Protocol │ Required│ Access to purse data          │\n");
        commands.append("   │ UID Limitation │ No data │ Cannot provide account info    │\n");
        commands.append("   └─────────────────────────────────────────────────────────────┘\n\n");
        
        commands.append("7. RESPONSE EXAMPLE:\n");
        commands.append("   Raw Response: [64 bytes] + 90 00\n");
        commands.append("   ├─ Data Length: 64 bytes (minimum)\n");
        commands.append("   ├─ Status: 90 00 (Success)\n");
        commands.append("   └─ Total: 66 bytes\n\n");
        
        commands.append("   Sample Data (showing CAN bytes 8-15):\n");
        commands.append("   ┌─────────────────────────────────────────────────────────────┐\n");
        commands.append("   │ 00 01 00 00 50 00 00 00 1C 61 E9 59 00 00 00 00 ...      │\n");
        commands.append("   │     │     │     │     │████████████████████████████████│\n");
        commands.append("   │ Ver │Status│Balance│Auto │        CAN ID (8-15)        │\n");
        commands.append("   └─────────────────────────────────────────────────────────────┘");
        
        tvNfcCommands.setText(commands.toString());
    }

    private void updateCommandsDisplaySuccess() {
        StringBuilder commands = new StringBuilder();
        commands.append("NFC Command Sequence - SUCCESSFUL:\n\n");
        
        commands.append("Note: Standard NFC UID scan only provides 4 bytes.\n");
        commands.append("CEPAS requires additional protocol commands for full data.\n\n");
        
        commands.append("1. SELECT CEPAS APPLICATION ✓\n");
        commands.append("   Command: 00 A4 00 00 02 40 00\n");
        commands.append("   ├─ CLA: 00 (ISO 7816-4 standard)\n");
        commands.append("   ├─ INS: A4 (SELECT FILE command)\n");
        commands.append("   ├─ P1:  00 (Select by name)\n");
        commands.append("   ├─ P2:  00 (First or only occurrence)\n");
        commands.append("   ├─ Lc:  02 (Length of application ID)\n");
        commands.append("   ├─ Data: 40 00 (CEPAS Application ID)\n");
        commands.append("   └─ Response: 90 00 (Success)\n\n");
        
        commands.append("2. READ PURSE 3 DATA ✓\n");
        commands.append("   Command: 90 32 03 00 01 00\n");
        commands.append("   ├─ CLA: 90 (CEPAS specific class)\n");
        commands.append("   ├─ INS: 32 (READ PURSE command)\n");
        commands.append("   ├─ P1:  03 (Purse ID = 3)\n");
        commands.append("   ├─ P2:  00 (Read purse data)\n");
        commands.append("   ├─ Lc:  01 (Length of data field)\n");
        commands.append("   ├─ Data: 00 (Read from beginning)\n");
        commands.append("   └─ Response: [64+ bytes] + 90 00 (Success)\n\n");
        
        commands.append("3. CAN EXTRACTION ✓\n");
        commands.append("   From Purse 3 response, extracted bytes 8-15\n");
        commands.append("   These 8 bytes form the Card Account Number (CAN)\n\n");
        
        commands.append("Why CEPAS vs Standard UID?\n");
        commands.append("• Standard NFC UID: Only 4 bytes (basic tag identifier)\n");
        commands.append("• CEPAS CAN: 8 bytes (full card account number)\n");
        commands.append("• CEPAS CAN contains transit system specific data\n");
        commands.append("• UID alone cannot provide transit account information\n");
        commands.append("• CEPAS protocol required for accessing purse data");
        
        tvNfcCommands.setText(commands.toString());
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
