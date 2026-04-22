package com.example.capstonex;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * SplashActivity — CapstonX
 * <p>
 * Entry point of the app. Shows the splash screen for 2 seconds, then:
 * <p>
 * ── If user IS logged in ─────────────────────────────────────────────
 * Reads Users/{uid}/role from Realtime DB and navigates directly to
 * the correct dashboard — no login screen shown at all.
 * <p>
 * role = "student" → StudentDashboardActivity
 * role = "mentor"  → MentorDashboardActivity
 * role = "admin"   → AdminDashboardActivity
 * <p>
 * ── If user is NOT logged in ─────────────────────────────────────────
 * Navigates to LoginActivity as before.
 * <p>
 * ── If DB read fails (no internet on first launch after install) ─────
 * Falls back to LoginActivity — safe degradation.
 */
public class SplashActivity extends BaseActivity {

    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private static final long SPLASH_DELAY = 2000L; // 2 seconds

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private boolean navigated = false; // prevent double navigation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        applySystemWindowInsets(findViewById(android.R.id.content));

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference().child("Users");

        // After the splash delay, decide where to go
        new Handler(Looper.getMainLooper()).postDelayed(
                this::checkAuthAndNavigate,
                SPLASH_DELAY
        );
    }

    private void applySystemWindowInsets(View viewById) {
    }


    // ─────────────────────────────────────────────────────────────────────
    // Check Firebase Auth session
    // ─────────────────────────────────────────────────────────────────────
    private void checkAuthAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Not logged in → go to login screen
            goTo(LoginActivity.class);
            return;
        }

        // Logged in → read role from Realtime DB and route to correct dashboard
        String uid = currentUser.getUid();
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String role = null;
                if (snapshot.exists()) {
                    role = snapshot.child("role").getValue(String.class);
                }

                // Route based on role
                if ("mentor".equalsIgnoreCase(role)) {
                    goTo(MentorDashboardActivity.class);
                } else if ("admin".equalsIgnoreCase(role)) {
                    goTo(AdminDashboardActivity.class);
                } else if ("student".equalsIgnoreCase(role)) {
                    goTo(StudentDashboardActivity.class);
                } else {
                    // Role missing or DB record doesn't exist yet
                    // Sign out and send to login for a clean re-authentication
                    mAuth.signOut();
                    goTo(LoginActivity.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Network error — fall back to login safely
                // User can log in again and will be routed correctly
                goTo(LoginActivity.class);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Navigate to destination and finish splash
    // ─────────────────────────────────────────────────────────────────────
    private void goTo(Class<?> destination) {
        if (navigated) return; // safety guard — prevent double-call
        navigated = true;

        Intent intent = new Intent(SplashActivity.this, destination);
        // Clear the back stack — user should not be able to
        // press back from the dashboard and return to splash
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Slide transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }

        finish();
    }
}

