package com.example.capstonex;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
            if (validateStep(1)) {
                showStep(2);
            }
        });

        btnNext2.setOnClickListener(v -> {
            if (validateStep(2)) {
                showStep(3);
            }
        });

        btnBack2.setOnClickListener(v -> showStep(1));
        btnBack3.setOnClickListener(v -> showStep(2));

        btnSubmitApproval.setOnClickListener(v -> {
            if (validateStep(3)) {
                submitTopics();
            }
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
                    if (domain != null && domain.getName() != null) {
                        domainNames.add(domain.getName());
                    }
                }
                
                if (domainNames.isEmpty()) {
                    // Fallback to resources if DB is empty or use a placeholder
                    domainNames.add("No Domains Available");
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(TopicApprovalActivity.this, 
                        android.R.layout.simple_list_item_1, domainNames);
                
                actDomain1.setAdapter(adapter);
                actDomain2.setAdapter(adapter);
                actDomain3.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TopicApprovalActivity.this, "Failed to load domains", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateStep(int step) {
        TextInputEditText etTitle, etDesc;
        AutoCompleteTextView actDomain;
        
        if (step == 1) {
            etTitle = etTitle1;
            etDesc = etDesc1;
            actDomain = actDomain1;
        } else if (step == 2) {
            etTitle = etTitle2;
            etDesc = etDesc2;
            actDomain = actDomain2;
        } else {
            etTitle = etTitle3;
            etDesc = etDesc3;
            actDomain = actDomain3;
        }

        if (etTitle.getText().toString().trim().isEmpty()) {
            etTitle.setError("Required");
            return false;
        }
        if (actDomain.getText().toString().trim().isEmpty()) {
            actDomain.setError("Required");
            return false;
        }
        if (etDesc.getText().toString().trim().isEmpty()) {
            etDesc.setError("Required");
            return false;
        }
        return true;
    }

    private void fetchUserGroupId() {
        String uid = mAuth.getUid();
        if (uid == null) return;
        mDatabase.child("Users").child(uid).child("groupId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) userGroupId = snapshot.getValue(String.class);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void submitTopics() {
        if (userGroupId == null || userGroupId.isEmpty()) {
            Toast.makeText(this, "Group allocation not found.", Toast.LENGTH_LONG).show();
            return;
        }

        btnSubmitApproval.setEnabled(false);
        btnSubmitApproval.setText("Submitting...");

        Map<String, Object> topics = new HashMap<>();
        
        Map<String, String> t1 = new HashMap<>();
        t1.put("title", etTitle1.getText().toString().trim());
        t1.put("domain", actDomain1.getText().toString().trim());
        t1.put("description", etDesc1.getText().toString().trim());
        t1.put("status", "Pending");
        
        Map<String, String> t2 = new HashMap<>();
        t2.put("title", etTitle2.getText().toString().trim());
        t2.put("domain", actDomain2.getText().toString().trim());
        t2.put("description", etDesc2.getText().toString().trim());
        t2.put("status", "Pending");

        Map<String, String> t3 = new HashMap<>();
        t3.put("title", etTitle3.getText().toString().trim());
        t3.put("domain", actDomain3.getText().toString().trim());
        t3.put("description", etDesc3.getText().toString().trim());
        t3.put("status", "Pending");

        topics.put("topic1", t1);
        topics.put("topic2", t2);
        topics.put("topic3", t3);
        topics.put("submittedBy", mAuth.getUid());
        topics.put("timestamp", System.currentTimeMillis());

        mDatabase.child("TopicApprovals").child(userGroupId).setValue(topics)
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
}
