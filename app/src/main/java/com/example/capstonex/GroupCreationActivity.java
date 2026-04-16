package com.example.capstonex;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupCreationActivity extends BaseActivity {

    private TextInputEditText etLeaderSap, etMember2Sap, etMember3Sap, etMember4Sap;
    private TextInputLayout tilMember2, tilMember3, tilMember4;
    private MaterialButton btnAddMember, btnRemoveMember, btnCreateGroup;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private int memberCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_creation);
        setupEdgeToEdge(findViewById(R.id.group_creation_root));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        etLeaderSap = findViewById(R.id.etLeaderSap);
        etMember2Sap = findViewById(R.id.etMember2Sap);
        etMember3Sap = findViewById(R.id.etMember3Sap);
        etMember4Sap = findViewById(R.id.etMember4Sap);

        tilMember2 = findViewById(R.id.tilMember2);
        tilMember3 = findViewById(R.id.tilMember3);
        tilMember4 = findViewById(R.id.tilMember4);

        btnAddMember = findViewById(R.id.btnAddMember);
        btnRemoveMember = findViewById(R.id.btnRemoveMember);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);

        btnAddMember.setOnClickListener(v -> addMemberField());
        btnRemoveMember.setOnClickListener(v -> removeMemberField());
        btnCreateGroup.setOnClickListener(v -> validateAndCreateGroup());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void addMemberField() {
        if (memberCount == 1) {
            tilMember2.setVisibility(View.VISIBLE);
            btnRemoveMember.setVisibility(View.VISIBLE);
            memberCount++;
        } else if (memberCount == 2) {
            tilMember3.setVisibility(View.VISIBLE);
            memberCount++;
        } else if (memberCount == 3) {
            tilMember4.setVisibility(View.VISIBLE);
            memberCount++;
            btnAddMember.setVisibility(View.GONE);
        }
    }

    private void removeMemberField() {
        if (memberCount == 4) {
            tilMember4.setVisibility(View.GONE);
            etMember4Sap.setText("");
            btnAddMember.setVisibility(View.VISIBLE);
            memberCount--;
        } else if (memberCount == 3) {
            tilMember3.setVisibility(View.GONE);
            etMember3Sap.setText("");
            memberCount--;
        } else if (memberCount == 2) {
            tilMember2.setVisibility(View.GONE);
            etMember2Sap.setText("");
            btnRemoveMember.setVisibility(View.GONE);
            memberCount--;
        }
    }

    private void validateAndCreateGroup() {
        String leaderSap = etLeaderSap.getText().toString().trim();
        if (leaderSap.isEmpty()) {
            etLeaderSap.setError("Leader SAP ID required");
            return;
        }

        List<String> memberList = new ArrayList<>();
        memberList.add(leaderSap);

        if (tilMember2.getVisibility() == View.VISIBLE) {
            String val = etMember2Sap.getText().toString().trim();
            if (val.isEmpty()) { etMember2Sap.setError("Required"); return; }
            memberList.add(val);
        }
        if (tilMember3.getVisibility() == View.VISIBLE) {
            String val = etMember3Sap.getText().toString().trim();
            if (val.isEmpty()) { etMember3Sap.setError("Required"); return; }
            memberList.add(val);
        }
        if (tilMember4.getVisibility() == View.VISIBLE) {
            String val = etMember4Sap.getText().toString().trim();
            if (val.isEmpty()) { etMember4Sap.setError("Required"); return; }
            memberList.add(val);
        }

        // Check for duplicate SAP IDs
        Set<String> uniqueCheck = new HashSet<>(memberList);
        if (uniqueCheck.size() < memberList.size()) {
            Toast.makeText(this, "Duplicate SAP IDs are not allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        createGroupInFirestore(memberList);
    }

    private void createGroupInFirestore(List<String> members) {
        btnCreateGroup.setEnabled(false);
        btnCreateGroup.setText("Creating...");

        String currentUid = mAuth.getUid();
        Map<String, Object> group = new HashMap<>();
        group.put("leaderUid", currentUid);
        group.put("members", members);
        group.put("createdAt", Timestamp.now());
        group.put("status", "Pending Title Approval");

        // Use a WriteBatch to ensure both the group is created and user is updated
        db.collection("groups").add(group)
                .addOnSuccessListener(ref -> {
                    String groupId = ref.getId();

                    // Update user's group status
                    db.collection("users").document(currentUid)
                            .update("hasGroup", true, "groupId", groupId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Group Created! Proceed to Title Approval.", Toast.LENGTH_LONG).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    btnCreateGroup.setEnabled(true);
                    btnCreateGroup.setText("CREATE GROUP");
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}