package com.itachi1706.cheesecakecepasreadersample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.itachi1706.cepaslib.app.feature.main.MainActivity;

public class RedirectAcrtivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_redirect);
        
        // Initialize buttons
        android.widget.Button btnMainApp = findViewById(R.id.btnMainApp);
        android.widget.Button btnCANScanner = findViewById(R.id.btnCANScanner);
        
        // Set up button click listeners
        btnMainApp.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
        
        btnCANScanner.setOnClickListener(v -> {
            Intent intent = new Intent(this, CANScannerActivity.class);
            startActivity(intent);
        });
    }
}
