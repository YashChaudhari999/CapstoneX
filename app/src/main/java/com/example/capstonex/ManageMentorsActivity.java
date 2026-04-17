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

public class ManageMentorsActivity extends BaseActivity {

    private RecyclerView rvMentors;
    private MentorAdapter adapter;
    private List<MentorModel> mentorList;
    private List<MentorModel> filteredList;
    private FirebaseFirestore db;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_mentors);
        setupEdgeToEdge(findViewById(R.id.manage_mentors_root));

        db = FirebaseFirestore.getInstance();
        rvMentors = findViewById(R.id.rvManageMentors);
        etSearch = findViewById(R.id.etSearchMentors);
        
        mentorList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MentorAdapter(filteredList);
        
        rvMentors.setLayoutManager(new LinearLayoutManager(this));
        rvMentors.setAdapter(adapter);

        findViewById(R.id.fabAddMentor).setOnClickListener(v -> {
            // Future: Open Add Mentor Dialog or Activity
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
        db.collection("mentors").get().addOnSuccessListener(queryDocumentSnapshots -> {
            mentorList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                MentorModel model = doc.toObject(MentorModel.class);
                model.setId(doc.getId());
                mentorList.add(model);
            }
            filterMentors(etSearch.getText().toString());
        });
    }

    private void filterMentors(String query) {
        filteredList.clear();
        for (MentorModel mentor : mentorList) {
            if (mentor.getName() != null && mentor.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(mentor);
            } else if (mentor.getDomain() != null && mentor.getDomain().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(mentor);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class MentorAdapter extends RecyclerView.Adapter<MentorAdapter.ViewHolder> {
        private List<MentorModel> list;

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
                for (String gid : groupIds) groups.append(gid).append(", ");
                holder.tvGroups.setText(groups.toString().substring(0, groups.length() - 2));
            } else {
                holder.tvGroups.setText("Assigned Groups: None");
            }

            holder.btnDelete.setOnClickListener(v -> {
                db.collection("mentors").document(mentor.getId()).delete().addOnSuccessListener(aVoid -> loadMentors());
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDomain, tvGroups;
            MaterialButton btnDelete;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvMentorName);
                tvDomain = v.findViewById(R.id.tvMentorDomain);
                tvGroups = v.findViewById(R.id.tvAssignedGroups);
                btnDelete = v.findViewById(R.id.btnDeleteMentor);
            }
        }
    }
}
