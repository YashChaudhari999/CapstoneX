package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * StudentDashboardActivity — CapstonX
 * <p>
 * Loads the logged-in student's data from Realtime DB → Users/{uid}
 * and populates the dashboard with real information:
 * <p>
 * tvStudentName    → "Hi, {firstName}!" greeting
 * cvToolbarProfile → profile photo loaded via Glide from profileImageUrl
 * pbOverall        → project progress (read from Users/{uid}/progress)
 * tvProgressPercent→ "X% Completed"
 * <p>
 * XML IDs (activity_student_dashboard.xml):
 * toolbar, tvStudentName, cvToolbarProfile (ImageView inside),
 * ivNotifications, pbOverall, tvProgressPercent,
 * tileTimeline, tileDocuments, tileChat, tilePlagiarism,
 * bottom_navigation
 * <p>
 * Realtime DB fields read:
 * Users/{uid}/name, profileImageUrl, sapId, rollNo, branch, progress
 */
public class StudentDashboardActivity extends BaseActivity {

    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";

    // ── Views ──────────────────────────────────────────────────────────────
    private TextView tvStudentName, tvProgressPercent;
    private ImageView ivProfile;
    private ProgressBar pbOverall;

    // ── Firebase ───────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);
        setupEdgeToEdge(findViewById(R.id.student_dashboard_root));

        mAuth = FirebaseAuth.getInstance();

        // Safety check — if somehow not logged in, go back to login
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference().child("Users").child(uid);

        // ── Bind views ─────────────────────────────────────────────────────
        tvStudentName = findViewById(R.id.tvStudentName);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        pbOverall = findViewById(R.id.pbOverall);

        // Profile image is inside the MaterialCardView cvToolbarProfile
        ivProfile = findViewById(R.id.cvToolbarProfile)
                .findViewById(android.R.id.content);
        // Fallback: find the ImageView directly inside the card
        if (ivProfile == null) {
            ivProfile = (ImageView) ((android.view.ViewGroup)
                    findViewById(R.id.cvToolbarProfile)).getChildAt(0);
        }

        // ── Load user data from Realtime DB ────────────────────────────────
        loadUserData();

        // ── Bottom Navigation ──────────────────────────────────────────────
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true; // already here
            } else if (id == R.id.nav_logbook) {
                navigateTo(LogbookActivity.class);
                return true;
            } else if (id == R.id.nav_reviews) {
                navigateTo(MyMarksActivity.class);
                return true;
            } else if (id == R.id.nav_profile) {
                navigateTo(ProfileActivity.class);
                return true;
            }
            return false;
        });

        // ── Toolbar actions ────────────────────────────────────────────────
        findViewById(R.id.ivNotifications).setOnClickListener(
                v -> navigateTo(NotificationsActivity.class));
        findViewById(R.id.cvToolbarProfile).setOnClickListener(
                v -> navigateTo(ProfileActivity.class));

        // ── Dashboard tiles ────────────────────────────────────────────────
        findViewById(R.id.tileTimeline).setOnClickListener(
                v -> navigateTo(ProjectTimelineActivity.class));
        findViewById(R.id.tileDocuments).setOnClickListener(
                v -> navigateTo(DocumentsActivity.class));
        findViewById(R.id.tileChat).setOnClickListener(
                v -> navigateTo(ChatActivity.class));
        findViewById(R.id.tilePlagiarism).setOnClickListener(
                v -> navigateTo(PlagiarismActivity.class));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Load student data from Realtime DB
    // ─────────────────────────────────────────────────────────────────────
    private void loadUserData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // ── Name greeting ─────────────────────────────────────────
                String fullName = snapshot.child("name").getValue(String.class);
                if (fullName != null && !fullName.isEmpty()) {
                    // Show only first name for a friendly greeting
                    String firstName = fullName.split(" ")[0];
                    tvStudentName.setText("Hi, " + firstName + " 👋");
                } else {
                    tvStudentName.setText("Student Dashboard");
                }

                // ── Profile photo ─────────────────────────────────────────
                String photoUrl = snapshot.child("profileImageUrl")
                        .getValue(String.class);
                if (photoUrl != null && !photoUrl.isEmpty() && ivProfile != null) {
                    Glide.with(StudentDashboardActivity.this)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_person)
                            .circleCrop()
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(ivProfile);
                }

                // ── Project progress ──────────────────────────────────────
                // Read from Users/{uid}/progress (0–100), default 0
                Integer progress = snapshot.child("progress").getValue(Integer.class);
                if (progress == null) progress = 0;
                pbOverall.setProgress(progress);
                tvProgressPercent.setText(progress + "% Completed");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Keep default placeholder text — don't crash
                tvStudentName.setText("Student Dashboard");
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Navigate helper with slide animation
    // ─────────────────────────────────────────────────────────────────────
    private void navigateTo(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}


//package com.example.capstonex;
//
//import android.content.Intent;
//import android.os.Bundle;
//
//import com.google.android.material.bottomnavigation.BottomNavigationView;
//
//public class StudentDashboardActivity extends BaseActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_student_dashboard);
//        setupEdgeToEdge(findViewById(R.id.bottom_navigation).getRootView());
//
//        // 1. Bottom Navigation Setup
//        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
//        bottomNavigation.setSelectedItemId(R.id.nav_home);
//
//        bottomNavigation.setOnItemSelectedListener(item -> {
//            int id = item.getItemId();
//            if (id == R.id.nav_home) {
/// /                navigateTo(StudentDashboardActivity.class);
/// /                Intent intent = new Intent(this, StudentDashboardActivity.class);
/// /                startActivity(intent);
//                return true;
//            } else if (id == R.id.nav_logbook) {
////                navigateTo(LogbookActivity.class);
//                Intent intent = new Intent(this, LogbookActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                startActivity(intent);
//                return true;
//            } else if (id == R.id.nav_reviews) {
////                navigateTo(MyMarksActivity.class);
//                Intent intent = new Intent(this, MyMarksActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                startActivity(intent);
//                return true;
//            } else if (id == R.id.nav_profile) {
////                navigateTo(ProfileActivity.class);
//                Intent intent = new Intent(this, ProfileActivity.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//                startActivity(intent);
//                return true;
//            }
//            return true;
//        });
//
//        // 2. Toolbar Actions
//        findViewById(R.id.ivNotifications).setOnClickListener(v -> navigateTo(NotificationsActivity.class));
//        findViewById(R.id.cvToolbarProfile).setOnClickListener(v -> navigateTo(ProfileActivity.class));
//
//        // 3. Tile Clicks
//        findViewById(R.id.tileTimeline).setOnClickListener(v -> navigateTo(ProjectTimelineActivity.class));
//        findViewById(R.id.tileDocuments).setOnClickListener(v -> navigateTo(DocumentsActivity.class));
//        findViewById(R.id.tileChat).setOnClickListener(v -> navigateTo(ChatActivity.class));
//        findViewById(R.id.tilePlagiarism).setOnClickListener(v -> navigateTo(PlagiarismActivity.class));
//    }
//
//    private void navigateTo(Class<?> cls) {
//        Intent intent = new Intent(this, cls);
//        startActivity(intent);
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
//    }
//}
