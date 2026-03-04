package com.example.capstonex;

import android.os.Bundle;

public class ReviewEvaluationActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_evaluation);
        setupEdgeToEdge(findViewById(android.R.id.content));
    }
}
