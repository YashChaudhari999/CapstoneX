package com.example.capstonex;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageStudentsActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private RecyclerView rvStudents;
    private StudentAdapter adapter;
    private List<UserModel> studentList, filteredList;
    private DatabaseReference mDatabase;
    private EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_students);
        setupEdgeToEdge(findViewById(R.id.manage_students_root));

        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();
        rvStudents = findViewById(R.id.rvManageStudents);
        etSearch = findViewById(R.id.etSearchStudents);
        
        studentList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new StudentAdapter(filteredList);
        
        rvStudents.setLayoutManager(new LinearLayoutManager(this));
        rvStudents.setAdapter(adapter);

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterStudents(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadStudents();
    }

    private void loadStudents() {
        mDatabase.child("Users").orderByChild("role").equalTo("student")
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                studentList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    UserModel user = data.getValue(UserModel.class);
                    if (user != null) {
                        user.setUid(data.getKey());
                        studentList.add(user);
                    }
                }
                filterStudents(etSearch.getText().toString());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterStudents(String query) {
        filteredList.clear();
        String q = query.toLowerCase();
        for (UserModel student : studentList) {
            if (student.getName().toLowerCase().contains(q) || 
                (student.getSapId() != null && student.getSapId().contains(q)) ||
                (student.getRollNo() != null && student.getRollNo().toLowerCase().contains(q))) {
                filteredList.add(student);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {
        private final List<UserModel> list;
        StudentAdapter(List<UserModel> list) { this.list = list; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_student, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserModel student = list.get(position);
            holder.tvName.setText(student.getName());
            holder.tvSap.setText("SAP ID: " + (student.getSapId() != null ? student.getSapId() : "N/A"));
            holder.tvRoll.setText("Roll No: " + (student.getRollNo() != null ? student.getRollNo() : "N/A"));
            holder.tvGroup.setText("Group: " + (student.getGroupId() != null && !student.getGroupId().isEmpty() ? student.getGroupId() : "Not Assigned"));

            if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isEmpty()) {
                Glide.with(ManageStudentsActivity.this).load(student.getProfileImageUrl()).circleCrop().into(holder.ivAvatar);
            }

            holder.btnDelete.setOnClickListener(v -> {
                mDatabase.child("Users").child(student.getUid()).removeValue().addOnSuccessListener(aVoid -> {
                    Toast.makeText(ManageStudentsActivity.this, "Student removed", Toast.LENGTH_SHORT).show();
                });
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSap, tvRoll, tvGroup;
            ImageView ivAvatar;
            MaterialButton btnDelete;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvStudentName);
                tvSap = v.findViewById(R.id.tvStudentSap);
                tvRoll = v.findViewById(R.id.tvStudentRoll);
                tvGroup = v.findViewById(R.id.tvStudentGroup);
                ivAvatar = v.findViewById(R.id.ivStudentAvatar);
                btnDelete = v.findViewById(R.id.btnDeleteStudent);
            }
        }
    }
}
