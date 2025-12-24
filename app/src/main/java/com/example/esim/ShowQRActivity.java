package com.example.esim;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ShowQRActivity extends AppCompatActivity {

    private ImageView ivQrCode;
    private TextView tvTitle, tvRawCode;
    private EsimProfile currentProfile;
    private String profileId;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_qr);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // Привязка UI
        ivQrCode = findViewById(R.id.iv_qr_code);
        tvTitle = findViewById(R.id.tv_qr_title);
        tvRawCode = findViewById(R.id.tv_raw_code);
        MaterialButton btnDelete = findViewById(R.id.btn_delete_profile);
        MaterialButton btnSystemInstall = findViewById(R.id.btn_activate_system);

        // Получение ID
        profileId = getIntent().getStringExtra("profile_id");
        if (profileId == null) {
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Загрузка данных
        loadProfile();

        // Логика удаления
        btnDelete.setOnClickListener(v -> deleteProfile());

        // Логика "Глубокой интеграции"
        btnSystemInstall.setOnClickListener(v -> {
            if (currentProfile != null) {
                installProfileToSystem(currentProfile.activationCode);
            }
        });
    }

    private void loadProfile() {
        db.collection("users").document(userId).collection("profiles").document(profileId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentProfile = EsimProfile.fromMap(documentSnapshot.getId(), documentSnapshot.getData());
                        tvTitle.setText(currentProfile.name);
                        tvRawCode.setText(currentProfile.activationCode);
                        String qrData = "LPA:1$" + currentProfile.activationCode + "$" + (currentProfile.matchingId != null ? currentProfile.matchingId : "");
                        generateQRCode(qrData);
                    } else {
                        Toast.makeText(ShowQRActivity.this, "Profile not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ShowQRActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void generateQRCode(String data) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            // Защита от пустых данных, которые крашат приложение
            if (data == null || data.isEmpty()) return;

            BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            ivQrCode.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteProfile() {
        db.collection("users").document(userId).collection("profiles").document(profileId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ShowQRActivity.this, "Profile deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ShowQRActivity.this, "Failed to delete", Toast.LENGTH_SHORT).show();
                });
    }

    // РЕАЛИЗАЦИЯ ИНТЕГРАЦИИ С ANDROID
    private void installProfileToSystem(String activationCode) {
        // 1. Копируем код в буфер обмена (для удобства пользователя)
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("eSIM Code", activationCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Code copied! Opening Settings...", Toast.LENGTH_LONG).show();

        // 2. Пытаемся открыть системные настройки eSIM
        // Это максимально глубокая интеграция, доступная без root/carrier прав
        try {
            Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            // Если специфичное меню не открывается, открываем общие настройки роуминга/сети
            try {
                Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "Please open Settings -> Network -> SIMs manually", Toast.LENGTH_LONG).show();
            }
        }
    }
}