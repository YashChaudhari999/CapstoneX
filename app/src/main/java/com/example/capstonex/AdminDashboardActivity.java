package com.example.capstonex;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * AdminDashboardActivity — CapstonX
 * <p>
 * FIX: Removed ALL Firestore references.
 * loadOverviewCounts() now reads from Realtime DB → Users node,
 * filtering by role = "mentor" for mentor count,
 * and reading Groups node for group count.
 * <p>
 * This eliminates the Firestore WriteStream NOT_FOUND errors.
 */
public class AdminDashboardActivity extends BaseActivity {

    private static final String TAG = "AdminDashboard";
    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private static final int REQ_PICK_STUDENTS = 101;
    private static final int REQ_PICK_MENTORS = 102;

    // ── Views ──────────────────────────────────────────────────────────────
    private MaterialButton btnAddStudents, btnAddMentors;
    private ProgressBar progressStudents, progressMentors;
    private TextView tvStudentImportStatus, tvMentorImportStatus;
    private TextView tvGroupCount, tvMentorCount;

    // ── Firebase — Realtime DB only, no Firestore ──────────────────────────
    private DatabaseReference usersRef;
    private DatabaseReference groupsRef;
    private UserImportHelper importHelper;

    // ── Progress dialog ────────────────────────────────────────────────────
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        setupEdgeToEdge(findViewById(R.id.admin_drawer_layout));

        // ── Realtime DB refs (explicit URL — no Firestore) ─────────────────
        FirebaseDatabase realtimeDb = FirebaseDatabase.getInstance(DB_URL);
        usersRef = realtimeDb.getReference().child("Users");
        groupsRef = realtimeDb.getReference().child("Groups");

        importHelper = new UserImportHelper(this);

        // ── Progress dialog ────────────────────────────────────────────────
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        // ── Toolbar & Drawer ───────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.admin_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");

        final DrawerLayout drawerLayout = findViewById(R.id.admin_drawer_layout);
        toolbar.setNavigationOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START));

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // ── Bind views ─────────────────────────────────────────────────────
        btnAddStudents = findViewById(R.id.btnAddStudents);
        btnAddMentors = findViewById(R.id.btnAddMentors);
        progressStudents = findViewById(R.id.progressStudents);
        progressMentors = findViewById(R.id.progressMentors);
        tvStudentImportStatus = findViewById(R.id.tvStudentImportStatus);
        tvMentorImportStatus = findViewById(R.id.tvMentorImportStatus);
        tvGroupCount = findViewById(R.id.tvGroupCount);
        tvMentorCount = findViewById(R.id.tvMentorCount);

        btnAddStudents.setOnClickListener(v -> openFilePicker(REQ_PICK_STUDENTS));
        btnAddMentors.setOnClickListener(v -> openFilePicker(REQ_PICK_MENTORS));

        loadOverviewCounts();
    }

    // ── File picker ───────────────────────────────────────────────────────
    private void openFilePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/csv",
                "text/comma-separated-values",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        });
        startActivityForResult(
                Intent.createChooser(intent, "Select CSV / Excel File"),
                requestCode
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;

        Uri fileUri = data.getData();
        if (requestCode == REQ_PICK_STUDENTS) {
            startImport(fileUri, "student",
                    btnAddStudents, progressStudents, tvStudentImportStatus);
        } else if (requestCode == REQ_PICK_MENTORS) {
            startImport(fileUri, "mentor",
                    btnAddMentors, progressMentors, tvMentorImportStatus);
        }
    }

    // ── Import ────────────────────────────────────────────────────────────
    private void startImport(Uri fileUri, String role,
                             MaterialButton button,
                             ProgressBar progressBar,
                             TextView statusText) {

        button.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Reading file…");

        progressDialog.setMessage("Reading file…");
        progressDialog.show();

        importHelper.importFromFile(fileUri, role, new UserImportHelper.ImportCallback() {

            @Override
            public void onRowProcessed(int done, int total,
                                       String email, boolean success, String error) {
                String icon = success ? "✓" : "✗";
                String detail = success
                        ? icon + " [" + done + "/" + total + "]  " + email
                        : icon + " [" + done + "/" + total + "]  " + email
                        + "\n     → " + error;

                Log.d(TAG, detail);
                progressDialog.setMessage("Importing " + role + "s…\n\n" + detail);
                statusText.setText(detail);
            }

            @Override
            public void onComplete(int succeeded, int failed, int total) {
                dismissProgress();
                button.setEnabled(true);
                progressBar.setVisibility(View.GONE);

                String summary = "Done: " + succeeded + " added, "
                        + failed + " failed out of " + total;
                statusText.setText(summary);
                Log.d(TAG, summary);

                Toast.makeText(AdminDashboardActivity.this,
                        succeeded + " " + role + "(s) added successfully!",
                        Toast.LENGTH_LONG).show();

                if (failed > 0) {
                    showInfoDialog("Some rows failed",
                            failed + " row(s) could not be imported.\n\n" +
                                    "Common reasons:\n" +
                                    "• Email already registered in Firebase Auth\n" +
                                    "  → Delete from Firebase Console → Authentication\n\n" +
                                    "• Password shorter than 6 characters\n" +
                                    "• Invalid email format\n\n" +
                                    "Check Logcat (tag: UserImportHelper) for details.");
                }

                // Refresh overview cards from Realtime DB
                loadOverviewCounts();
            }

            @Override
            public void onError(String error) {
                dismissProgress();
                button.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                statusText.setText("Import failed");
                Log.e(TAG, "Import error: " + error);
                showInfoDialog("Import Failed", error);
            }
        });
    }

    // ── Load overview counts from Realtime DB ─────────────────────────────
    // FIX: Reads Users node filtered by role="mentor" for mentor count
    //      and Groups node for group count — NO Firestore calls
    private void loadOverviewCounts() {

        // ── Mentor count ───────────────────────────────────────────────────
        usersRef.orderByChild("role").equalTo("mentor")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        tvMentorCount.setText(snapshot.getChildrenCount() + " Mentors");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Mentor count error: " + error.getMessage());
                    }
                });

        // ── Group count ────────────────────────────────────────────────────
        groupsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvGroupCount.setText(snapshot.getChildrenCount() + " Groups");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Group count error: " + error.getMessage());
                tvGroupCount.setText("0 Groups");
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}