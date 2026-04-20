package com.example.capstonex;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddDomainActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private final List<DomainModel> domainList = new ArrayList<>();
    
    private TextInputLayout tilDomainName;
    private TextInputEditText etDomainName;
    private MaterialCardView cardAddDomainSection;
    private MaterialButton btnAddDomain;
    private RecyclerView rvDomains;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvDomainCount;
    private MaterialButton btnSubmitDomains;
    private MaterialButton btnEditDomains;

    private DomainAdapter adapter;
    private DatabaseReference mDatabase;
    private ValueEventListener domainsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_domain);

        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();

        bindViews();
        setupRecyclerView();
        setupClickListeners();
        loadExistingDomains();
    }

    private void bindViews() {
        tilDomainName = findViewById(R.id.tilDomainName);
        etDomainName = findViewById(R.id.etDomainName);
        cardAddDomainSection = findViewById(R.id.cardAddDomainSection);
        btnAddDomain = findViewById(R.id.btnAddDomain);
        rvDomains = findViewById(R.id.rvDomains);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvDomainCount = findViewById(R.id.tvDomainCount);
        btnSubmitDomains = findViewById(R.id.btnSubmitDomains);
        btnEditDomains = findViewById(R.id.btnEditDomains);
    }

    private void setupRecyclerView() {
        adapter = new DomainAdapter(domainList, this::onDeleteRequested);
        rvDomains.setLayoutManager(new LinearLayoutManager(this));
        rvDomains.setAdapter(adapter);
    }

    private void setupClickListeners() {
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        btnAddDomain.setOnClickListener(v -> attemptAddDomain());
        btnSubmitDomains.setOnClickListener(v -> confirmAndSubmit());
        btnEditDomains.setOnClickListener(v -> toggleEditing(true));

        etDomainName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { tilDomainName.setError(null); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadExistingDomains() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("Domains").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progressBar.setVisibility(View.GONE);
                domainList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    DomainModel domain = data.getValue(DomainModel.class);
                    if (domain != null) {
                        domainList.add(domain);
                    }
                }
                adapter.notifyDataSetChanged();
                updateCountLabel();
                tvEmptyState.setVisibility(domainList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddDomainActivity.this, "Failed to load domains", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attemptAddDomain() {
        String name = etDomainName.getText().toString().trim();

        if (name.isEmpty()) {
            tilDomainName.setError("Please enter a domain name");
            return;
        }

        // Add to local list only
        DomainModel newDomain = new DomainModel(null, name);
        domainList.add(newDomain);
        adapter.notifyDataSetChanged();
        
        etDomainName.setText("");
        updateCountLabel();
        tvEmptyState.setVisibility(View.GONE);
        Toast.makeText(this, "Domain added to list", Toast.LENGTH_SHORT).show();
    }

    private void onDeleteRequested(DomainModel domain) {
        domainList.remove(domain);
        adapter.notifyDataSetChanged();
        updateCountLabel();
        if (domainList.isEmpty()) tvEmptyState.setVisibility(View.VISIBLE);
    }

    private void confirmAndSubmit() {
        if (domainList.isEmpty()) {
            Toast.makeText(this, "Add at least one domain", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle("Save Domains")
                .setMessage("Save all current domains to the database?")
                .setPositiveButton("Save", (d, w) -> saveToDatabase())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveToDatabase() {
        btnSubmitDomains.setEnabled(false);
        btnSubmitDomains.setText("Saving...");

        // Overwrite the Domains node with the current local list
        mDatabase.child("Domains").setValue(domainList).addOnCompleteListener(task -> {
            btnSubmitDomains.setEnabled(true);
            btnSubmitDomains.setText("SUBMIT DOMAINS");
            if (task.isSuccessful()) {
                Toast.makeText(this, "Domains saved successfully!", Toast.LENGTH_SHORT).show();
                toggleEditing(false);
            } else {
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleEditing(boolean enabled) {
        cardAddDomainSection.setVisibility(enabled ? View.VISIBLE : View.GONE);
        btnSubmitDomains.setVisibility(enabled ? View.VISIBLE : View.GONE);
        btnEditDomains.setVisibility(enabled ? View.GONE : View.VISIBLE);
        adapter.setCanDelete(enabled);
        adapter.notifyDataSetChanged();
    }

    private void updateCountLabel() {
        int n = domainList.size();
        tvDomainCount.setText(n + (n == 1 ? " domain" : " domains") + " added");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // --- Adapter ---
    interface OnDeleteListener { void onDelete(DomainModel domain); }

    private static class DomainAdapter extends RecyclerView.Adapter<DomainAdapter.ViewHolder> {
        private final List<DomainModel> list;
        private final OnDeleteListener deleteListener;
        private boolean canDelete = true;

        DomainAdapter(List<DomainModel> list, OnDeleteListener listener) {
            this.list = list;
            this.deleteListener = listener;
        }

        void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new ViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_domain, p, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder h, int p) {
            DomainModel domain = list.get(p);
            h.tvName.setText((p + 1) + ". " + domain.getName());
            h.btnDelete.setVisibility(canDelete ? View.VISIBLE : View.GONE);
            h.btnDelete.setOnClickListener(v -> deleteListener.onDelete(domain));
        }

        @Override public int getItemCount() { return list.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName; ImageButton btnDelete;
            ViewHolder(View v) { super(v); tvName = v.findViewById(R.id.tvDomainName); btnDelete = v.findViewById(R.id.btnDeleteDomain); }
        }
    }
}
