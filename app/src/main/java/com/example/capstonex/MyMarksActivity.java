package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class MyMarksActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    
    private TextView tvTotalMarks, tvR1Marks, tvR2Marks, tvInternalMarks, tvStudentHeader;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_marks);
        setupEdgeToEdge(findViewById(R.id.bottom_navigation));

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();

        // Bind Views
        tvTotalMarks = findViewById(R.id.tvTotalMarks);
        // We need to add IDs to the individual marks TextViews in XML
        // I will update the XML IDs in the next step to ensure this works.
        tvR1Marks = findViewById(R.id.tvR1Marks);
        tvR2Marks = findViewById(R.id.tvR2Marks);
        tvInternalMarks = findViewById(R.id.tvInternalMarks);
        tvStudentHeader = findViewById(R.id.tvStudentHeader);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        toolbar.setNavigationOnClickListener(v -> finish());
        
        setupBottomNavigation();
        loadMarks();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_reviews);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, StudentDashboardActivity.class));
                return true;
            } else if (id == R.id.nav_logbook) {
                startActivity(new Intent(this, LogbookActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return id == R.id.nav_reviews;
        });
    }

    private void loadMarks() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        // 1. Fetch Student Basic Info (Name, Branch, etc)
        mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String branch = snapshot.child("branch").getValue(String.class);
                    String sem = snapshot.child("semester").getValue(String.class);
                    tvStudentHeader.setText(String.format("%s · %s · %s", name, branch, sem));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Fetch Marks
        mDatabase.child("Marks").child(uid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double r1 = snapshot.child("r1").getValue(Double.class) != null ? snapshot.child("r1").getValue(Double.class) : 0.0;
                    double r2 = snapshot.child("r2").getValue(Double.class) != null ? snapshot.child("r2").getValue(Double.class) : 0.0;
                    double internal = snapshot.child("internal").getValue(Double.class) != null ? snapshot.child("internal").getValue(Double.class) : 0.0;
                    
                    double total = r1 + r2 + internal;

                    tvR1Marks.setText(String.format(Locale.getDefault(), "%.1f / 25", r1));
                    tvR2Marks.setText(String.format(Locale.getDefault(), "%.1f / 25", r2));
                    tvInternalMarks.setText(String.format(Locale.getDefault(), "%02d / 10", (int)internal));
                    tvTotalMarks.setText(String.format(Locale.getDefault(), "%.1f / 100", total));
                } else {
                    tvTotalMarks.setText("0.0 / 100");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyMarksActivity.this, "Failed to load marks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
