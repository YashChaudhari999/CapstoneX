package com.example.capstonex;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupCreationActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";

    private TextInputEditText etLeaderSap, etLeaderName, etLeaderRoll;
    private TextInputEditText etMember2Sap, etMember2Name, etMember2Roll;
    private TextInputEditText etMember3Sap, etMember3Name, etMember3Roll;
    private TextInputEditText etMember4Sap, etMember4Name, etMember4Roll;

    private View tilMember2, tilMember3, tilMember4;
    private MaterialButton btnAddMember, btnRemoveMember, btnCreateGroup;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private int memberCount = 1;

    private final Map<String, DataSnapshot> userCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_creation);
        setupEdgeToEdge(findViewById(R.id.group_creation_root));

        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupAutofill();

        btnAddMember.setOnClickListener(v -> addMemberField());
        btnRemoveMember.setOnClickListener(v -> removeMemberField());
        btnCreateGroup.setOnClickListener(v -> validateAndProcessGroup());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void initViews() {
        etLeaderSap = findViewById(R.id.etLeaderSap);
        etLeaderName = findViewById(R.id.etLeaderName);
        etLeaderRoll = findViewById(R.id.etLeaderRoll);
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
    }

    private void setupAutofill() {
        addSapWatcher(etLeaderSap, etLeaderName, etLeaderRoll);
        addSapWatcher(etMember2Sap, etMember2Name, etMember2Roll);
        addSapWatcher(etMember3Sap, etMember3Name, etMember3Roll);
        addSapWatcher(etMember4Sap, etMember4Name, etMember4Roll);
    }

    private void addSapWatcher(TextInputEditText sapEt, TextInputEditText nameEt, TextInputEditText rollEt) {
        sapEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String sap = s.toString().trim();
                if (sap.length() >= 8) fetchUserDetails(sap, nameEt, rollEt);
                else { nameEt.setText(""); rollEt.setText(""); userCache.remove(sap); }
            }
        });
    }

    private void fetchUserDetails(String sap, TextInputEditText nameEt, TextInputEditText rollEt) {
        mDatabase.child("Users").orderByChild("sapId").equalTo(sap)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot userDoc : snapshot.getChildren()) {
                                nameEt.setText(userDoc.child("name").getValue(String.class));
                                rollEt.setText(userDoc.child("rollNo").getValue(String.class));
                                userCache.put(sap, userDoc);
                                break;
                            }
                        } else {
                            nameEt.setText(""); rollEt.setText("");
                            nameEt.setError("User not registered");
                            userCache.remove(sap);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(GroupCreationActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void validateAndProcessGroup() {
        List<String> sapList = new ArrayList<>();
        sapList.add(etLeaderSap.getText().toString().trim());
        if (tilMember2.getVisibility() == View.VISIBLE) sapList.add(etMember2Sap.getText().toString().trim());
        if (tilMember3.getVisibility() == View.VISIBLE) sapList.add(etMember3Sap.getText().toString().trim());
        if (tilMember4.getVisibility() == View.VISIBLE) sapList.add(etMember4Sap.getText().toString().trim());

        Set<String> uniqueSaps = new HashSet<>();
        for (String sap : sapList) {
            if (sap.isEmpty()) { Toast.makeText(this, "Fill all visible SAP fields", Toast.LENGTH_SHORT).show(); return; }
            if (!uniqueSaps.add(sap)) { Toast.makeText(this, "Duplicate SAP ID: " + sap, Toast.LENGTH_SHORT).show(); return; }
        }

        List<DataSnapshot> membersToProcess = new ArrayList<>();
        for (String sap : sapList) {
            DataSnapshot doc = userCache.get(sap);
            if (doc == null) { Toast.makeText(this, "Details not found for SAP: " + sap, Toast.LENGTH_SHORT).show(); return; }
            
            Boolean hasGroup = doc.child("hasGroup").getValue(Boolean.class);
            if (Boolean.TRUE.equals(hasGroup)) {
                Toast.makeText(this, doc.child("name").getValue(String.class) + " is already in a group", Toast.LENGTH_LONG).show();
                return;
            }
            membersToProcess.add(doc);
        }
        executeGroupCreation(membersToProcess);
    }

    private void executeGroupCreation(List<DataSnapshot> memberDocs) {
        btnCreateGroup.setEnabled(false);
        btnCreateGroup.setText("Creating...");

        String groupId = mDatabase.child("Groups").push().getKey();
        if (groupId == null) {
            btnCreateGroup.setEnabled(true);
            btnCreateGroup.setText("CREATE GROUP");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        List<String> memberUids = new ArrayList<>();
        List<Map<String, String>> memberDetails = new ArrayList<>();

        for (DataSnapshot doc : memberDocs) {
            String uid = doc.getKey();
            String name = doc.child("name").getValue(String.class);
            String sap = doc.child("sapId").getValue(String.class);
            String roll = doc.child("rollNo").getValue(String.class);
            
            memberUids.add(uid);
            
            Map<String, String> details = new HashMap<>();
            details.put("sapId", sap); details.put("name", name); details.put("rollNo", roll);
            memberDetails.add(details);
            
            updates.put("/Users/" + uid + "/groupId", groupId);
            updates.put("/Users/" + uid + "/hasGroup", true);
            
            String notifId = mDatabase.child("Notifications").push().getKey();
            Map<String, Object> notif = new HashMap<>();
            notif.put("userId", uid); 
            notif.put("title", "Group Assigned");
            notif.put("message", "Added to Group " + groupId + " with members: " + name);
            notif.put("groupId", groupId); 
            notif.put("timestamp", ServerValue.TIMESTAMP);
            notif.put("isRead", false);
            
            if (notifId != null) {
                updates.put("/Notifications/" + notifId, notif);
            }
        }

        Map<String, Object> groupObj = new HashMap<>();
        groupObj.put("leaderUid", mAuth.getUid()); 
        groupObj.put("memberUids", memberUids);
        groupObj.put("memberDetails", memberDetails); 
        groupObj.put("createdAt", ServerValue.TIMESTAMP);
        groupObj.put("status", "Active"); 
        groupObj.put("groupId", groupId);
        
        updates.put("/Groups/" + groupId, groupObj);

        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Group Created Successfully!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                btnCreateGroup.setEnabled(true); 
                btnCreateGroup.setText("CREATE GROUP");
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMemberField() {
        if (memberCount == 1) { tilMember2.setVisibility(View.VISIBLE); btnRemoveMember.setVisibility(View.VISIBLE); memberCount++; }
        else if (memberCount == 2) { tilMember3.setVisibility(View.VISIBLE); memberCount++; }
        else if (memberCount == 3) { tilMember4.setVisibility(View.VISIBLE); memberCount++; btnAddMember.setVisibility(View.GONE); }
    }

    private void removeMemberField() {
        if (memberCount == 4) { tilMember4.setVisibility(View.GONE); clearMemberFields(etMember4Sap, etMember4Name, etMember4Roll); btnAddMember.setVisibility(View.VISIBLE); memberCount--; }
        else if (memberCount == 3) { tilMember3.setVisibility(View.GONE); clearMemberFields(etMember3Sap, etMember3Name, etMember3Roll); memberCount--; }
        else if (memberCount == 2) { tilMember2.setVisibility(View.GONE); clearMemberFields(etMember2Sap, etMember2Name, etMember2Roll); btnRemoveMember.setVisibility(View.GONE); memberCount--; }
    }

    private void clearMemberFields(TextInputEditText sap, TextInputEditText name, TextInputEditText roll) {
        userCache.remove(sap.getText().toString().trim());
        sap.setText(""); name.setText(""); roll.setText("");
    }
}
