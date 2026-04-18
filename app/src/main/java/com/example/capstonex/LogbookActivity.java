package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LogbookActivity extends AppCompatActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    
    private RecyclerView rvLogEntries;
    private LogbookAdapter adapter;
    private List<LogEntryModel> logList;
    private TextView tvTotalEntries, tvSignedEntries, tvPendingEntries;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String userGroupId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logbook);

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
        mDatabase.child("Logbook").child(userGroupId).addValueEventListener(new ValueEventListener() {
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
        });
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
