package com.example.capstonex;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NotificationsActivity — CapstonX
 * <p>
 * FIX BUG-001: Added null check for getCurrentUser() before every access.
 * FIX BUG-002: Switched from Firestore to Firebase Realtime DB to match where
 * GroupCreationActivity writes notifications (/Notifications/{nid}).
 * FIX BUG-007: Permanent ValueEventListener stored and removed in onDestroy()
 * to prevent memory leaks.
 */
public class NotificationsActivity extends BaseActivity {

    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    // ── BUG-007 FIX: store listener reference so we can remove it ─────────
    private ValueEventListener notifListener;
    private Query notifRef; // Changed from DatabaseReference to Query

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        setupEdgeToEdge(findViewById(R.id.notif_root));

        mAuth = FirebaseAuth.getInstance();

        // ── BUG-001 FIX: guard against null user ──────────────────────────
        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        // ── BUG-002 FIX: use Realtime DB (not Firestore) ──────────────────
        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();

        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setHasFixedSize(true);

        notificationList = new ArrayList<>();

        adapter = new NotificationAdapter(notificationList, notification -> {
            // ── BUG-001 FIX: null-safe access inside lambda ────────────────
            if (mAuth.getCurrentUser() == null || notification.getId() == null) return;
            mDatabase.child("Notifications")
                    .child(notification.getId())
                    .child("isRead")
                    .setValue(true);
        });

        rvNotifications.setAdapter(adapter);
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        listenForNotifications();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUG-002 FIX: Read from Realtime DB path /Notifications, filter by userId
    // BUG-007 FIX: Store listener reference for cleanup in onDestroy()
    // ─────────────────────────────────────────────────────────────────────────
    private void listenForNotifications() {
        String uid = mAuth.getCurrentUser().getUid(); // safe — guarded in onCreate

        // .equalTo() returns a Query, so notifRef must be of type Query
        notifRef = mDatabase.child("Notifications").orderByChild("userId").equalTo(uid);

        notifListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    NotificationModel model = ds.getValue(NotificationModel.class);
                    if (model != null) {
                        model.setId(ds.getKey());
                        notificationList.add(model);
                    }
                }
                // Show newest notifications first
                Collections.reverse(notificationList);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(NotificationsActivity.this,
                        "Could not load notifications", Toast.LENGTH_SHORT).show();
            }
        };

        notifRef.addValueEventListener(notifListener);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUG-007 FIX: Remove listener on destroy to prevent memory leak
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notifListener != null && notifRef != null) {
            notifRef.removeEventListener(notifListener);
        }
    }
}
