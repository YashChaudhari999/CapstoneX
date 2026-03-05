package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StudentDashboardActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);
        setupEdgeToEdge(findViewById(R.id.bottom_navigation).getRootView());

        // 1. Bottom Navigation Setup
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
//                navigateTo(StudentDashboardActivity.class);
//                Intent intent = new Intent(this, StudentDashboardActivity.class);
//                startActivity(intent);
                return true;
            } else if (id == R.id.nav_logbook) {
//                navigateTo(LogbookActivity.class);
                Intent intent = new Intent(this, LogbookActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_reviews) {
//                navigateTo(MyMarksActivity.class);
                Intent intent = new Intent(this, MyMarksActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_profile) {
//                navigateTo(ProfileActivity.class);
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            return true;
        });

        // 2. Toolbar Actions
        findViewById(R.id.ivNotifications).setOnClickListener(v -> navigateTo(NotificationsActivity.class));
        findViewById(R.id.cvToolbarProfile).setOnClickListener(v -> navigateTo(ProfileActivity.class));

        // 3. Tile Clicks
        findViewById(R.id.tileTimeline).setOnClickListener(v -> navigateTo(ProjectTimelineActivity.class));
        findViewById(R.id.tileDocuments).setOnClickListener(v -> navigateTo(DocumentsActivity.class));
        findViewById(R.id.tileChat).setOnClickListener(v -> navigateTo(ChatActivity.class));
        findViewById(R.id.tilePlagiarism).setOnClickListener(v -> navigateTo(PlagiarismActivity.class));
    }

    private void navigateTo(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
