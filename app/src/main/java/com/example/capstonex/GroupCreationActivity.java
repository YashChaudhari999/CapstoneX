package com.example.capstonex;

import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class GroupCreationActivity extends BaseActivity {

    private TextInputEditText etGroupName, etLeaderSap, etMember2Sap, etMember3Sap;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_creation);
        setupEdgeToEdge(findViewById(R.id.group_creation_root));

        db = FirebaseFirestore.getInstance();

        etGroupName = findViewById(R.id.etGroupName);
        etLeaderSap = findViewById(R.id.etLeaderSap);
        etMember2Sap = findViewById(R.id.etMember2Sap);
        etMember3Sap = findViewById(R.id.etMember3Sap);

        findViewById(R.id.btnCreateGroup).setOnClickListener(v -> createGroup());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void createGroup() {
        String name = etGroupName.getText().toString().trim();
        String s1 = etLeaderSap.getText().toString().trim();
        String s2 = etMember2Sap.getText().toString().trim();
        String s3 = etMember3Sap.getText().toString().trim();

        if (name.isEmpty() || s1.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> group = new HashMap<>();
        group.put("name", name);
        group.put("members", Arrays.asList(s1, s2, s3));
        group.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("groups").add(group)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Group Created Successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
