package com.example.capstonex;

import android.os.Bundle;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProjectTimelineActivity extends BaseActivity {

    private RecyclerView rvTimeline;
    private TimelineAdapter adapter;
    private List<TimelineModel> timelineList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_timeline);
        setupEdgeToEdge(findViewById(R.id.timeline_root));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvTimeline = findViewById(R.id.rvTimeline);
        rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        
        timelineList = new ArrayList<>();
        adapter = new TimelineAdapter(timelineList);
        rvTimeline.setAdapter(adapter);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        fetchTimelineData();
    }

    private void fetchTimelineData() {
        String uid = mAuth.getCurrentUser().getUid();
        // Assuming milestones are stored under a collection linked to the user or their group
        db.collection("milestones")
                .whereEqualTo("studentUid", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    timelineList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        TimelineModel model = document.toObject(TimelineModel.class);
                        model.setId(document.getId());
                        timelineList.add(model);
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (timelineList.isEmpty()) {
                        // Add some dummy data if empty for demonstration
                        addDummyMilestones();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addDummyMilestones() {
        timelineList.add(new TimelineModel("1", "Topic Approval", "15 Aug 2024", "Completed", 100));
        timelineList.add(new TimelineModel("2", "Proposal Submission", "30 Aug 2024", "Pending", 40));
        timelineList.add(new TimelineModel("3", "Review 1", "15 Oct 2024", "Pending", 0));
        timelineList.add(new TimelineModel("4", "Review 2", "20 Dec 2024", "Pending", 0));
        timelineList.add(new TimelineModel("5", "Final Report", "10 Feb 2025", "Pending", 0));
        timelineList.add(new TimelineModel("6", "Viva", "20 Mar 2025", "Pending", 0));
        adapter.notifyDataSetChanged();
    }
}
