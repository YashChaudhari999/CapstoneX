package com.example.capstonex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RubricManagementActivity extends BaseActivity {

    private RecyclerView rvRubrics;
    private RubricAdapter adapter;
    private List<RubricModel> rubricList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rubric_management);
        setupEdgeToEdge(findViewById(R.id.rubric_root));

        db = FirebaseFirestore.getInstance();
        rvRubrics = findViewById(R.id.rvRubrics);
        rvRubrics.setLayoutManager(new LinearLayoutManager(this));
        
        rubricList = new ArrayList<>();
        adapter = new RubricAdapter(rubricList, rubric -> deleteRubric(rubric));
        rvRubrics.setAdapter(adapter);

        findViewById(R.id.fabAddRubric).setOnClickListener(v -> showAddRubricDialog());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        loadRubrics();
    }

    private void loadRubrics() {
        db.collection("rubrics").get().addOnSuccessListener(queryDocumentSnapshots -> {
            rubricList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                RubricModel model = doc.toObject(RubricModel.class);
                model.setId(doc.getId());
                rubricList.add(model);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showAddRubricDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_rubric, null);
        EditText etCriteria = view.findViewById(R.id.etCriteria);
        EditText etMaxMarks = view.findViewById(R.id.etMaxMarks);

        new AlertDialog.Builder(this)
                .setTitle("Add Evaluation Criteria")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String criteria = etCriteria.getText().toString().trim();
                    String marks = etMaxMarks.getText().toString().trim();
                    if (!criteria.isEmpty() && !marks.isEmpty()) {
                        addRubricToFirestore(criteria, Integer.parseInt(marks));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addRubricToFirestore(String criteria, int maxMarks) {
        RubricModel rubric = new RubricModel(criteria, maxMarks);
        db.collection("rubrics").add(rubric).addOnSuccessListener(ref -> loadRubrics());
    }

    private void deleteRubric(RubricModel rubric) {
        db.collection("rubrics").document(rubric.getId()).delete().addOnSuccessListener(aVoid -> loadRubrics());
    }
}
