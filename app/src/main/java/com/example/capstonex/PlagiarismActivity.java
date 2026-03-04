package com.example.capstonex;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PlagiarismActivity extends BaseActivity {

    private TextInputEditText etSimilarity;
    private MaterialCardView cvStatusPlag;
    private TextView tvPlagStatus, tvSimilarityResult;
    private View llStatusContainer;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plagiarism);
        setupEdgeToEdge(findViewById(R.id.plagiarism_root));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etSimilarity = findViewById(R.id.etSimilarity);
        cvStatusPlag = findViewById(R.id.cvStatusPlag);
        tvPlagStatus = findViewById(R.id.tvPlagStatus);
        tvSimilarityResult = findViewById(R.id.tvSimilarityResult);
        llStatusContainer = findViewById(R.id.llStatusContainer);

        findViewById(R.id.btnSubmitPlag).setOnClickListener(v -> submitPlagiarismData());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        
        loadCurrentStatus();
    }

    private void submitPlagiarismData() {
        String simStr = etSimilarity.getText().toString().trim();
        if (simStr.isEmpty()) {
            etSimilarity.setError("Enter percentage");
            return;
        }

        double similarity = Double.parseDouble(simStr);
        String uid = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("similarity", similarity);
        data.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("users").document(uid).collection("plagiarism")
                .document("latest")
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Report Submitted", Toast.LENGTH_SHORT).show();
                    updateUI(similarity);
                });
    }

    private void loadCurrentStatus() {
        if (mAuth.getCurrentUser() == null) return;
        
        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("plagiarism").document("latest")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double sim = doc.getDouble("similarity");
                        if (sim != null) updateUI(sim);
                    }
                });
    }

    private void updateUI(double similarity) {
        cvStatusPlag.setVisibility(View.VISIBLE);
        tvSimilarityResult.setText("Current Similarity: " + similarity + "%");

        if (similarity < 20) {
            tvPlagStatus.setText("ACCEPTED");
            tvPlagStatus.setTextColor(ContextCompat.getColor(this, R.color.colorAccentGreen));
        } else if (similarity <= 30) {
            tvPlagStatus.setText("WARNING");
            tvPlagStatus.setTextColor(ContextCompat.getColor(this, R.color.colorAccentOrange));
        } else {
            tvPlagStatus.setText("REJECTED");
            tvPlagStatus.setTextColor(ContextCompat.getColor(this, R.color.colorAccentRed));
        }
    }
}
