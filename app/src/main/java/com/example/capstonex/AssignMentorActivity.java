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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class  AssignMentorActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private RecyclerView rvMentors;
    private MentorAdapter adapter;
    private List<UserModel> mentorList, filteredList;
    private DatabaseReference mDatabase;
    private String targetGroupId;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assign_mentor);
        setupEdgeToEdge(findViewById(R.id.toolbar));

        targetGroupId = getIntent().getStringExtra("groupId");
        if (targetGroupId == null) {
            Toast.makeText(this, "Error: No Group ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        rvMentors = findViewById(R.id.rvMentors);
        etSearch = findViewById(R.id.etSearchMentors);

        mentorList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MentorAdapter(filteredList);

        rvMentors.setLayoutManager(new LinearLayoutManager(this));
        rvMentors.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMentors(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadMentors();
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void loadMentors() {
        mDatabase.child("Users").orderByChild("role").equalTo("mentor")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mentorList.clear();
                for (DataSnapshot mentorSnap : snapshot.getChildren()) {
                    UserModel user = mentorSnap.getValue(UserModel.class);
                    if (user != null) {
                        user.setUid(mentorSnap.getKey());
                        mentorList.add(user);
                    }
                }
                filterMentors(etSearch.getText().toString());
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterMentors(String query) {
        filteredList.clear();
        for (UserModel mentor : mentorList) {
            if (mentor.getName().toLowerCase().contains(query.toLowerCase()) || 
                (mentor.getDomain() != null && mentor.getDomain().toLowerCase().contains(query.toLowerCase()))) {
                filteredList.add(mentor);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void performAssignment(UserModel mentor) {
        Map<String, Object> updates = new HashMap<>();
        
        // 1. Update Group node with Mentor UID
        updates.put("/Groups/" + targetGroupId + "/mentorUid", mentor.getUid());
        
        // 2. Add Group ID to Mentor's list (Simplified: assuming list or simple string for now)
        // In a real app, you'd fetch the existing list first. Here we assume a simple update.
        updates.put("/Users/" + mentor.getUid() + "/currentAssignedGroup", targetGroupId);

        mDatabase.updateChildren(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Mentor " + mentor.getName() + " assigned successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private class MentorAdapter extends RecyclerView.Adapter<MentorAdapter.ViewHolder> {
        private final List<UserModel> list;
        MentorAdapter(List<UserModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assign_mentor, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserModel mentor = list.get(position);
            holder.tvName.setText(mentor.getName());
            holder.tvDomain.setText("Domain: " + (mentor.getDomain() != null ? mentor.getDomain() : "General"));
            holder.btnAssign.setOnClickListener(v -> performAssignment(mentor));
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvDomain;
            MaterialButton btnAssign;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvMentorName);
                tvDomain = v.findViewById(R.id.tvMentorDomain);
                btnAssign = v.findViewById(R.id.btnConfirmAssign);
            }
        }
    }
}
