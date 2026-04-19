package com.example.capstonex;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TopicApprovalActivity extends BaseActivity {

    private LinearLayout step1Container, step2Container, step3Container;
    private TextView tvToolbarTitle;
    
    // Step 1 Views
    private AutoCompleteTextView actDomain1;
    private View etTitle1, etDesc1;

    // Step 2 Views
    private AutoCompleteTextView actDomain2;
    private View etTitle2, etDesc2;

    // Step 3 Views
    private AutoCompleteTextView actDomain3;
    private View etTitle3, etDesc3;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_approval);
        setupEdgeToEdge(findViewById(R.id.topic_approval_root));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize Containers
        step1Container = findViewById(R.id.step1Container);
        step2Container = findViewById(R.id.step2Container);
        step3Container = findViewById(R.id.step3Container);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);

        // Initialize Input Views (Step 1)
        actDomain1 = findViewById(R.id.actDomain1);
        etTitle1 = findViewById(R.id.etTitle1);
        etDesc1 = findViewById(R.id.etDesc1);

        // Initialize Input Views (Step 2)
        actDomain2 = findViewById(R.id.actDomain2);
        etTitle2 = findViewById(R.id.etTitle2);
        etDesc2 = findViewById(R.id.etDesc2);

        // Initialize Input Views (Step 3)
        actDomain3 = findViewById(R.id.actDomain3);
        etTitle3 = findViewById(R.id.etTitle3);
        etDesc3 = findViewById(R.id.etDesc3);

        setupDomainDropdowns();

        // Step 1 Actions
        findViewById(R.id.btnNext1).setOnClickListener(v -> showStep(2));

        // Step 2 Actions
        findViewById(R.id.btnNext2).setOnClickListener(v -> showStep(3));
        findViewById(R.id.btnBack2).setOnClickListener(v -> showStep(1));

        // Step 3 Actions
        findViewById(R.id.btnBack3).setOnClickListener(v -> showStep(2));
        findViewById(R.id.btnSubmitApproval).setOnClickListener(v -> submitAllTopics());

        findViewById(R.id.toolbar).setOnClickListener(v -> {
            if (step1Container.getVisibility() == View.VISIBLE) {
                finish();
            } else if (step2Container.getVisibility() == View.VISIBLE) {
                showStep(1);
            } else {
                showStep(2);
            }
        });
    }

    private void setupDomainDropdowns() {
        String[] domains = getResources().getStringArray(R.array.domains);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, domains);
        actDomain1.setAdapter(adapter);
        actDomain2.setAdapter(adapter);
        actDomain3.setAdapter(adapter);
    }

    private void showStep(int step) {
        step1Container.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2Container.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3Container.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        
        tvToolbarTitle.setText("Topic Approval (Step " + step + "/3)");
    }

    private void submitAllTopics() {
        // Collect all data and submit to Firestore
        String uid = mAuth.getUid();
        if (uid == null) return;

        Map<String, Object> submission = new HashMap<>();
        submission.put("studentUid", uid);
        submission.put("timestamp", com.google.firebase.Timestamp.now());
        submission.put("status", "Pending Approval");

        // Topic 1
        Map<String, String> topic1 = new HashMap<>();
        topic1.put("title", ((TextView)etTitle1).getText().toString());
        topic1.put("domain", actDomain1.getText().toString());
        topic1.put("description", ((TextView)etDesc1).getText().toString());
        submission.put("topic1", topic1);

        // Topic 2
        Map<String, String> topic2 = new HashMap<>();
        topic2.put("title", ((TextView)etTitle2).getText().toString());
        topic2.put("domain", actDomain2.getText().toString());
        topic2.put("description", ((TextView)etDesc2).getText().toString());
        submission.put("topic2", topic2);

        // Topic 3
        Map<String, String> topic3 = new HashMap<>();
        topic3.put("title", ((TextView)etTitle3).getText().toString());
        topic3.put("domain", actDomain3.getText().toString());
        topic3.put("description", ((TextView)etDesc3).getText().toString());
        submission.put("topic3", topic3);

        db.collection("topic_submissions").document(uid).set(submission)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "All 3 topics submitted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Submission failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
