package com.example.capstonex;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
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
 * Displays: Assigned Mentor, Group meta details (ID, Status, Leader), and all members.
 */
public class GroupViewActivity extends BaseActivity {

    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";

    private TextView tvGroupId, tvGroupStatus, tvLeaderName, tvError;
    private LinearLayout llMembersContainer;
    private ProgressBar progressBar;

    // Mentor Views
    private MaterialCardView cardMentorInfo;
    private TextView tvDetailMentorName, tvDetailMentorDomain;
    private ImageView ivMentorAvatar;

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

        // Mentor Views
        cardMentorInfo       = findViewById(R.id.cardMentorInfo);
        tvDetailMentorName   = findViewById(R.id.tvDetailMentorName);
        tvDetailMentorDomain = findViewById(R.id.tvDetailMentorDomain);
        ivMentorAvatar       = findViewById(R.id.ivMentorAvatar);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        // ── Load group ─────────────────────────────────────────────────────
        if (mAuth.getCurrentUser() == null) { finish(); return; }
        loadGroupData(mAuth.getCurrentUser().getUid());
    }

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
                        showError("Failed to load user data.");
                    }
                });
    }

    private void fetchGroup(String groupId) {
        mDatabase.child("Groups").child(groupId)
                .addValueEventListener(new ValueEventListener() {
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
                        showError("Failed to load group.");
                    }
                });
    }

    private void populateUI(DataSnapshot groupSnap) {
        // Group ID and Status
        String gid = groupSnap.child("groupId").getValue(String.class);
        tvGroupId.setText("#" + (gid != null ? gid : groupSnap.getKey()));

        String status = groupSnap.child("status").getValue(String.class);
        tvGroupStatus.setText(status != null ? status.toUpperCase() : "PENDING");

        // ── Handle Leader Resolution ──
        String leaderUid = groupSnap.child("leaderUid").getValue(String.class);
        if (leaderUid != null && !leaderUid.isEmpty()) {
            resolveLeaderName(leaderUid);
        } else {
            tvLeaderName.setText("Not Assigned");
        }

        // ── Handle Mentor Details ──
        String mentorUid = groupSnap.child("mentorUid").getValue(String.class);
        if (mentorUid != null && !mentorUid.isEmpty()) {
            fetchMentorDetails(mentorUid);
        } else {
            cardMentorInfo.setVisibility(View.GONE);
        }

        // Build Member List
        buildMemberList(groupSnap);
    }

    private void resolveLeaderName(String uid) {
        mDatabase.child("Users").child(uid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists()) {
                            tvLeaderName.setText(snap.getValue(String.class));
                        } else {
                            tvLeaderName.setText("Unknown");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchMentorDetails(String mentorUid) {
        mDatabase.child("Users").child(mentorUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot mentorSnap) {
                        if (mentorSnap.exists()) {
                            String name = mentorSnap.child("name").getValue(String.class);
                            String domain = mentorSnap.child("domain").getValue(String.class);
                            String photo = mentorSnap.child("profileImageUrl").getValue(String.class);

                            tvDetailMentorName.setText(name != null ? name : "Mentor");
                            tvDetailMentorDomain.setText("Expertise: " + (domain != null ? domain : "General"));
                            
                            if (photo != null && !photo.isEmpty()) {
                                Glide.with(GroupViewActivity.this)
                                        .load(photo)
                                        .placeholder(R.drawable.ic_person)
                                        .circleCrop()
                                        .into(ivMentorAvatar);
                            }

                            cardMentorInfo.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void buildMemberList(DataSnapshot groupSnap) {
        llMembersContainer.removeAllViews();
        Iterable<DataSnapshot> memberDetails = groupSnap.child("memberDetails").getChildren();

        int index = 1;
        for (DataSnapshot memberSnap : memberDetails) {
            String name  = memberSnap.child("name").getValue(String.class);
            String sap   = memberSnap.child("sapId").getValue(String.class);
            String roll  = memberSnap.child("rollNo").getValue(String.class);

            View row = getLayoutInflater().inflate(R.layout.item_group_member, llMembersContainer, false);

            ((TextView) row.findViewById(R.id.tvMemberIndex)).setText(String.valueOf(index));
            ((TextView) row.findViewById(R.id.tvMemberName)).setText(name != null ? name : "—");
            ((TextView) row.findViewById(R.id.tvMemberSap)).setText("SAP: " + (sap != null ? sap : "—"));
            ((TextView) row.findViewById(R.id.tvMemberRoll)).setText("Roll: " + (roll != null ? roll : "—"));

            llMembersContainer.addView(row);
            index++;
        }
    }

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
