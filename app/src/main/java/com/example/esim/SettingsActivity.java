package com.example.esim;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Импорт
        findViewById(R.id.card_import_system).setOnClickListener(v -> {
            startActivity(new Intent(this, SystemEsimActivity.class));
        });

        // Очистка
        findViewById(R.id.card_wipe_data).setOnClickListener(v -> {
            wipeDatabase();
        });

        // Выход
        findViewById(R.id.card_logout).setOnClickListener(v -> {
            logout();
        });
    }

    private void wipeDatabase() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId).collection("profiles").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        WriteBatch batch = db.batch();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            batch.delete(document.getReference());
                        }
                        batch.commit().addOnCompleteListener(batchTask -> {
                            if (batchTask.isSuccessful()) {
                                Toast.makeText(SettingsActivity.this, "All data erased", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(SettingsActivity.this, "Failed to erase data", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(SettingsActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}