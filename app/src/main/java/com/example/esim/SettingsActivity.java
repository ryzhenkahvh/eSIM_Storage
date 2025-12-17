package com.example.esim;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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
    }

    private void wipeDatabase() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                AppDatabase.getDatabase(getApplicationContext()).clearAllTables();
                return null;
            }
            @Override
            protected void onPostExecute(Void unused) {
                Toast.makeText(SettingsActivity.this, "All data erased", Toast.LENGTH_SHORT).show();
                finish();
            }
        }.execute();
    }
}