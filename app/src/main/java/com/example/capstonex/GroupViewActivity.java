package com.example.capstonex;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * GroupViewActivity — CapstonX
 *
 * Shown after a student's group has been registered.
 * Reads:  Users/{uid}/groupId  →  Groups/{groupId}
 * Displays: group ID, status, leader, and all member names/SAP/roll numbers.
 *
 * XML IDs required (activity_group_view.xml):
 *   toolbar, tvGroupId, tvGroupStatus, tvLeaderName,
 *   llMembersContainer, progressBar, tvError
 */
public class GroupViewActivity extends BaseActivity {

    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";

    private TextView tvGroupId, tvGroupStatus, tvLeaderName, tvError;
    private LinearLayout llMembersContainer;
    private ProgressBar progressBar;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_view);
        setupEdgeToEdge(findViewById(R.id.group_view_root));

        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        mAuth     = FirebaseAuth.getInstance();

        // ── Bind views ─────────────────────────────────────────────────────
        tvGroupId         = findViewById(R.id.tvGroupId);
        tvGroupStatus     = findViewById(R.id.tvGroupStatus);
        tvLeaderName      = findViewById(R.id.tvLeaderName);
        llMembersContainer = findViewById(R.id.llMembersContainer);
        progressBar       = findViewById(R.id.progressBar);
        tvError           = findViewById(R.id.tvError);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        // ── Load group ─────────────────────────────────────────────────────
        if (mAuth.getCurrentUser() == null) { finish(); return; }
        loadGroupData(mAuth.getCurrentUser().getUid());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Step 1: get groupId from user node
    // ─────────────────────────────────────────────────────────────────────
    private void loadGroupData(String uid) {
        showLoading(true);
        mDatabase.child("Users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                        String groupId = userSnap.child("groupId").getValue(String.class);
                        if (groupId == null || groupId.isEmpty()) {
                            showError("No group found for your account.");
                            return;
                        }
                        fetchGroup(groupId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("Failed to load user data: " + error.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Step 2: fetch the group document
    // ─────────────────────────────────────────────────────────────────────
    private void fetchGroup(String groupId) {
        mDatabase.child("Groups").child(groupId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot groupSnap) {
                        showLoading(false);
                        if (!groupSnap.exists()) {
                            showError("Group data not found.");
                            return;
                        }
                        populateUI(groupSnap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("Failed to load group: " + error.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Populate all views from the group snapshot
    // ─────────────────────────────────────────────────────────────────────
    private void populateUI(DataSnapshot groupSnap) {
        // ── Group meta ────────────────────────────────────────────────────
        String groupId = groupSnap.child("groupId").getValue(String.class);
        tvGroupId.setText(groupId != null ? groupId : "—");

        String status = groupSnap.child("status").getValue(String.class);
        tvGroupStatus.setText(status != null ? status : "—");

        // ── Resolve leader UID → name ─────────────────────────────────────
        String leaderUid = groupSnap.child("leaderUid").getValue(String.class);
        if (leaderUid != null) {
            resolveLeaderName(leaderUid, groupSnap);
        } else {
            tvLeaderName.setText("—");
            buildMemberList(groupSnap);
        }
    }

    private void resolveLeaderName(String leaderUid, DataSnapshot groupSnap) {
        mDatabase.child("Users").child(leaderUid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        String name = snap.getValue(String.class);
                        tvLeaderName.setText(name != null ? name : leaderUid);
                        buildMemberList(groupSnap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvLeaderName.setText(leaderUid);
                        buildMemberList(groupSnap);
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Build one row per member from memberDetails list
    // ─────────────────────────────────────────────────────────────────────
    private void buildMemberList(DataSnapshot groupSnap) {
        llMembersContainer.removeAllViews();

        Iterable<DataSnapshot> memberDetails = groupSnap
                .child("memberDetails").getChildren();

        int index = 1;
        for (DataSnapshot memberSnap : memberDetails) {
            String name  = memberSnap.child("name").getValue(String.class);
            String sap   = memberSnap.child("sapId").getValue(String.class);
            String roll  = memberSnap.child("rollNo").getValue(String.class);

            View row = getLayoutInflater()
                    .inflate(R.layout.item_group_member, llMembersContainer, false);

            ((TextView) row.findViewById(R.id.tvMemberIndex)).setText(String.valueOf(index));
            ((TextView) row.findViewById(R.id.tvMemberName)).setText(name  != null ? name  : "—");
            ((TextView) row.findViewById(R.id.tvMemberSap)).setText("SAP: "  + (sap  != null ? sap  : "—"));
            ((TextView) row.findViewById(R.id.tvMemberRoll)).setText("Roll: " + (roll != null ? roll : "—"));

            llMembersContainer.addView(row);
            index++;
        }

        if (index == 1) {
            // No memberDetails stored — show plain message
            TextView empty = new TextView(this);
            empty.setText("No member details available.");
            llMembersContainer.addView(empty);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // UI helpers
    // ─────────────────────────────────────────────────────────────────────
    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        showLoading(false);
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
