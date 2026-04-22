package com.example.capstonex;

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
import java.util.List;

public class ManageMentorsActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private RecyclerView rvMentors;
    private MentorAdapter adapter;
    private List<MentorModel> mentorList;
    private List<MentorModel> filteredList;
    private DatabaseReference mDatabase;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_mentors);
        setupEdgeToEdge(findViewById(R.id.manage_mentors_root));

        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        rvMentors = findViewById(R.id.rvManageMentors);
        etSearch = findViewById(R.id.etSearchMentors);
        
        mentorList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MentorAdapter(filteredList);
        
        rvMentors.setLayoutManager(new LinearLayoutManager(this));
        rvMentors.setAdapter(adapter);

        findViewById(R.id.fabAddMentor).setOnClickListener(v -> {
            Toast.makeText(this, "Feature coming soon: Add Mentor", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMentors(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadMentors();
    }

    private void loadMentors() {
        mDatabase.child("Users").orderByChild("role").equalTo("mentor")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mentorList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    MentorModel model = data.getValue(MentorModel.class);
                    if (model != null) {
                        model.setUid(data.getKey());
                        mentorList.add(model);
                    }
                }
                filterMentors(etSearch.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageMentorsActivity.this, "Failed to load mentors", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterMentors(String query) {
        filteredList.clear();
        String q = query.toLowerCase();
        for (MentorModel mentor : mentorList) {
            boolean matchesName = mentor.getName() != null && mentor.getName().toLowerCase().contains(q);
            boolean matchesDomain = mentor.getDomain() != null && mentor.getDomain().toLowerCase().contains(q);
            
            if (matchesName || matchesDomain) {
                filteredList.add(mentor);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class MentorAdapter extends RecyclerView.Adapter<MentorAdapter.ViewHolder> {
        private final List<MentorModel> list;

        MentorAdapter(List<MentorModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_mentor, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MentorModel mentor = list.get(position);
            holder.tvName.setText(mentor.getName());
            holder.tvDomain.setText("Domain: " + (mentor.getDomain() != null ? mentor.getDomain() : "N/A"));
            
            List<String> groupIds = mentor.getAssignedGroupIds();
            if (groupIds != null && !groupIds.isEmpty()) {
                StringBuilder groups = new StringBuilder("Assigned Groups: ");
                for (int i = 0; i < groupIds.size(); i++) {
                    groups.append(groupIds.get(i));
                    if (i < groupIds.size() - 1) groups.append(", ");
                }
                holder.tvGroups.setText(groups.toString());
            } else {
                holder.tvGroups.setText("Assigned Groups: None");
            }

            holder.btnDelete.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(ManageMentorsActivity.this)
                        .setTitle("Remove Mentor")
                        .setMessage("Are you sure you want to remove " + mentor.getName() + "? This action cannot be undone.")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            mDatabase.child("Users").child(mentor.getUid()).removeValue().addOnSuccessListener(aVoid -> {
                                Toast.makeText(ManageMentorsActivity.this, "Mentor removed", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
            
            holder.btnViewProfile.setOnClickListener(v -> {
                Toast.makeText(ManageMentorsActivity.this, "Profile: " + mentor.getEmail(), Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDomain, tvGroups;
            MaterialButton btnDelete, btnViewProfile;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvMentorName);
                tvDomain = v.findViewById(R.id.tvMentorDomain);
                tvGroups = v.findViewById(R.id.tvAssignedGroups);
                btnDelete = v.findViewById(R.id.btnDeleteMentor);
                btnViewProfile = v.findViewById(R.id.btnViewMentorProfile);
            }
        }
    }
}
