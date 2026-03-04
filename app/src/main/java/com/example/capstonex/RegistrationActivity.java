package com.example.capstonex;

import android.os.Bundle;

public class RegistrationActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        setupEdgeToEdge(findViewById(android.R.id.content));
    }
}
