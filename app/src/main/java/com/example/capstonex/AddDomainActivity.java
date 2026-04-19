package com.example.capstonex;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddDomainActivity extends BaseActivity {

    private TextInputEditText etDomainName;
    private RecyclerView rvDomains;
    private DomainAdapter adapter;
    private List<DomainModel> domainList;
    private FirebaseFirestore db;
    
    private MaterialCardView cardAddDomainSection;
    private MaterialButton btnAddDomain, btnSubmitDomains, btnEditDomains;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_domain);
        setupEdgeToEdge(findViewById(R.id.add_domain_root));

        db = FirebaseFirestore.getInstance();
        etDomainName = findViewById(R.id.etDomainName);
        rvDomains = findViewById(R.id.rvDomains);
        cardAddDomainSection = findViewById(R.id.cardAddDomainSection);
        btnAddDomain = findViewById(R.id.btnAddDomain);
        btnSubmitDomains = findViewById(R.id.btnSubmitDomains);
        btnEditDomains = findViewById(R.id.btnEditDomains);
        
        domainList = new ArrayList<>();
        adapter = new DomainAdapter(domainList);
        rvDomains.setLayoutManager(new LinearLayoutManager(this));
        rvDomains.setAdapter(adapter);

        btnAddDomain.setOnClickListener(v -> addDomain());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
        
        btnSubmitDomains.setOnClickListener(v -> submitDomains());
        btnEditDomains.setOnClickListener(v -> enableEditing());

        checkSubmissionStatus();
        loadDomains();
    }

    private void addDomain() {
        String name = etDomainName.getText().toString().trim();
        if (name.isEmpty()) {
            etDomainName.setError("Enter domain name");
            return;
        }

        Map<String, Object> domain = new HashMap<>();
        domain.put("name", name);
        domain.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("domains").add(domain)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Domain Added", Toast.LENGTH_SHORT).show();
                    etDomainName.setText("");
                    loadDomains();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to add domain: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void submitDomains() {
        if (domainList.isEmpty()) {
            Toast.makeText(this, "Please add at least one domain before submitting", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> status = new HashMap<>();
        status.put("isSubmitted", true);

        db.collection("settings").document("domain_status")
                .set(status)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Domains finalized and submitted!", Toast.LENGTH_SHORT).show();
                    setViewState(true);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Submission failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void enableEditing() {
        Map<String, Object> status = new HashMap<>();
        status.put("isSubmitted", false);

        // Using set with merge is safer if the document state is uncertain
        db.collection("settings").document("domain_status")
                .set(status, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Editing enabled", Toast.LENGTH_SHORT).show();
                    setViewState(false);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to enable editing: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void checkSubmissionStatus() {
        db.collection("settings").document("domain_status")
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Boolean isSubmitted = documentSnapshot.getBoolean("isSubmitted");
                        setViewState(isSubmitted != null && isSubmitted);
                    }
                });
    }

    private void setViewState(boolean isSubmitted) {
        if (isSubmitted) {
            cardAddDomainSection.setVisibility(View.GONE);
            btnSubmitDomains.setVisibility(View.GONE);
            btnEditDomains.setVisibility(View.VISIBLE);
            adapter.setCanDelete(false);
        } else {
            cardAddDomainSection.setVisibility(View.VISIBLE);
            btnSubmitDomains.setVisibility(View.VISIBLE);
            btnEditDomains.setVisibility(View.GONE);
            adapter.setCanDelete(true);
        }
    }

    private void loadDomains() {
        db.collection("domains").orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    domainList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        DomainModel model = doc.toObject(DomainModel.class);
                        model.setId(doc.getId());
                        domainList.add(model);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private class DomainAdapter extends RecyclerView.Adapter<DomainAdapter.ViewHolder> {
        private List<DomainModel> list;
        private boolean canDelete = true;
        
        DomainAdapter(List<DomainModel> list) { this.list = list; }

        void setCanDelete(boolean canDelete) {
            this.canDelete = canDelete;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_domain, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DomainModel domain = list.get(position);
            holder.tvName.setText(domain.getName());
            holder.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
            holder.btnDelete.setOnClickListener(v -> {
                db.collection("domains").document(domain.getId()).delete().addOnSuccessListener(aVoid -> loadDomains());
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            ImageButton btnDelete;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvDomainName);
                btnDelete = v.findViewById(R.id.btnDeleteDomain);
            }
        }
    }
}
