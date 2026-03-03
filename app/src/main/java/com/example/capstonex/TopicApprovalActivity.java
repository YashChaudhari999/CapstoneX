package com.example.capstonex;

import android.os.Bundle;

public class TopicApprovalActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_approval);
        applySystemWindowInsets(findViewById(android.R.id.content));
    }
}
