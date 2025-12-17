package com.example.esim;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;

public class SystemEsimActivity extends AppCompatActivity {

    private ListView listView;
    private List<SubscriptionInfo> esimList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list); // Используем существующий layout списка

        listView = findViewById(R.id.list_view);

        // Проверка прав
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 101);
        } else {
            loadSystemEsims();
        }
    }

    private void loadSystemEsims() {
        SubscriptionManager sm = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
            esimList = new ArrayList<>();
            List<String> displayNames = new ArrayList<>();

            if (subs != null) {
                for (SubscriptionInfo sub : subs) {
                    // isEmbedded() true только для eSIM
                    if (sub.isEmbedded()) {
                        esimList.add(sub);
                        displayNames.add(sub.getDisplayName() + " (Carrier: " + sub.getCarrierName() + ")");
                    }
                }
            }

            if (esimList.isEmpty()) {
                Toast.makeText(this, "No active eSIMs found in system", Toast.LENGTH_LONG).show();
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayNames);
            listView.setAdapter(adapter);

            // При клике - добавляем в нашу БД как "справочную" запись
            listView.setOnItemClickListener((parent, view, position, id) -> {
                SubscriptionInfo info = esimList.get(position);
                saveReferenceProfile(info);
            });
        }
    }

    private void saveReferenceProfile(SubscriptionInfo info) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                // Мы не можем получить QR код, поэтому пишем заглушку
                EsimProfile profile = new EsimProfile(
                        info.getDisplayName().toString(),
                        "INSTALLED_IN_SYSTEM_ID_" + info.getSubscriptionId(),
                        "No QR Available"
                );
                db.esimProfileDao().insert(profile);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                Toast.makeText(SystemEsimActivity.this, "Imported to local DB", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSystemEsims();
        }
    }
}