package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageGroupsActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private RecyclerView rvGroups;
    private GroupAdapter adapter;
    private List<GroupModel> groupList, filteredList;
    private DatabaseReference mDatabase;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_groups);
        setupEdgeToEdge(findViewById(R.id.manage_groups_root));

        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        rvGroups = findViewById(R.id.rvManageGroups);
        etSearch = findViewById(R.id.etSearchGroups);
        
        groupList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new GroupAdapter(filteredList);
        
        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        rvGroups.setAdapter(adapter);

        findViewById(R.id.fabAddGroup).setOnClickListener(v -> {
            startActivity(new Intent(this, GroupCreationActivity.class));
        });

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadGroups();
    }

    private void loadGroups() {
        mDatabase.child("Groups").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                groupList.clear();
                for (DataSnapshot groupSnap : snapshot.getChildren()) {
                    GroupModel model = groupSnap.getValue(GroupModel.class);
                    if (model != null) {
                        groupList.add(model);
                    }
                }
                filterGroups(etSearch.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageGroupsActivity.this, "Failed to load groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterGroups(String query) {
        filteredList.clear();
        String q = query.toLowerCase();
        for (GroupModel group : groupList) {
            boolean matchesId = group.getGroupId() != null && group.getGroupId().toLowerCase().contains(q);
            boolean matchesMember = false;
            
            if (group.getMemberDetails() != null) {
                for (Map<String, String> member : group.getMemberDetails()) {
                    String sap = member.get("sapId");
                    String name = member.get("name");
                    if ((sap != null && sap.contains(q)) || (name != null && name.toLowerCase().contains(q))) {
                        matchesMember = true;
                        break;
                    }
                }
            }
            
            if (matchesId || matchesMember) {
                filteredList.add(group);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {
        private final List<GroupModel> list;

        GroupAdapter(List<GroupModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_group, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GroupModel group = list.get(position);
            
            holder.tvId.setText("Group ID: " + (group.getGroupId() != null ? group.getGroupId() : "N/A"));
            holder.tvStatus.setText(group.getStatus() != null ? group.getStatus().toUpperCase() : "PENDING");
            
            if (group.getMemberDetails() != null && !group.getMemberDetails().isEmpty()) {
                StringBuilder members = new StringBuilder("Members: ");
                for (int i = 0; i < group.getMemberDetails().size(); i++) {
                    members.append(group.getMemberDetails().get(i).get("name"));
                    if (i < group.getMemberDetails().size() - 1) members.append(", ");
                }
                holder.tvMembers.setText(members.toString());
            } else {
                holder.tvMembers.setText("Members: None");
            }

            // Fetch Mentor Name from UID
            if (group.getMentorUid() != null && !group.getMentorUid().isEmpty()) {
                mDatabase.child("Users").child(group.getMentorUid()).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            holder.tvMentor.setText("Mentor: " + snapshot.getValue(String.class));
                        } else {
                            holder.tvMentor.setText("Mentor: Not Found");
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            } else {
                holder.tvMentor.setText("Mentor: Not Assigned");
            }

            holder.btnDelete.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(ManageGroupsActivity.this)
                        .setTitle("Delete Group")
                        .setMessage("Are you sure you want to delete group " + group.getGroupId() + "? This action cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            deleteGroupAtomically(group);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            holder.btnAssign.setOnClickListener(v -> {
                Intent intent = new Intent(ManageGroupsActivity.this, AssignMentorActivity.class);
                intent.putExtra("groupId", group.getGroupId());
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            });
        }

        private void deleteGroupAtomically(GroupModel group) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("/Groups/" + group.getGroupId(), null);
            if (group.getMemberUids() != null) {
                for (String uid : group.getMemberUids()) {
                    updates.put("/Users/" + uid + "/hasGroup", false);
                    updates.put("/Users/" + uid + "/groupId", "");
                }
            }
            mDatabase.updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ManageGroupsActivity.this, "Group and member assignments cleared", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ManageGroupsActivity.this, "Deletion failed", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvId, tvStatus, tvMembers, tvMentor;
            MaterialButton btnDelete, btnAssign;
            ViewHolder(View v) {
                super(v);
                tvId = v.findViewById(R.id.tvManageGroupId);
                tvStatus = v.findViewById(R.id.tvManageGroupStatus);
                tvMembers = v.findViewById(R.id.tvManageGroupMembers);
                tvMentor = v.findViewById(R.id.tvManageGroupMentor);
                btnDelete = v.findViewById(R.id.btnDeleteGroup);
                btnAssign = v.findViewById(R.id.btnAssignMentor);
            }
        }
    }
}
