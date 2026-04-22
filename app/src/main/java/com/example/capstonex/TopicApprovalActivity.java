package com.example.capstonex;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicApprovalActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";

    private View step1Container, step2Container, step3Container;
    private TextView tvToolbarTitle;
    private TextInputEditText etTitle1, etDesc1, etTitle2, etDesc2, etTitle3, etDesc3;
    private AutoCompleteTextView actDomain1, actDomain2, actDomain3;
    private MaterialButton btnNext1, btnNext2, btnBack2, btnBack3, btnSubmitApproval;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userGroupId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_approval);
        setupEdgeToEdge(findViewById(R.id.topic_approval_root));

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();

        initViews();
        setupStepNavigation();
        setupDomainDropdowns();

        // ── BUG-009 FIX: Disable submit until groupId is confirmed async ──────
        btnSubmitApproval.setEnabled(false);
        btnSubmitApproval.setAlpha(0.5f);
        fetchUserGroupId();

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void initViews() {
        step1Container = findViewById(R.id.step1Container);
        step2Container = findViewById(R.id.step2Container);
        step3Container = findViewById(R.id.step3Container);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);

        etTitle1 = findViewById(R.id.etTitle1);
        etDesc1 = findViewById(R.id.etDesc1);
        actDomain1 = findViewById(R.id.actDomain1);

        etTitle2 = findViewById(R.id.etTitle2);
        etDesc2 = findViewById(R.id.etDesc2);
        actDomain2 = findViewById(R.id.actDomain2);

        etTitle3 = findViewById(R.id.etTitle3);
        etDesc3 = findViewById(R.id.etDesc3);
        actDomain3 = findViewById(R.id.actDomain3);

        btnNext1 = findViewById(R.id.btnNext1);
        btnNext2 = findViewById(R.id.btnNext2);
        btnBack2 = findViewById(R.id.btnBack2);
        btnBack3 = findViewById(R.id.btnBack3);
        btnSubmitApproval = findViewById(R.id.btnSubmitApproval);
    }

    private void setupStepNavigation() {
        btnNext1.setOnClickListener(v -> {
            if (validateStep(1)) showStep(2);
        });

        btnNext2.setOnClickListener(v -> {
            if (validateStep(2)) showStep(3);
        });

        btnBack2.setOnClickListener(v -> showStep(1));
        btnBack3.setOnClickListener(v -> showStep(2));

        btnSubmitApproval.setOnClickListener(v -> {
            if (validateStep(3)) submitTopics();
        });
        
        showStep(1);
    }

    private void showStep(int step) {
        step1Container.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2Container.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3Container.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        tvToolbarTitle.setText("Topic Approval (Step " + step + "/3)");
    }

    private void setupDomainDropdowns() {
        mDatabase.child("Domains").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> domainNames = new ArrayList<>();
                for (DataSnapshot domainSnap : snapshot.getChildren()) {
                    DomainModel domain = domainSnap.getValue(DomainModel.class);
                    if (domain != null && domain.getName() != null) domainNames.add(domain.getName());
                }
                
                if (domainNames.isEmpty()) domainNames.add("No Domains Available");

                ArrayAdapter<String> adapter = new ArrayAdapter<>(TopicApprovalActivity.this, 
                        android.R.layout.simple_list_item_1, domainNames);
                
                actDomain1.setAdapter(adapter);
                actDomain2.setAdapter(adapter);
                actDomain3.setAdapter(adapter);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private boolean validateStep(int step) {
        TextInputEditText etTitle, etDesc;
        AutoCompleteTextView actDomain;
        
        if (step == 1) { etTitle = etTitle1; etDesc = etDesc1; actDomain = actDomain1; }
        else if (step == 2) { etTitle = etTitle2; etDesc = etDesc2; actDomain = actDomain2; }
        else { etTitle = etTitle3; etDesc = etDesc3; actDomain = actDomain3; }

        if (etTitle.getText().toString().trim().isEmpty()) { etTitle.setError("Required"); return false; }
        if (actDomain.getText().toString().trim().isEmpty()) { actDomain.setError("Required"); return false; }
        if (etDesc.getText().toString().trim().isEmpty()) { etDesc.setError("Required"); return false; }
        return true;
    }

    private void fetchUserGroupId() {
        String uid = mAuth.getUid();
        if (uid == null) {
            showGroupError();
            return;
        }
        mDatabase.child("Users").child(uid).child("groupId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String gid = snapshot.getValue(String.class);
                    if (gid != null && !gid.isEmpty()) {
                        userGroupId = gid;
                        // ── BUG-009 FIX: enable submit now that groupId is confirmed ──
                        btnSubmitApproval.setEnabled(true);
                        btnSubmitApproval.setAlpha(1.0f);
                        return;
                    }
                }
                showGroupError();
            }
            // ── BUG-009 FIX: was completely silent ──
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TopicApprovalActivity.this,
                        "Network error — could not verify group. Check your connection.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /** Shows error and keeps Submit disabled if group not found. */
    private void showGroupError() {
        btnSubmitApproval.setEnabled(false);
        btnSubmitApproval.setAlpha(0.5f);
        Toast.makeText(this,
                "You are not in a group. Contact your admin.",
                Toast.LENGTH_LONG).show();
    }

    private void submitTopics() {
        if (userGroupId == null || userGroupId.isEmpty()) {
            Toast.makeText(this, "Group allocation not found.", Toast.LENGTH_LONG).show();
            return;
        }

        btnSubmitApproval.setEnabled(false);
        btnSubmitApproval.setText("Submitting...");

        Map<String, Object> submittedTopics = new HashMap<>();
        
        submittedTopics.put("topic1", createTopicMap(etTitle1, actDomain1, etDesc1));
        submittedTopics.put("topic2", createTopicMap(etTitle2, actDomain2, etDesc2));
        submittedTopics.put("topic3", createTopicMap(etTitle3, actDomain3, etDesc3));

        Map<String, Object> finalData = new HashMap<>();
        finalData.put("submittedTopics", submittedTopics);
        finalData.put("submittedBy", mAuth.getUid());
        finalData.put("timestamp", System.currentTimeMillis());

        mDatabase.child("TopicApprovals").child(userGroupId).setValue(finalData)
                .addOnSuccessListener(aVoid -> {
                    mDatabase.child("Groups").child(userGroupId).child("status").setValue("Topic Submitted");
                    Toast.makeText(TopicApprovalActivity.this, "Topics submitted successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitApproval.setEnabled(true);
                    btnSubmitApproval.setText("SUBMIT ALL");
                    Toast.makeText(TopicApprovalActivity.this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private Map<String, String> createTopicMap(TextInputEditText title, AutoCompleteTextView domain, TextInputEditText desc) {
        Map<String, String> map = new HashMap<>();
        map.put("title", title.getText().toString().trim());
        map.put("domain", domain.getText().toString().trim());
        map.put("description", desc.getText().toString().trim());
        map.put("status", "Pending");
        return map;
    }
}
