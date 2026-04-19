package com.example.capstonex;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

public class AddLogActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;
    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private static final String TAG = "AddLogActivity";

    private TextInputEditText etWorkDone, etFromDate, etToDate;
    private MaterialButton btnUploadDocument, btnCancel, btnAddEntry;
    private TextView tvSelectedDocName;
    
    private Uri pdfUri;
    private String documentUrl = "";
    private String documentName = "";
    private String userGroupId = "";
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_log_entry);
        
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DB_URL).getReference();

        etWorkDone = findViewById(R.id.etWorkDone);
        etFromDate = findViewById(R.id.etFromDate);
        etToDate = findViewById(R.id.etToDate);
        btnUploadDocument = findViewById(R.id.btnUploadDocument);
        tvSelectedDocName = findViewById(R.id.tvSelectedDocName);
        btnCancel = findViewById(R.id.btnCancel);
        btnAddEntry = findViewById(R.id.btnAddEntry);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // Pre-fetch group info immediately
        fetchUserGroupId(null);

        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));
        btnUploadDocument.setOnClickListener(v -> openFilePicker());
        btnCancel.setOnClickListener(v -> finish());
        btnAddEntry.setOnClickListener(v -> attemptSaveEntry());
    }

    private void fetchUserGroupId(Runnable onComplete) {
        String uid = mAuth.getUid();
        if (uid == null) { if (onComplete != null) onComplete.run(); return; }

        mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (userSnapshot.exists()) {
                    userGroupId = userSnapshot.child("groupId").getValue(String.class);
                    if (userGroupId != null && !userGroupId.isEmpty()) {
                        if (onComplete != null) onComplete.run();
                    } else {
                        String mySap = String.valueOf(userSnapshot.child("sapId").getValue());
                        deepSearchGroups(uid, mySap, onComplete);
                    }
                } else if (onComplete != null) onComplete.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { if (onComplete != null) onComplete.run(); }
        });
    }

    private void deepSearchGroups(String myUid, String mySap, Runnable onComplete) {
        mDatabase.child("Groups").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot groupsSnapshot) {
                for (DataSnapshot group : groupsSnapshot.getChildren()) {
                    String leaderUid = group.child("leaderUid").getValue(String.class);
                    if (myUid.equals(leaderUid)) { saveFoundGroupId(group.getKey(), onComplete); return; }
                    DataSnapshot uids = group.child("memberUids");
                    for (DataSnapshot idSnap : uids.getChildren()) {
                        if (myUid.equals(idSnap.getValue(String.class))) { saveFoundGroupId(group.getKey(), onComplete); return; }
                    }
                    DataSnapshot details = group.child("memberDetails");
                    for (DataSnapshot member : details.getChildren()) {
                        String sap = String.valueOf(member.child("sapId").getValue());
                        if (mySap.equals(sap)) { saveFoundGroupId(group.getKey(), onComplete); return; }
                    }
                }
                if (onComplete != null) onComplete.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { if (onComplete != null) onComplete.run(); }
        });
    }

    private void saveFoundGroupId(String groupId, Runnable onComplete) {
        this.userGroupId = groupId;
        mDatabase.child("Users").child(mAuth.getUid()).child("groupId").setValue(groupId);
        mDatabase.child("Users").child(mAuth.getUid()).child("hasGroup").setValue(true);
        if (onComplete != null) onComplete.run();
    }

    private void showDatePicker(TextInputEditText editText) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year);
            editText.setText(date);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), PICK_PDF_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            pdfUri = data.getData();
            documentName = "log_doc_" + System.currentTimeMillis() + ".pdf";
            tvSelectedDocName.setText(documentName);
        }
    }

    private void attemptSaveEntry() {
        String workDone = etWorkDone.getText().toString().trim();
        String from = etFromDate.getText().toString().trim();
        String to = etToDate.getText().toString().trim();

        if (workDone.isEmpty() || from.isEmpty() || to.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userGroupId == null || userGroupId.isEmpty()) {
            progressDialog.setMessage("Verifying group...");
            progressDialog.show();
            fetchUserGroupId(() -> {
                progressDialog.dismiss();
                if (userGroupId == null || userGroupId.isEmpty()) {
                    Toast.makeText(this, "Group allocation not found.", Toast.LENGTH_LONG).show();
                } else {
                    processUploadAndSave(workDone, from, to);
                }
            });
        } else {
            processUploadAndSave(workDone, from, to);
        }
    }

    private void processUploadAndSave(String workDone, String from, String to) {
        if (pdfUri != null) {
            uploadDocument(workDone, from, to);
        } else {
            saveToFirebase(workDone, from, to, "", "");
        }
    }

    private void uploadDocument(String workDone, String from, String to) {
        progressDialog.setMessage("Uploading to Cloudinary...");
        progressDialog.show();

        MediaManager.get().upload(pdfUri)
                .unsigned("Massanger") 
                .callback(new UploadCallback() {
                    @Override public void onStart(String id) { Log.d(TAG, "Upload Started"); }
                    @Override public void onProgress(String id, long b, long t) {}
                    @Override public void onSuccess(String id, Map resultData) {
                        Log.d(TAG, "Upload Success: " + resultData.get("secure_url"));
                        documentUrl = resultData.get("secure_url").toString();
                        saveToFirebase(workDone, from, to, documentUrl, documentName);
                    }
                    @Override public void onError(String id, ErrorInfo error) {
                        progressDialog.dismiss();
                        Log.e(TAG, "Upload Failed: " + error.getDescription());
                        Toast.makeText(AddLogActivity.this, "Cloudinary Error: " + error.getDescription(), Toast.LENGTH_LONG).show();
                    }
                    @Override public void onReschedule(String id, ErrorInfo error) {}
                }).dispatch();
    }

    private void saveToFirebase(String workDone, String from, String to, String url, String name) {
        if (!progressDialog.isShowing()) {
            progressDialog.setMessage("Saving log entry...");
            progressDialog.show();
        }

        String uid = mAuth.getUid();
        DatabaseReference logRef = mDatabase.child("Logbook").child(userGroupId);
        String entryId = logRef.push().getKey();

        if (entryId == null || uid == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Database Error", Toast.LENGTH_SHORT).show();
            return;
        }

        LogEntryModel entry = new LogEntryModel(entryId, uid, userGroupId, workDone, from, to, url, name, System.currentTimeMillis());

        logRef.child(entryId).setValue(entry).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(this, "Log entry added successfully!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String err = task.getException() != null ? task.getException().getMessage() : "Unknown DB error";
                Toast.makeText(this, "Save Failed: " + err, Toast.LENGTH_LONG).show();
            }
        });
    }
}
