package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
// BUG-LOG-01 FIX: removed unused AppCompatActivity import — now extends BaseActivity
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// BUG-LOG-01 FIX: was AppCompatActivity — changed to BaseActivity for consistent status bar + edge-to-edge
public class LogbookActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";

    private RecyclerView rvLogEntries;
    private LogbookAdapter adapter;
    private List<LogEntryModel> logList;
    private TextView tvTotalEntries, tvSignedEntries, tvPendingEntries;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userGroupId = "";

    // BUG-LOG-02 FIX: store permanent listener reference for cleanup in onDestroy()
    private ValueEventListener logbookListener;
    private DatabaseReference logbookRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logbook);
        setupEdgeToEdge(findViewById(android.R.id.content)); // BUG-LOG-01 FIX

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();

        initViews();
        setupBottomNav();
        fetchUserGroupId();

        findViewById(R.id.fabAddEntry).setOnClickListener(v -> {
            startActivity(new Intent(LogbookActivity.this, AddLogActivity.class));
        });
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");
        toolbar.setNavigationOnClickListener(v -> finish());

        rvLogEntries = findViewById(R.id.rvLogEntries);
        rvLogEntries.setLayoutManager(new LinearLayoutManager(this));
        logList = new ArrayList<>();
        adapter = new LogbookAdapter(logList);
        rvLogEntries.setAdapter(adapter);

        tvTotalEntries = findViewById(R.id.tvTotalEntries);
        tvSignedEntries = findViewById(R.id.tvSignedEntries);
        tvPendingEntries = findViewById(R.id.tvPendingEntries);
    }

    private void fetchUserGroupId() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        mDatabase.child("Users").child(uid).child("groupId").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    userGroupId = snapshot.getValue(String.class);
                    if (userGroupId != null && !userGroupId.isEmpty()) {
                        loadLogEntries();
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadLogEntries() {
        // BUG-LOG-02 FIX: store listener reference so it can be removed in onDestroy()
        logbookRef = mDatabase.child("Logbook").child(userGroupId);
        logbookListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                logList.clear();
                int total = 0, signed = 0, pending = 0;

                for (DataSnapshot logSnap : snapshot.getChildren()) {
                    LogEntryModel entry = logSnap.getValue(LogEntryModel.class);
                    if (entry != null) {
                        logList.add(0, entry); // Newest first
                        total++;
                        if (entry.isSigned()) signed++;
                        else pending++;
                    }
                }

                adapter.notifyDataSetChanged();
                updateStats(total, signed, pending);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LogbookActivity.this, "Failed to load logs", Toast.LENGTH_SHORT).show();
            }
        };
        logbookRef.addValueEventListener(logbookListener);
    }

    // BUG-LOG-02 FIX: remove permanent listener on destroy to prevent memory leak
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (logbookListener != null && logbookRef != null) {
            logbookRef.removeEventListener(logbookListener);
        }
    }

    private void updateStats(int total, int signed, int pending) {
        tvTotalEntries.setText(String.valueOf(total));
        tvSignedEntries.setText(String.valueOf(signed));
        tvPendingEntries.setText(String.valueOf(pending));
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_logbook);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, StudentDashboardActivity.class));
                return true;
            } else if (id == R.id.nav_logbook) {
                return true;
            } else if (id == R.id.nav_reviews) {
                startActivity(new Intent(this, MyMarksActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
