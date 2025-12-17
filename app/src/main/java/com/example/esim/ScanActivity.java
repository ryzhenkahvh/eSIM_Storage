package com.example.esim;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            startScan();
        }
    }

    private void startScan() {
//        IntentIntegrator integrator = new IntentIntegrator(this);
//        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
//        integrator.setPrompt("Scan eSIM QR Code");
//        integrator.setCameraId(0);
//        integrator.setOrientationLocked(true);
//        integrator.setBeepEnabled(true);
//        integrator.setBarcodeImageEnabled(true);
//        integrator.initiateScan();
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CustomScannerActivity.class); // <-- ВАЖНО
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setOrientationLocked(true); // Блокировка
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScan();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                finish();
            } else {
                String scannedData = result.getContents();
                parseAndSaveEsimData(scannedData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void parseAndSaveEsimData(String data) {
        // Assume format: LPA:1$activation_code$matching_id
        if (data.startsWith("LPA:")) {
            String[] parts = data.split("\\$");
            if (parts.length >= 3) {
                String activationCode = parts[1];
                String matchingId = parts[2];
                // For simplicity, use default name
                String name = "eSIM " + System.currentTimeMillis();
                saveProfile(name, activationCode, matchingId);
            } else {
                Toast.makeText(this, "Invalid eSIM QR code format", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Not a valid eSIM QR code", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void saveProfile(String name, String activationCode, String matchingId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                EsimProfile profile = new EsimProfile(name, activationCode, matchingId);
                db.esimProfileDao().insert(profile);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(ScanActivity.this, "eSIM profile saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        }.execute();
    }

}