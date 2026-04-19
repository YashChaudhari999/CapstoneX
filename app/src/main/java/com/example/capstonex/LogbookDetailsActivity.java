package com.example.capstonex;

import android.os.Bundle;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

public class LogbookDetailsActivity extends BaseActivity {

    private TextView tvWeek, tvStatus, tvDuration, tvScore;
    private TextView tvWorkDesc, tvTech, tvMetrics, tvLearning, tvIssues, tvFeedback, tvNextSteps;
    private MaterialButton btnDownload, btnEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logbook_details);
        setupEdgeToEdge(findViewById(R.id.logbook_details_root));

        // Header views
        tvWeek = findViewById(R.id.tvDetailWeek);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvDuration = findViewById(R.id.tvDetailDuration);
        tvScore = findViewById(R.id.tvDetailScore);

        // Content views
        tvWorkDesc = findViewById(R.id.tvDetailWorkDesc);
        tvTech = findViewById(R.id.tvDetailTech);
        tvMetrics = findViewById(R.id.tvDetailMetrics);
        tvLearning = findViewById(R.id.tvDetailLearning);
        tvIssues = findViewById(R.id.tvDetailIssues);
        tvFeedback = findViewById(R.id.tvDetailFeedback);
        tvNextSteps = findViewById(R.id.tvDetailNextSteps);

        // Buttons
        btnDownload = findViewById(R.id.btnDownloadReport);
        btnEdit = findViewById(R.id.btnEditLog);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        loadLogDetails();
    }

    private void loadLogDetails() {
        // Logic to fetch full details from Intent extras or Firestore
        // Placeholder data logic
    }
}
