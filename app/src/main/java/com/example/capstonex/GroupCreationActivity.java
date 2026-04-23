package com.example.capstonex;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

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

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private int memberCount = 1;
    private ProgressDialog progressDialog;

    private final Map<String, DataSnapshot> userCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_creation);
        setupEdgeToEdge(findViewById(R.id.group_creation_root));

        mDatabase = FirebaseDatabase.getInstance(AppConstants.REALTIME_DB_URL).getReference();
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

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
        mDatabase.child(AppConstants.NODE_USERS).orderByChild("sapId").equalTo(sap)
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
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void validateAndProcessGroup() {
        List<String> sapList = new ArrayList<>();
        String leaderSap = etLeaderSap.getText().toString().trim();
        sapList.add(leaderSap);
        if (tilMember2.getVisibility() == View.VISIBLE) sapList.add(etMember2Sap.getText().toString().trim());
        if (tilMember3.getVisibility() == View.VISIBLE) sapList.add(etMember3Sap.getText().toString().trim());
        if (tilMember4.getVisibility() == View.VISIBLE) sapList.add(etMember4Sap.getText().toString().trim());

        Set<String> uniqueSaps = new HashSet<>();
        for (String sap : sapList) {
            if (sap.isEmpty()) { Toast.makeText(this, "Fill all visible fields", Toast.LENGTH_SHORT).show(); return; }
            if (!uniqueSaps.add(sap)) { Toast.makeText(this, "Duplicate SAP ID: " + sap, Toast.LENGTH_SHORT).show(); return; }
        }

        List<DataSnapshot> membersToProcess = new ArrayList<>();
        for (String sap : sapList) {
            DataSnapshot doc = userCache.get(sap);
            if (doc == null) { Toast.makeText(this, "Details not found for SAP: " + sap, Toast.LENGTH_SHORT).show(); return; }
            if (Boolean.TRUE.equals(doc.child("hasGroup").getValue(Boolean.class))) {
                Toast.makeText(this, doc.child("name").getValue(String.class) + " is already in a group", Toast.LENGTH_LONG).show();
                return;
            }
            membersToProcess.add(doc);
        }

        DataSnapshot leaderDoc = userCache.get(leaderSap);
        String branch = leaderDoc.child("branch").getValue(String.class);
        if (branch == null || branch.isEmpty()) branch = "IT";
        int fixedYear = 2026;

        allocateGroupIdAndCreate(branch.toUpperCase(), fixedYear, membersToProcess);
    }

    private void allocateGroupIdAndCreate(String branch, int year, List<DataSnapshot> memberDocs) {
        btnCreateGroup.setEnabled(false);
        progressDialog.setMessage("Allocating Group ID...");
        progressDialog.show();

        // Node: Counters/IT2026
        DatabaseReference counterRef = mDatabase.child(AppConstants.NODE_COUNTERS).child(branch + year);
        final String prefix = branch + year + "_";

        counterRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer current = currentData.getValue(Integer.class);
                if (current == null) current = 0;
                currentData.setValue(current + 1);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot snapshot) {
                if (committed && snapshot != null) {
                    Integer count = snapshot.getValue(Integer.class);
                    executeCreation(prefix + count, memberDocs);
                } else {
                    progressDialog.dismiss();
                    btnCreateGroup.setEnabled(true);
                    Toast.makeText(GroupCreationActivity.this, "Transaction failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void executeCreation(String groupId, List<DataSnapshot> memberDocs) {
        progressDialog.setMessage("Finalizing " + groupId + "...");
        Map<String, Object> updates = new HashMap<>();
        List<String> memberUids = new ArrayList<>();
        List<Map<String, String>> memberDetails = new ArrayList<>();

        String firstStudentUid = memberDocs.get(0).getKey();

        for (DataSnapshot doc : memberDocs) {
            String uid = doc.getKey();
            memberUids.add(uid);
            Map<String, String> d = new HashMap<>();
            d.put("sapId", doc.child("sapId").getValue(String.class));
            d.put("name", doc.child("name").getValue(String.class));
            d.put("rollNo", doc.child("rollNo").getValue(String.class));
            memberDetails.add(d);

            updates.put("/" + AppConstants.NODE_USERS + "/" + uid + "/groupId", groupId);
            updates.put("/" + AppConstants.NODE_USERS + "/" + uid + "/hasGroup", true);

            String notifId = mDatabase.child(AppConstants.NODE_NOTIFICATIONS).push().getKey();
            Map<String, Object> n = new HashMap<>();
            n.put("userId", uid); n.put("title", "Group Assigned");
            n.put("message", "You have been added to group " + groupId);
            n.put("groupId", groupId); n.put("timestamp", ServerValue.TIMESTAMP);
            n.put("isRead", false);
            if (notifId != null) updates.put("/" + AppConstants.NODE_NOTIFICATIONS + "/" + notifId, n);
        }

        Map<String, Object> group = new HashMap<>();
        group.put("groupId", groupId);
        group.put("leaderUid", firstStudentUid); // Always set to first student UID
        group.put("memberUids", memberUids);
        group.put("memberDetails", memberDetails);
        group.put("status", "Active");
        group.put("createdAt", ServerValue.TIMESTAMP);

        updates.put("/" + AppConstants.NODE_GROUPS + "/" + groupId, group);

        mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(this, "Group " + groupId + " Created!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                btnCreateGroup.setEnabled(true);
                Toast.makeText(this, "Creation Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMemberField() {
        if (memberCount == 1) { tilMember2.setVisibility(View.VISIBLE); btnRemoveMember.setVisibility(View.VISIBLE); memberCount++; }
        else if (memberCount == 2) { tilMember3.setVisibility(View.VISIBLE); memberCount++; }
        else if (memberCount == 3) { tilMember4.setVisibility(View.VISIBLE); memberCount++; btnAddMember.setVisibility(View.GONE); }
    }

    private void removeMemberField() {
        if (memberCount == 4) { tilMember4.setVisibility(View.GONE); clearFields(etMember4Sap, etMember4Name, etMember4Roll); btnAddMember.setVisibility(View.VISIBLE); memberCount--; }
        else if (memberCount == 3) { tilMember3.setVisibility(View.GONE); clearFields(etMember3Sap, etMember3Name, etMember3Roll); memberCount--; }
        else if (memberCount == 2) { tilMember2.setVisibility(View.GONE); clearFields(etMember2Sap, etMember2Name, etMember2Roll); btnRemoveMember.setVisibility(View.GONE); memberCount--; }
    }

    private void clearFields(TextInputEditText s, TextInputEditText n, TextInputEditText r) {
        userCache.remove(s.getText().toString().trim());
        s.setText(""); n.setText(""); r.setText("");
    }
}
