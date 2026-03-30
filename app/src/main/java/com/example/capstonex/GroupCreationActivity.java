package com.example.capstonex;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupCreationActivity extends BaseActivity {

    private TextInputEditText etGroupName, etLeaderSap, etMember2Sap, etMember3Sap, etMember4Sap;
    private TextInputLayout tilMember2, tilMember3, tilMember4;
    private MaterialButton btnAddMember, btnCreateGroup;
    private FirebaseFirestore db;
    private int memberCount = 1; // Starting with only leader

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_creation);
        setupEdgeToEdge(findViewById(R.id.group_creation_root));

        db = FirebaseFirestore.getInstance();

        // Initialize views
        etGroupName = findViewById(R.id.etGroupName);
        etLeaderSap = findViewById(R.id.etLeaderSap);
        etMember2Sap = findViewById(R.id.etMember2Sap);
        etMember3Sap = findViewById(R.id.etMember3Sap);
        etMember4Sap = findViewById(R.id.etMember4Sap);

        tilMember2 = findViewById(R.id.tilMember2);
        tilMember3 = findViewById(R.id.tilMember3);
        tilMember4 = findViewById(R.id.tilMember4);

        btnAddMember = findViewById(R.id.btnAddMember);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);

        btnAddMember.setOnClickListener(v -> addMemberField());
        btnCreateGroup.setOnClickListener(v -> createGroup());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void addMemberField() {
        if (memberCount == 1) {
            tilMember2.setVisibility(View.VISIBLE);
            memberCount++;
        } else if (memberCount == 2) {
            tilMember3.setVisibility(View.VISIBLE);
            memberCount++;
        } else if (memberCount == 3) {
            tilMember4.setVisibility(View.VISIBLE);
            memberCount++;
            btnAddMember.setVisibility(View.GONE); // Max 4 members reached
        }
    }

    private void createGroup() {
        String name = etGroupName.getText().toString().trim();
        String leaderSap = etLeaderSap.getText().toString().trim();

        if (name.isEmpty() || leaderSap.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> members = new ArrayList<>();
        members.add(leaderSap);

        if (tilMember2.getVisibility() == View.VISIBLE) {
            String m2 = etMember2Sap.getText().toString().trim();
            if (!m2.isEmpty()) members.add(m2);
        }
        if (tilMember3.getVisibility() == View.VISIBLE) {
            String m3 = etMember3Sap.getText().toString().trim();
            if (!m3.isEmpty()) members.add(m3);
        }
        if (tilMember4.getVisibility() == View.VISIBLE) {
            String m4 = etMember4Sap.getText().toString().trim();
            if (!m4.isEmpty()) members.add(m4);
        }

        Map<String, Object> group = new HashMap<>();
        group.put("name", name);
        group.put("members", members);
        group.put("createdAt", com.google.firebase.Timestamp.now());
        group.put("status", "pending"); // Default status

        db.collection("groups").add(group)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Group Created Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
