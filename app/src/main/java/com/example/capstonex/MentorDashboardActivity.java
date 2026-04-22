package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.view.MenuItem;

public class MentorDashboardActivity extends BaseActivity {

    private static final String DB_URL =
            AppConstants.REALTIME_DB_URL;

    private DrawerLayout drawerLayout;
    private TextView tvMentorName;
    private ImageView ivProfileCard;
    private RecyclerView rvGroups;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private DatabaseReference groupsRef;

    // ── BUG-007 FIX: store listener so it can be removed in onDestroy() ──────
    private ValueEventListener groupsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mentor_dashboard);
        setupEdgeToEdge(findViewById(android.R.id.content));

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference().child("Users").child(uid);
        groupsRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference().child("Groups");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                    Intent intent = new Intent(MentorDashboardActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else if (id == R.id.nav_my_groups) {
                    Intent intent = new Intent(MentorDashboardActivity.this, MentorGroupsActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                } else if (id == R.id.nav_logout) {
                    mAuth.signOut();
                    Intent intent = new Intent(MentorDashboardActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        tvMentorName = findViewById(R.id.tvMentorName);
        rvGroups = findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        // ── BUG-005 FIX: use direct findViewById instead of fragile view traversal ──
        ivProfileCard = findViewById(R.id.ivMentorProfileCard);

        findViewById(R.id.ivNotifications).setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        findViewById(R.id.btnSummary).setOnClickListener(v -> {
            Intent intent = new Intent(this, AnalyticsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        loadMentorData(uid, navigationView);
    }

    private void loadMentorData(String uid, NavigationView navView) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String name = snapshot.child("name").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String photoUrl = snapshot.child("profileImageUrl").getValue(String.class);

                if (name == null) name = "Mentor";
                if (email == null) email = "";

                tvMentorName.setText(name);

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    if (ivProfileCard != null) {
                        Glide.with(MentorDashboardActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .circleCrop()
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(ivProfileCard);
                    }
                }

                View headerView = navView.getHeaderView(0);
                if (headerView != null) {
                    TextView tvHeaderName = headerView.findViewById(R.id.tvMentorHeaderName);
                    TextView tvHeaderEmail = headerView.findViewById(R.id.tvMentorHeaderEmail);
                    ImageView ivHeaderAvatar = headerView.findViewById(R.id.cvMentorHeaderAvatar)
                            != null
                            ? (ImageView) ((android.view.ViewGroup)
                            headerView.findViewById(R.id.cvMentorHeaderAvatar)).getChildAt(0)
                            : null;

                    if (tvHeaderName != null) tvHeaderName.setText(name);
                    if (tvHeaderEmail != null) tvHeaderEmail.setText(email);

                    if (ivHeaderAvatar != null && photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(MentorDashboardActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .circleCrop()
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(ivHeaderAvatar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvMentorName.setText("Mentor Dashboard");
            }
        });

        loadAssignedGroups(uid);
    }

    private void loadAssignedGroups(String uid) {
        // ── BUG-007 FIX: store listener reference for removal in onDestroy() ──
        groupsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.List<String> groupNames = new java.util.ArrayList<>();
                for (DataSnapshot group : snapshot.getChildren()) {
                    String groupName = group.child("groupId").getValue(String.class);
                    if (groupName != null) groupNames.add(groupName);
                }
                rvGroups.setAdapter(new SimpleStringAdapter(groupNames));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        groupsRef.orderByChild("mentorUid").equalTo(uid).addValueEventListener(groupsListener);
    }

    // ── BUG-007 FIX: remove all permanent listeners to prevent memory leaks ──
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (groupsListener != null && groupsRef != null) {
            groupsRef.removeEventListener(groupsListener);
        }
    }

    // ── BUG-005 FIX: old fragile view-traversal method removed ───────────────
    // findProfileImageView() was navigating getChildAt(0) chains that broke
    // whenever the layout changed. Now uses direct findViewById(R.id.ivMentorProfileCard).

    private static class SimpleStringAdapter
            extends RecyclerView.Adapter<SimpleStringAdapter.VH> {

        private final java.util.List<String> items;

        SimpleStringAdapter(java.util.List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setPadding(32, 24, 32, 24);
            tv.setTextSize(15f);
            tv.setTextColor(parent.getContext()
                    .getColor(R.color.colorBodyText));
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ((TextView) holder.itemView).setText("• " + items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            VH(android.view.View v) {
                super(v);
            }
        }
    }
}
