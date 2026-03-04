package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupEdgeToEdge(findViewById(R.id.settings_root));

        findViewById(R.id.btnLogoutSettings).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.tvPrivacyPolicy).setOnClickListener(v -> 
            Toast.makeText(this, "Opening Privacy Policy...", Toast.LENGTH_SHORT).show());

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }
}
