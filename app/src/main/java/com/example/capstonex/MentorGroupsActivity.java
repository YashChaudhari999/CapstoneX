package com.example.capstonex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MentorGroupsActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private RecyclerView rvGroups;
    private GroupAdapter adapter;
    private List<GroupModel> groupList;
    private DatabaseReference mDatabase;
    private String mentorUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mentor_groups);
        setupEdgeToEdge(findViewById(R.id.mentor_groups_root));

        mentorUid = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        
        rvGroups = findViewById(R.id.rvMentorGroups);
        groupList = new ArrayList<>();
        adapter = new GroupAdapter(groupList);
        rvGroups.setAdapter(adapter);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        loadMentorGroups();
    }

    private void loadMentorGroups() {
        mDatabase.child("Groups").orderByChild("mentorUid").equalTo(mentorUid)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupList.clear();
                for (DataSnapshot groupSnap : snapshot.getChildren()) {
                    GroupModel model = groupSnap.getValue(GroupModel.class);
                    if (model != null) {
                        groupList.add(model);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MentorGroupsActivity.this, "Failed to load assigned groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {
        private final List<GroupModel> list;
        GroupAdapter(List<GroupModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mentor_group, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GroupModel group = list.get(position);
            holder.tvId.setText("Group ID: #" + group.getGroupId());
            
            if (group.getMemberDetails() != null && !group.getMemberDetails().isEmpty()) {
                StringBuilder members = new StringBuilder("Members: ");
                for (int i = 0; i < group.getMemberDetails().size(); i++) {
                    members.append(group.getMemberDetails().get(i).get("name"));
                    if (i < group.getMemberDetails().size() - 1) members.append(", ");
                }
                holder.tvMembers.setText(members.toString());
            } else {
                holder.tvMembers.setText("Members: Not assigned");
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvId, tvMembers;
            ViewHolder(View v) {
                super(v);
                tvId = v.findViewById(R.id.tvMentorGroupId);
                tvMembers = v.findViewById(R.id.tvMentorGroupMembers);
            }
        }
    }
}
