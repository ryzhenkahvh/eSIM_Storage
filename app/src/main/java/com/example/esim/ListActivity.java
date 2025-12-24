package com.example.esim;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    private ListView listView;
    private List<EsimProfile> profiles;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        listView = findViewById(R.id.list_view);

        loadProfiles();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                EsimProfile profile = profiles.get(position);
                Intent intent = new Intent(ListActivity.this, ShowQRActivity.class);
                intent.putExtra("profile_id", profile.id);
                startActivity(intent);
            }
        });
    }

    private void loadProfiles() {
        db.collection("users").document(userId).collection("profiles").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        profiles = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            EsimProfile profile = EsimProfile.fromMap(document.getId(), document.getData());
                            profiles.add(profile);
                        }
                        ArrayAdapter<EsimProfile> adapter = new ArrayAdapter<EsimProfile>(ListActivity.this, R.layout.list_item, R.id.tv_name, profiles) {
                            @Override
                            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                EsimProfile profile = getItem(position);
                                android.widget.TextView tvName = view.findViewById(R.id.tv_name);
                                android.widget.TextView tvCode = view.findViewById(R.id.tv_activation_code);
                                tvName.setText(profile.name != null ? profile.name : "Unnamed eSIM");
                                tvCode.setText("Activation Code: " + profile.activationCode);
                                return view;
                            }
                        };
                        listView.setAdapter(adapter);
                    }
                });
    }
}