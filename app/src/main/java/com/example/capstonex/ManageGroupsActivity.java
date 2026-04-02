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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManageGroupsActivity extends BaseActivity {

    private RecyclerView rvGroups;
    private GroupAdapter adapter;
    private List<GroupModel> groupList;
    private List<GroupModel> filteredList;
    private FirebaseFirestore db;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_groups);
        setupEdgeToEdge(findViewById(R.id.manage_groups_root));

        db = FirebaseFirestore.getInstance();
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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadGroups();
    }

    private void loadGroups() {
        db.collection("groups").get().addOnSuccessListener(queryDocumentSnapshots -> {
            groupList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                GroupModel model = doc.toObject(GroupModel.class);
                model.setId(doc.getId());
                groupList.add(model);
            }
            filterGroups(etSearch.getText().toString());
        });
    }

    private void filterGroups(String query) {
        filteredList.clear();
        for (GroupModel group : groupList) {
            if (group.getGroupName() != null && group.getGroupName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(group);
            } else {
                for (String sap : group.getMembers()) {
                    if (sap.contains(query)) {
                        filteredList.add(group);
                        break;
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.ViewHolder> {
        private List<GroupModel> list;

        GroupAdapter(List<GroupModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_group, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            GroupModel group = list.get(position);
            holder.tvName.setText(group.getGroupName() != null ? group.getGroupName() : "Group " + group.getId().substring(0, 5));
            holder.tvStatus.setText(group.getStatus() != null ? group.getStatus().toUpperCase() : "PENDING");
            
            StringBuilder members = new StringBuilder("Members: ");
            for (String sap : group.getMembers()) members.append(sap).append(", ");
            holder.tvMembers.setText(members.toString().substring(0, members.length() - 2));

            holder.btnDelete.setOnClickListener(v -> {
                db.collection("groups").document(group.getId()).delete().addOnSuccessListener(aVoid -> loadGroups());
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvStatus, tvMembers;
            MaterialButton btnDelete;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvManageGroupName);
                tvStatus = v.findViewById(R.id.tvManageGroupStatus);
                tvMembers = v.findViewById(R.id.tvManageGroupMembers);
                btnDelete = v.findViewById(R.id.btnDeleteGroup);
            }
        }
    }
}
