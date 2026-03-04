package com.example.capstonex;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class DocumentsActivity extends BaseActivity {

    private static final int PICK_FILE_REQUEST = 2;
    private String currentUploadType = "";
    
    private RecyclerView rvDocuments;
    private DocumentAdapter adapter;
    private List<DocumentModel> documentList;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);
        setupEdgeToEdge(findViewById(R.id.documents_root));

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        rvDocuments = findViewById(R.id.rvDocuments);
        rvDocuments.setLayoutManager(new LinearLayoutManager(this));
        documentList = new ArrayList<>();
        adapter = new DocumentAdapter(documentList);
        rvDocuments.setAdapter(adapter);

        findViewById(R.id.btnUploadProposal).setOnClickListener(v -> selectFile("Proposal"));
        findViewById(R.id.btnUploadPPT).setOnClickListener(v -> selectFile("PPT"));
        findViewById(R.id.btnUploadCode).setOnClickListener(v -> selectFile("SourceCode"));

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        loadDocuments();
    }

    private void selectFile(String type) {
        currentUploadType = type;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadFileToFirebase(data.getData());
        }
    }

    private void uploadFileToFirebase(Uri fileUri) {
        String uid = mAuth.getCurrentUser().getUid();
        String fileName = currentUploadType + "_" + System.currentTimeMillis();
        StorageReference fileRef = storage.getReference().child("documents/" + uid + "/" + fileName);

        fileRef.putFile(fileUri).addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            saveDocumentMetadata(currentUploadType, uri.toString());
        })).addOnFailureListener(e -> Toast.makeText(this, "Upload Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveDocumentMetadata(String type, String url) {
        String uid = mAuth.getCurrentUser().getUid();
        DocumentModel doc = new DocumentModel();
        doc.setTitle(type + " Submission");
        doc.setType(type);
        doc.setDownloadUrl(url);
        doc.setTimestamp(Timestamp.now());

        db.collection("users").document(uid).collection("documents")
                .add(doc)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Document Uploaded", Toast.LENGTH_SHORT).show();
                    loadDocuments();
                });
    }

    private void loadDocuments() {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).collection("documents")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    documentList.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        DocumentModel model = doc.toObject(DocumentModel.class);
                        model.setId(doc.getId());
                        documentList.add(model);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
