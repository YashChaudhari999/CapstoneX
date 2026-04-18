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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupCreationActivity extends BaseActivity {

    private TextInputEditText etLeaderSap, etLeaderName, etLeaderRoll;
    private TextInputEditText etMember2Sap, etMember2Name, etMember2Roll;
    private TextInputEditText etMember3Sap, etMember3Name, etMember3Roll;
    private TextInputEditText etMember4Sap, etMember4Name, etMember4Roll;
    
    private View tilMember2, tilMember3, tilMember4;
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

        // Initialize leader views
        etLeaderSap = findViewById(R.id.etLeaderSap);
        etLeaderName = findViewById(R.id.etLeaderName);
        etLeaderRoll = findViewById(R.id.etLeaderRoll);

        // Initialize member views
        etMember2Sap = findViewById(R.id.etMember2Sap);
        etMember2Name = findViewById(R.id.etMember2Name);
        etMember2Roll = findViewById(R.id.etMember2Roll);
        tilMember2 = findViewById(R.id.tilMember2);

        etMember3Sap = findViewById(R.id.etMember3Sap);
        etMember3Name = findViewById(R.id.etMember3Name);
        etMember3Roll = findViewById(R.id.etMember3Roll);
        tilMember3 = findViewById(R.id.tilMember3);

        etMember4Sap = findViewById(R.id.etMember4Sap);
        etMember4Name = findViewById(R.id.etMember4Name);
        etMember4Roll = findViewById(R.id.etMember4Roll);
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
            clearMemberFields(etMember4Sap, etMember4Name, etMember4Roll);
            btnAddMember.setVisibility(View.VISIBLE);
            memberCount--;
        } else if (memberCount == 3) {
            tilMember3.setVisibility(View.GONE);
            clearMemberFields(etMember3Sap, etMember3Name, etMember3Roll);
            memberCount--;
        } else if (memberCount == 2) {
            tilMember2.setVisibility(View.GONE);
            clearMemberFields(etMember2Sap, etMember2Name, etMember2Roll);
            btnRemoveMember.setVisibility(View.GONE);
            memberCount--;
        }
    }

    private void clearMemberFields(TextInputEditText sap, TextInputEditText name, TextInputEditText roll) {
        sap.setText("");
        name.setText("");
        roll.setText("");
    }

    private void validateAndCreateGroup() {
        List<Map<String, String>> memberList = new ArrayList<>();
        Set<String> sapIds = new HashSet<>();

        // Add leader
        if (!addMemberToList(etLeaderSap, etLeaderName, etLeaderRoll, memberList, sapIds)) return;

        // Add extra members
        if (tilMember2.getVisibility() == View.VISIBLE) 
            if (!addMemberToList(etMember2Sap, etMember2Name, etMember2Roll, memberList, sapIds)) return;
        if (tilMember3.getVisibility() == View.VISIBLE) 
            if (!addMemberToList(etMember3Sap, etMember3Name, etMember3Roll, memberList, sapIds)) return;
        if (tilMember4.getVisibility() == View.VISIBLE) 
            if (!addMemberToList(etMember4Sap, etMember4Name, etMember4Roll, memberList, sapIds)) return;

        createGroupInFirestore(memberList);
    }

    private boolean addMemberToList(TextInputEditText etSap, TextInputEditText etName, TextInputEditText etRoll, List<Map<String, String>> list, Set<String> sapIds) {
        String sap = etSap.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String roll = etRoll.getText().toString().trim();

        if (sap.isEmpty() || name.isEmpty() || roll.isEmpty()) {
            Toast.makeText(this, "Please fill all visible fields", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (sapIds.contains(sap)) {
            Toast.makeText(this, "Duplicate SAP ID: " + sap, Toast.LENGTH_SHORT).show();
            return false;
        }

        Map<String, String> member = new HashMap<>();
        member.put("sapId", sap);
        member.put("name", name);
        member.put("rollNo", roll);
        list.add(member);
        sapIds.add(sap);
        return true;
    }

    private void createGroupInFirestore(List<Map<String, String>> members) {
        btnCreateGroup.setEnabled(false);
        btnCreateGroup.setText("Creating...");

        String currentUid = mAuth.getUid();
        Map<String, Object> group = new HashMap<>();
        group.put("leaderUid", currentUid);
        group.put("memberDetails", members);
        group.put("createdAt", Timestamp.now());
        group.put("status", "Pending Title Approval");

        db.collection("groups").add(group)
                .addOnSuccessListener(ref -> {
                    String groupId = ref.getId();
                    db.collection("users").document(currentUid)
                            .update("hasGroup", true, "groupId", groupId)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Group Created Successfully!", Toast.LENGTH_LONG).show();
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
