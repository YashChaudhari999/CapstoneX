package com.example.capstonex;

import android.os.Bundle;

public class TopicApprovalActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_approval);
        setupEdgeToEdge(findViewById(android.R.id.content));

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }
}
