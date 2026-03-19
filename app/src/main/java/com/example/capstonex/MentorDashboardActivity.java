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

/**
 * MentorDashboardActivity — CapstonX
 * <p>
 * Loads the logged-in mentor's data from Realtime DB → Users/{uid}
 * and populates:
 * <p>
 * tvMentorName      → mentor's full name in the profile card
 * Profile ImageView → mentor's photo via Glide
 * Nav drawer header → tvMentorHeaderName, tvMentorHeaderEmail, avatar
 * ivNotifications   → opens NotificationsActivity
 * rvGroups          → groups assigned to this mentor (from Groups node)
 * btnSummary        → opens AnalyticsActivity
 * <p>
 * XML IDs (activity_mentor_dashboard.xml):
 * drawer_layout, toolbar, nav_view (NavigationView),
 * tvMentorName, ivNotifications, rvGroups, btnSummary
 * <p>
 * Nav header IDs (nav_header_mentor.xml):
 * tvMentorHeaderName, tvMentorHeaderEmail, cvMentorHeaderAvatar
 * <p>
 * Realtime DB fields read:
 * Users/{uid}/name, email, profileImageUrl, mentorId
 * Groups/ (filtered by mentorUid == uid)
 */
public class MentorDashboardActivity extends BaseActivity {

    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";

    // ── Views ──────────────────────────────────────────────────────────────
    private DrawerLayout drawerLayout;
    private TextView tvMentorName;
    private ImageView ivProfileCard; // inside profile card in main content
    private RecyclerView rvGroups;

    // ── Firebase ───────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private DatabaseReference groupsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mentor_dashboard);
        setupEdgeToEdge(findViewById(android.R.id.content));

        mAuth = FirebaseAuth.getInstance();

        // Safety check
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

        // ── Toolbar & Drawer ───────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

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

        // ── Bind views ─────────────────────────────────────────────────────
        tvMentorName = findViewById(R.id.tvMentorName);
        rvGroups = findViewById(R.id.rvGroups);
        rvGroups.setLayoutManager(new LinearLayoutManager(this));

        // Profile ImageView is inside the MaterialCardView in the content card
        // It's the first ImageView in the horizontal layout inside the blue card
        ivProfileCard = findProfileImageView();

        // ── Toolbar actions ────────────────────────────────────────────────
        findViewById(R.id.ivNotifications).setOnClickListener(v -> {
            Intent intent = new Intent(this, NotificationsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // ── Summary button ─────────────────────────────────────────────────
        findViewById(R.id.btnSummary).setOnClickListener(v -> {
            Intent intent = new Intent(this, AnalyticsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // ── Drawer nav item clicks ─────────────────────────────────────────
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            int id = item.getItemId();
//            if (id == R.id.nav_meetings) {
//                startActivity(new Intent(this, MeetingsActivity.class));
//            } else if (id == R.id.nav_rubric) {
//                startActivity(new Intent(this, RubricManagementActivity.class));
//            } else if (id == R.id.nav_topic_approval) {
//                startActivity(new Intent(this, TopicApprovalActivity.class));
//            } else if (id == R.id.nav_notifications) {
//                startActivity(new Intent(this, NotificationsActivity.class));
//            } else if (id == R.id.nav_settings) {
//                startActivity(new Intent(this, SettingsActivity.class));
//            }
            return true;
        });

        // ── Load mentor data ───────────────────────────────────────────────
        loadMentorData(uid, navView);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Load mentor data from Realtime DB
    // ─────────────────────────────────────────────────────────────────────
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

                // ── Populate main content profile card ────────────────────
                tvMentorName.setText(name);

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    // Load into main content card avatar
                    if (ivProfileCard != null) {
                        Glide.with(MentorDashboardActivity.this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_person)
                                .circleCrop()
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(ivProfileCard);
                    }
                }

                // ── Populate Navigation Drawer header ─────────────────────
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

        // ── Load assigned groups ───────────────────────────────────────────
        loadAssignedGroups(uid);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Load groups where mentorUid == this mentor's uid
    // ─────────────────────────────────────────────────────────────────────
    private void loadAssignedGroups(String uid) {
        groupsRef.orderByChild("mentorUid").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Build a simple list of group names for now
                        // Replace with a proper GroupAdapter when ready
                        java.util.List<String> groupNames = new java.util.ArrayList<>();
                        for (DataSnapshot group : snapshot.getChildren()) {
                            String groupName = group.child("name").getValue(String.class);
                            if (groupName != null) groupNames.add(groupName);
                        }

                        // Simple text adapter to show group names until GroupAdapter is built
                        rvGroups.setAdapter(new SimpleStringAdapter(groupNames));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Show empty state — no crash
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Find the profile ImageView inside the blue profile card in main content
    // The card contains a horizontal LinearLayout → CardView → ImageView
    // ─────────────────────────────────────────────────────────────────────
    private ImageView findProfileImageView() {
        try {
            // The content card's LinearLayout → first child is the avatar CardView
            android.view.ViewGroup contentCard =
                    (android.view.ViewGroup) ((android.view.ViewGroup)
                            rvGroups.getParent()).getChildAt(0);
            // Walk into the first MaterialCardView → LinearLayout → CardView → ImageView
            android.view.ViewGroup outerLayout =
                    (android.view.ViewGroup) contentCard.getChildAt(0);
            android.view.ViewGroup avatarCard =
                    (android.view.ViewGroup) outerLayout.getChildAt(0);
            return (ImageView) avatarCard.getChildAt(0);
        } catch (Exception e) {
            return null; // graceful fallback — photo just won't load
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Minimal inline adapter for group name strings
    // Replace this with a full GroupAdapter when building the groups feature
    // ─────────────────────────────────────────────────────────────────────
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


//package com.example.capstonex;
//
//import android.os.Bundle;
//
//import androidx.activity.OnBackPressedCallback;
//import androidx.appcompat.widget.Toolbar;
//import androidx.core.view.GravityCompat;
//import androidx.drawerlayout.widget.DrawerLayout;
//
//public class MentorDashboardActivity extends BaseActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_mentor_dashboard);
//        setupEdgeToEdge(findViewById(android.R.id.content));
//
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle("");
//        }
//
//        final DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
//        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
//
//        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
//            @Override
//            public void handleOnBackPressed() {
//                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
//                    drawerLayout.closeDrawer(GravityCompat.START);
//                } else {
//                    setEnabled(false);
//                    getOnBackPressedDispatcher().onBackPressed();
//                }
//            }
//        });
//    }
//}
