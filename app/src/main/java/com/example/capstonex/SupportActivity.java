package com.example.capstonex;

import android.os.Bundle;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class SupportActivity extends BaseActivity {

    private TextInputEditText etSubject, etDesc;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);
        setupEdgeToEdge(findViewById(R.id.support_root));

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etSubject = findViewById(R.id.etTicketSubject);
        etDesc = findViewById(R.id.etTicketDesc);

        findViewById(R.id.btnSubmitTicket).setOnClickListener(v -> submitTicket());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void submitTicket() {
        String subject = etSubject.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        if (subject.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> ticket = new HashMap<>();
        ticket.put("uid", mAuth.getUid());
        ticket.put("subject", subject);
        ticket.put("description", desc);
        ticket.put("status", "Open");
        ticket.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("tickets").add(ticket)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Ticket raised successfully", Toast.LENGTH_SHORT).show();
                    etSubject.setText("");
                    etDesc.setText("");
                });
    }
}
