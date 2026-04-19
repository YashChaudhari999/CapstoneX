package com.example.capstonex;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AddLogActivity extends AppCompatActivity {

    private static final String UPLOAD_PRESET = "Massanger";
    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private static final String TAG = "AddLogActivity";
    private static final int PICK_FILES_REQ = 1;
    // State
    private final List<Uri> selectedFileUris = new ArrayList<>();
    private final List<String> uploadedUrls = Collections.synchronizedList(new ArrayList<>());
    private final List<String> uploadedNames = Collections.synchronizedList(new ArrayList<>());
    // Cloudinary callbacks fire on a background thread — this posts back to UI thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // Views
    private TextInputEditText etWorkDone, etFromDate, etToDate;
    private MaterialButton btnUploadDocument, btnCancel, btnAddEntry, btnClearFiles;
    private TextView tvSelectedDocName;
    private LinearLayout llSelectedFilesContainer;
    private MaterialCardView cardSelectedFiles;
    private String userGroupId = "";
    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ProgressDialog progressDialog;

    // ─────────────────────────────────────────────────────────────────────────
    //  onCreate
    // ─────────────────────────────────────────────────────────────────────────

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
        llSelectedFilesContainer = findViewById(R.id.llSelectedFilesContainer);
        cardSelectedFiles = findViewById(R.id.cardSelectedFiles);
        btnClearFiles = findViewById(R.id.btnClearFiles);
        btnCancel = findViewById(R.id.btnCancel);
        btnAddEntry = findViewById(R.id.btnAddEntry);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        fetchUserGroupId(null);

        etFromDate.setOnClickListener(v -> showDatePicker(etFromDate));
        etToDate.setOnClickListener(v -> showDatePicker(etToDate));
        btnUploadDocument.setOnClickListener(v -> openFilePicker());
        btnClearFiles.setOnClickListener(v -> {
            selectedFileUris.clear();
            updateFileListUI();
        });
        btnCancel.setOnClickListener(v -> finish());
        btnAddEntry.setOnClickListener(v -> attemptSaveEntry());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  File Picker — any format, multiple files
    // ─────────────────────────────────────────────────────────────────────────

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");                               // ALL file types
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);  // multiple at once
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Files"), PICK_FILES_REQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_FILES_REQ || resultCode != RESULT_OK || data == null) return;

        selectedFileUris.clear();

        if (data.getClipData() != null) {
            // Multiple files
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                selectedFileUris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            // Single file
            selectedFileUris.add(data.getData());
        }

        updateFileListUI();
    }

    private void updateFileListUI() {
        llSelectedFilesContainer.removeAllViews();
        int count = selectedFileUris.size();

        if (count == 0) {
            tvSelectedDocName.setText("No files selected");
            cardSelectedFiles.setVisibility(View.GONE);
            return;
        }

        tvSelectedDocName.setText(count + " file(s) selected");
        cardSelectedFiles.setVisibility(View.VISIBLE);

        for (Uri uri : selectedFileUris) {
            TextView tv = new TextView(this);
            tv.setText("• " + getFileName(uri));
            tv.setTextSize(13f);
            tv.setPadding(4, 6, 4, 6);
            llSelectedFilesContainer.addView(tv);
        }
    }

    private String getFileName(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null && name.contains("/")) name = name.substring(name.lastIndexOf("/") + 1);
        return (name != null && !name.isEmpty()) ? name : "file_" + System.currentTimeMillis();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Save Flow
    // ─────────────────────────────────────────────────────────────────────────

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
                    Toast.makeText(this, "Group not found. Contact admin.", Toast.LENGTH_LONG).show();
                } else {
                    processUploadAndSave(workDone, from, to);
                }
            });
        } else {
            processUploadAndSave(workDone, from, to);
        }
    }

    private void processUploadAndSave(String workDone, String from, String to) {
        if (!selectedFileUris.isEmpty()) {
            uploadAllFiles(workDone, from, to);
        } else {
            saveToFirebase(workDone, from, to);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Cloudinary Upload via MediaManager (initialized in CloudinaryManager.java)
    // ─────────────────────────────────────────────────────────────────────────

    private void uploadAllFiles(String workDone, String from, String to) {
        uploadedUrls.clear();
        uploadedNames.clear();

        int total = selectedFileUris.size();
        AtomicInteger doneCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        progressDialog.setMessage("Uploading 0/" + total + " files...");
        if (!progressDialog.isShowing()) progressDialog.show();

        for (Uri fileUri : selectedFileUris) {
            String fileName = getFileName(fileUri);

            MediaManager.get()
                    .upload(fileUri)
                    .unsigned(UPLOAD_PRESET)
                    .option("resource_type", "auto")  // ← handles ALL file types (pdf, img, docx, etc.)
                    .option("public_id", "logbook/" + System.currentTimeMillis() + "_" + fileName)
                    .callback(new UploadCallback() {

                        @Override
                        public void onStart(String requestId) {
                            Log.d(TAG, "Started: " + fileName);
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            int pct = (totalBytes > 0) ? (int) ((bytes * 100) / totalBytes) : 0;
                            // ⚠️ Cloudinary callbacks are on background thread — must post to main
                            mainHandler.post(() ->
                                    progressDialog.setMessage("Uploading " + fileName + " " + pct + "%...")
                            );
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            String url = String.valueOf(resultData.get("secure_url"));
                            Log.d(TAG, "Success: " + url);

                            synchronized (uploadedUrls) {
                                uploadedUrls.add(url);
                                uploadedNames.add(fileName);
                            }

                            int done = doneCount.incrementAndGet();
                            // ⚠️ Must post to main thread
                            mainHandler.post(() -> {
                                progressDialog.setMessage("Uploaded " + done + "/" + total + " files...");
                                if (done + failedCount.get() == total) {
                                    saveToFirebase(workDone, from, to);
                                }
                            });
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            Log.e(TAG, "Failed [" + fileName + "]: " + error.getDescription());
                            int failed = failedCount.incrementAndGet();

                            // ⚠️ Must post to main thread
                            mainHandler.post(() -> {
                                Toast.makeText(AddLogActivity.this,
                                        "Failed: " + fileName + "\n" + error.getDescription(),
                                        Toast.LENGTH_LONG).show();

                                int done = doneCount.get();
                                if (done + failed == total) {
                                    if (done > 0) {
                                        saveToFirebase(workDone, from, to); // save successful ones
                                    } else {
                                        progressDialog.dismiss();
                                        Toast.makeText(AddLogActivity.this,
                                                "All uploads failed. Check Cloudinary preset settings.",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            Log.w(TAG, "Rescheduled: " + fileName);
                        }
                    })
                    .dispatch();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Firebase Save
    // ─────────────────────────────────────────────────────────────────────────

    private void saveToFirebase(String workDone, String from, String to) {
        progressDialog.setMessage("Saving log entry...");
        if (!progressDialog.isShowing()) progressDialog.show();

        String uid = mAuth.getUid();
        if (uid == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference logRef = mDatabase.child("Logbook").child(userGroupId);
        String entryId = logRef.push().getKey();

        if (entryId == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "Database error. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build documents list
        List<Map<String, String>> documents = new ArrayList<>();
        for (int i = 0; i < uploadedUrls.size(); i++) {
            Map<String, String> file = new HashMap<>();
            file.put("url", uploadedUrls.get(i));
            file.put("name", uploadedNames.get(i));
            documents.add(file);
        }

        // Build entry
        Map<String, Object> entry = new HashMap<>();
        entry.put("entryId", entryId);
        entry.put("userId", uid);
        entry.put("groupId", userGroupId);
        entry.put("workDone", workDone);
        entry.put("fromDate", from);
        entry.put("toDate", to);
        entry.put("documents", documents);
        entry.put("timestamp", System.currentTimeMillis());

        logRef.child(entryId).setValue(entry)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Log entry saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Log.e(TAG, "Firebase save failed: " + e.getMessage());
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Group Fetch
    // ─────────────────────────────────────────────────────────────────────────

    private void fetchUserGroupId(Runnable onComplete) {
        String uid = mAuth.getUid();
        if (uid == null) {
            if (onComplete != null) onComplete.run();
            return;
        }

        mDatabase.child("Users").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (snap.exists()) {
                    String gid = snap.child("groupId").getValue(String.class);
                    if (gid != null && !gid.isEmpty()) {
                        userGroupId = gid;
                        if (onComplete != null) onComplete.run();
                    } else {
                        String mySap = String.valueOf(snap.child("sapId").getValue());
                        deepSearchGroups(uid, mySap, onComplete);
                    }
                } else if (onComplete != null) onComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void deepSearchGroups(String myUid, String mySap, Runnable onComplete) {
        mDatabase.child("Groups").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot groupsSnap) {
                for (DataSnapshot group : groupsSnap.getChildren()) {
                    if (myUid.equals(group.child("leaderUid").getValue(String.class))) {
                        saveFoundGroupId(group.getKey(), onComplete);
                        return;
                    }
                    for (DataSnapshot id : group.child("memberUids").getChildren()) {
                        if (myUid.equals(id.getValue(String.class))) {
                            saveFoundGroupId(group.getKey(), onComplete);
                            return;
                        }
                    }
                    for (DataSnapshot member : group.child("memberDetails").getChildren()) {
                        if (mySap.equals(String.valueOf(member.child("sapId").getValue()))) {
                            saveFoundGroupId(group.getKey(), onComplete);
                            return;
                        }
                    }
                }
                if (onComplete != null) onComplete.run();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                if (onComplete != null) onComplete.run();
            }
        });
    }

    private void saveFoundGroupId(String groupId, Runnable onComplete) {
        userGroupId = groupId;
        String uid = mAuth.getUid();
        if (uid != null) {
            mDatabase.child("Users").child(uid).child("groupId").setValue(groupId);
            mDatabase.child("Users").child(uid).child("hasGroup").setValue(true);
        }
        if (onComplete != null) onComplete.run();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Date Picker
    // ─────────────────────────────────────────────────────────────────────────

    private void showDatePicker(TextInputEditText field) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) -> field.setText(
                        String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }
}