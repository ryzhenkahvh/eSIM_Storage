package com.example.esim;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.esim.adapters.EsimAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EsimAdapter adapter;
    private TextView tvUsage;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        userId = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // 1. Инициализация UI элементов
        recyclerView = findViewById(R.id.recycler_view);
        tvUsage = findViewById(R.id.tv_usage);

        // Кнопки
        ImageView btnSettings = findViewById(R.id.btn_settings);
        ExtendedFloatingActionButton fabAdd = findViewById(R.id.fab_add);
        MaterialButton btnSync = findViewById(R.id.btn_sync_nfc);

        // 2. Настройка списка (RecyclerView)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // При клике на элемент списка открываем ShowQRActivity
        adapter = new EsimAdapter(new ArrayList<>(), profile -> {
            Intent intent = new Intent(MainActivity.this, ShowQRActivity.class);
            intent.putExtra("profile_id", profile.id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // 3. ЛОГИКА КНОПКИ НАСТРОЕК
        // Переход в экран настроек (Импорт/Удаление)
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // 4. ЛОГИКА СКАНЕРА (Запуск нашего CustomScannerActivity)
        fabAdd.setOnClickListener(v -> startScan());

        // 5. ЛОГИКА NFC (Пока заглушка для демо)
        btnSync.setOnClickListener(v -> {
            Toast.makeText(this, "Searching for NFC Tag...", Toast.LENGTH_SHORT).show();
            // В будущем здесь будет код для чтения/записи ST25TV02K
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем список профилей каждый раз, когда возвращаемся на этот экран
        // (например, после сканирования, удаления или выхода из настроек)
        loadProfiles();
    }

    /**
     * Запускает сканер QR-кодов с использованием нашего кастомного дизайна
     */
    private void startScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setCaptureActivity(CustomScannerActivity.class); // <-- ВАЖНО: Наш красивый сканер
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setOrientationLocked(true); // Блокируем поворот
        integrator.setBeepEnabled(true);
        integrator.setPrompt(""); // Убираем стандартный текст, так как у нас свой UI
        integrator.initiateScan();
    }

    /**
     * Получает результат сканирования от CustomScannerActivity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // Если скан успешен, обрабатываем строку
                String scannedData = result.getContents();
                parseAndSaveEsimData(scannedData);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Разбирает строку LPA и сохраняет в БД
     */
    private void parseAndSaveEsimData(String data) {
        // Формат: LPA:1$SMDP_ADDRESS$ACTIVATION_CODE
        if (data != null && data.startsWith("LPA:")) {
            String[] parts = data.split("\\$");

            // Защита от выхода за границы массива
            String activationCode = (parts.length > 1) ? parts[1] : "Unknown Code";
            String matchingId = (parts.length > 2) ? parts[2] : "";

            // Генерируем имя (в реальном проекте можно спрашивать пользователя)
            String name = "New eSIM " + (System.currentTimeMillis() % 1000);

            saveProfile(name, activationCode, matchingId);
        } else {
            Toast.makeText(this, "Invalid eSIM QR code format", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Сохранение в Firestore
     */
    private void saveProfile(String name, String activationCode, String matchingId) {
        EsimProfile profile = new EsimProfile(name, activationCode, matchingId);
        db.collection("users").document(userId).collection("profiles").add(profile.toMap())
                .addOnSuccessListener(documentReference -> {
                    profile.id = documentReference.getId();
                    Toast.makeText(MainActivity.this, "eSIM Profile Saved!", Toast.LENGTH_SHORT).show();
                    loadProfiles();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Загрузка профилей из Firestore
     */
    private void loadProfiles() {
        db.collection("users").document(userId).collection("profiles").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<EsimProfile> profiles = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            EsimProfile profile = EsimProfile.fromMap(document.getId(), document.getData());
                            profiles.add(profile);
                        }
                        if (adapter != null) {
                            adapter.updateData(profiles);
                        }
                        int count = profiles.size();
                        tvUsage.setText(count + " / 15 Profiles Stored");
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to load profiles", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}