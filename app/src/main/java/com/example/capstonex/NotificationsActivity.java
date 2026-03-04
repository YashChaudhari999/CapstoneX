package com.example.capstonex;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends BaseActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<NotificationModel> notificationList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        setupEdgeToEdge(findViewById(R.id.notif_root));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        
        adapter = new NotificationAdapter(notificationList, notification -> {
            // Mark as read in Firestore
            db.collection("users").document(mAuth.getCurrentUser().getUid())
                    .collection("notifications").document(notification.getId())
                    .update("read", true);
        });
        
        rvNotifications.setAdapter(adapter);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        listenForNotifications();
    }

    private void listenForNotifications() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            NotificationModel model = doc.toObject(NotificationModel.class);
                            model.setId(doc.getId());
                            notificationList.add(model);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
