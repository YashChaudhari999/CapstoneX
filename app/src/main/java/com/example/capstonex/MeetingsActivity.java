package com.example.capstonex;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MeetingsActivity extends BaseActivity {

    private RecyclerView rvMeetings;
    private List<MeetingModel> meetingList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meetings);
        setupEdgeToEdge(findViewById(R.id.meetings_root));

        db = FirebaseFirestore.getInstance();
        rvMeetings = findViewById(R.id.rvMeetings);
        rvMeetings.setLayoutManager(new LinearLayoutManager(this));
        
        meetingList = new ArrayList<>();
        // Simple Adapter inline for brevity or separate as needed
        rvMeetings.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(getLayoutInflater().inflate(R.layout.item_meeting, parent, false)) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                MeetingModel m = meetingList.get(position);
                ((TextView)holder.itemView.findViewById(R.id.tvMeetingTitle)).setText(m.getTitle());
            }

            @Override
            public int getItemCount() { return meetingList.size(); }
        });

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        fetchMeetings();
    }

    private void fetchMeetings() {
        db.collection("meetings").orderBy("dateTime", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        meetingList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            meetingList.add(doc.toObject(MeetingModel.class));
                        }
                        if (rvMeetings.getAdapter() != null) {
                            rvMeetings.getAdapter().notifyDataSetChanged();
                        }
                    }
                });
    }
}
