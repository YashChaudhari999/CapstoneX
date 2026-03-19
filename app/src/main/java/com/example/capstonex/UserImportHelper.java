package com.example.capstonex;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * UserImportHelper — CapstonX
 * Firebase Auth + Realtime Database
 * <p>
 * ── STUDENT CSV Format (6 columns, header row optional) ──────────────────
 * <p>
 * sapId, rollNo, name, email, password, branch
 * <p>
 * 70012400171, A265, Yash Chaudhari, Yash.Chaudhari171@nmims.in, Pass@123, IT
 * 70012400172, A266, Priya Sharma,   Priya.Sharma172@nmims.in,   Pass@456, CS
 * <p>
 * ── MENTOR CSV Format (4 columns, header row optional) ───────────────────
 * <p>
 * id, name, email, password
 * <p>
 * M001, Prof. Rajesh Mehta, rajesh.mehta@nmims.in, Prof@123
 * M002, Prof. Sunita Joshi, sunita.joshi@nmims.in, Prof@456
 * <p>
 * ── Realtime DB structure ─────────────────────────────────────────────────
 * <p>
 * Users/{uid}/
 * uid             : Firebase Auth UID
 * email           : from CSV
 * role            : "student" | "mentor"
 * name            : from CSV
 * profileImageUrl : auto-generated from name → ui-avatars.com
 * status          : "active"
 * fcmToken        : "" (filled on first login)
 * createdAt       : timestamp (ms)
 * createdBy       : "admin_import"
 * <p>
 * — Student-only fields —
 * sapId           : e.g. "70012400171"
 * rollNo          : e.g. "A265"
 * branch          : e.g. "IT" | "CS" | "CE" | "AIML"
 * <p>
 * — Mentor-only fields —
 * mentorId        : e.g. "M001"
 */
public class UserImportHelper {

    private static final String TAG = "UserImportHelper";
    private static final String SECONDARY_APP = "capstonex_importer";
    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private static final String EMAIL_PATTERN =
            "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    private final Context context;
    private final DatabaseReference usersRef;

    public UserImportHelper(Context context) {
        this.context = context.getApplicationContext();
        this.usersRef = FirebaseDatabase
                .getInstance(DB_URL)
                .getReference()
                .child("Users");
        Log.d(TAG, "Realtime DB ref → " + DB_URL + "/Users");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point — role decides which parser to use
    // ─────────────────────────────────────────────────────────────────────
    public void importFromFile(Uri fileUri, String role, ImportCallback callback) {
        new Thread(() -> {
            android.os.Handler main =
                    new android.os.Handler(android.os.Looper.getMainLooper());
            try {
                FirebaseAuth secondaryAuth;
                try {
                    secondaryAuth = getSecondaryAuth();
                } catch (Exception e) {
                    main.post(() -> callback.onError(
                            "Firebase setup error: " + e.getMessage()));
                    return;
                }

                if ("student".equals(role)) {
                    List<StudentRow> rows = parseStudentCsv(fileUri);
                    Log.d(TAG, "Parsed " + rows.size() + " student rows");
                    if (rows.isEmpty()) {
                        main.post(() -> callback.onError(
                                "No valid rows found.\n" +
                                        "Expected: sapId,rollNo,name,email,password,branch"));
                        return;
                    }
                    processStudentRows(rows, secondaryAuth, callback);

                } else {
                    List<MentorRow> rows = parseMentorCsv(fileUri);
                    Log.d(TAG, "Parsed " + rows.size() + " mentor rows");
                    if (rows.isEmpty()) {
                        main.post(() -> callback.onError(
                                "No valid rows found.\n" +
                                        "Expected: id,name,email,password"));
                        return;
                    }
                    processMentorRows(rows, secondaryAuth, callback);
                }

            } catch (Exception e) {
                Log.e(TAG, "Import error: " + e.getMessage(), e);
                main.post(() -> callback.onError(
                        "Failed to read file: " + e.getMessage()));
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Parse student CSV
    // Columns: sapId, rollNo, name, email, password, branch
    // ─────────────────────────────────────────────────────────────────────
    private List<StudentRow> parseStudentCsv(Uri fileUri) throws Exception {
        List<StudentRow> rows = new ArrayList<>();
        BufferedReader reader = openReader(fileUri);
        String line;
        boolean firstLine = true;
        int lineNum = 0;

        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (firstLine) line = line.replace("\uFEFF", "");
            line = line.trim();
            if (line.isEmpty()) continue;

            // Skip header row (starts with "sap" or "SAP")
            if (firstLine && line.toLowerCase().startsWith("sap")) {
                Log.d(TAG, "Skipping header: " + line);
                firstLine = false;
                continue;
            }
            firstLine = false;

            String[] c = line.split(",", -1);
            if (c.length < 5) {
                Log.w(TAG, "Row " + lineNum + " skipped — only " + c.length + " col(s)");
                continue;
            }

            String sapId = c[0].trim();
            String rollNo = c[1].trim();
            String name = c[2].trim();
            String email = c[3].trim();
            String password = c[4].trim();
            String branch = c.length >= 6 ? c[5].trim() : "";

            rows.add(new StudentRow(sapId, rollNo, name, email, password, branch));
            Log.d(TAG, "Student row " + lineNum + " → " + email);
        }
        reader.close();
        return rows;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Parse mentor CSV
    // Columns: id, name, email, password
    // ─────────────────────────────────────────────────────────────────────
    private List<MentorRow> parseMentorCsv(Uri fileUri) throws Exception {
        List<MentorRow> rows = new ArrayList<>();
        BufferedReader reader = openReader(fileUri);
        String line;
        boolean firstLine = true;
        int lineNum = 0;

        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (firstLine) line = line.replace("\uFEFF", "");
            line = line.trim();
            if (line.isEmpty()) continue;

            // Skip header row (starts with "id" or "ID")
            if (firstLine && line.toLowerCase().startsWith("id")) {
                Log.d(TAG, "Skipping header: " + line);
                firstLine = false;
                continue;
            }
            firstLine = false;

            String[] c = line.split(",", -1);
            if (c.length < 4) {
                Log.w(TAG, "Row " + lineNum + " skipped — only " + c.length + " col(s)");
                continue;
            }

            String mentorId = c[0].trim();
            String name = c[1].trim();
            String email = c[2].trim();
            String password = c[3].trim();

            rows.add(new MentorRow(mentorId, name, email, password));
            Log.d(TAG, "Mentor row " + lineNum + " → " + email);
        }
        reader.close();
        return rows;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Process student rows
    // ─────────────────────────────────────────────────────────────────────
    private void processStudentRows(List<StudentRow> rows,
                                    FirebaseAuth secondaryAuth,
                                    ImportCallback callback) {
        android.os.Handler main =
                new android.os.Handler(android.os.Looper.getMainLooper());
        int total = rows.size(), succeeded = 0, failed = 0;

        for (int i = 0; i < rows.size(); i++) {
            StudentRow row = rows.get(i);
            final int pos = i + 1;

            Log.d(TAG, "Processing student " + pos + "/" + total + " → " + row.email);

            // Validate
            String err = validateStudent(row);
            if (err != null) {
                failed++;
                final String e = err;
                main.post(() -> callback.onRowProcessed(pos, total, row.email, false, e));
                continue;
            }

            // Auth + DB
            final boolean[] ok = {false};
            final String[] error = {null};
            CountDownLatch latch = new CountDownLatch(1);

            secondaryAuth.createUserWithEmailAndPassword(row.email, row.password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        Log.d(TAG, "✓ Auth: " + row.email + " uid=" + uid);
                        secondaryAuth.signOut();
                        saveStudentToDb(uid, row,
                                () -> {
                                    ok[0] = true;
                                    latch.countDown();
                                },
                                e -> {
                                    error[0] = e;
                                    latch.countDown();
                                }
                        );
                    })
                    .addOnFailureListener(e -> {
                        error[0] = authError(e);
                        latch.countDown();
                    });

            await(latch, row.email);

            if (ok[0]) {
                succeeded++;
                main.post(() -> callback.onRowProcessed(pos, total, row.email, true, null));
            } else {
                failed++;
                final String e = error[0] != null ? error[0] : "Unknown error";
                main.post(() -> callback.onRowProcessed(pos, total, row.email, false, e));
            }
        }

        final int s = succeeded, f = failed;
        main.post(() -> callback.onComplete(s, f, total));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Process mentor rows
    // ─────────────────────────────────────────────────────────────────────
    private void processMentorRows(List<MentorRow> rows,
                                   FirebaseAuth secondaryAuth,
                                   ImportCallback callback) {
        android.os.Handler main =
                new android.os.Handler(android.os.Looper.getMainLooper());
        int total = rows.size(), succeeded = 0, failed = 0;

        for (int i = 0; i < rows.size(); i++) {
            MentorRow row = rows.get(i);
            final int pos = i + 1;

            Log.d(TAG, "Processing mentor " + pos + "/" + total + " → " + row.email);

            // Validate
            String err = validateMentor(row);
            if (err != null) {
                failed++;
                final String e = err;
                main.post(() -> callback.onRowProcessed(pos, total, row.email, false, e));
                continue;
            }

            // Auth + DB
            final boolean[] ok = {false};
            final String[] error = {null};
            CountDownLatch latch = new CountDownLatch(1);

            secondaryAuth.createUserWithEmailAndPassword(row.email, row.password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        Log.d(TAG, "✓ Auth: " + row.email + " uid=" + uid);
                        secondaryAuth.signOut();
                        saveMentorToDb(uid, row,
                                () -> {
                                    ok[0] = true;
                                    latch.countDown();
                                },
                                e -> {
                                    error[0] = e;
                                    latch.countDown();
                                }
                        );
                    })
                    .addOnFailureListener(e -> {
                        error[0] = authError(e);
                        latch.countDown();
                    });

            await(latch, row.email);

            if (ok[0]) {
                succeeded++;
                main.post(() -> callback.onRowProcessed(pos, total, row.email, true, null));
            } else {
                failed++;
                final String e = error[0] != null ? error[0] : "Unknown error";
                main.post(() -> callback.onRowProcessed(pos, total, row.email, false, e));
            }
        }

        final int s = succeeded, f = failed;
        main.post(() -> callback.onComplete(s, f, total));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Save student to Realtime DB
    // ─────────────────────────────────────────────────────────────────────
    private void saveStudentToDb(String uid, StudentRow row,
                                 Runnable onSuccess, StringConsumer onFailure) {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", row.email);
        map.put("role", "student");
        map.put("name", row.name);
        map.put("sapId", row.sapId);
        map.put("rollNo", row.rollNo);
        map.put("branch", row.branch);
        map.put("profileImageUrl", avatarUrl(row.name));
        map.put("status", "active");
        map.put("fcmToken", "");
        map.put("createdAt", System.currentTimeMillis());
        map.put("createdBy", "admin_import");

        Log.d(TAG, "Saving student to DB: Users/" + uid);
        usersRef.child(uid).setValue(map)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "✓ DB saved: " + uid);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ DB FAILED: " + uid + " | " + e.getMessage());
                    onFailure.accept("Auth OK but DB save failed: " + e.getMessage());
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Save mentor to Realtime DB
    // ─────────────────────────────────────────────────────────────────────
    private void saveMentorToDb(String uid, MentorRow row,
                                Runnable onSuccess, StringConsumer onFailure) {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", row.email);
        map.put("role", "mentor");
        map.put("name", row.name);
        map.put("mentorId", row.mentorId);
        map.put("profileImageUrl", avatarUrl(row.name));
        map.put("status", "active");
        map.put("fcmToken", "");
        map.put("createdAt", System.currentTimeMillis());
        map.put("createdBy", "admin_import");

        Log.d(TAG, "Saving mentor to DB: Users/" + uid);
        usersRef.child(uid).setValue(map)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "✓ DB saved: " + uid);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ DB FAILED: " + uid + " | " + e.getMessage());
                    onFailure.accept("Auth OK but DB save failed: " + e.getMessage());
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────
    private String validateStudent(StudentRow r) {
        if (r.sapId.isEmpty()) return "SAP ID is empty";
        if (r.rollNo.isEmpty()) return "Roll No is empty";
        if (r.name.isEmpty()) return "Name is empty";
        if (r.email.isEmpty()) return "Email is empty";
        if (r.password.isEmpty()) return "Password is empty";
        if (!r.email.matches(EMAIL_PATTERN)) return "Invalid email: " + r.email;
        if (r.password.length() < 6) return "Password too short (min 6 chars)";
        return null;
    }

    private String validateMentor(MentorRow r) {
        if (r.mentorId.isEmpty()) return "Mentor ID is empty";
        if (r.name.isEmpty()) return "Name is empty";
        if (r.email.isEmpty()) return "Email is empty";
        if (r.password.isEmpty()) return "Password is empty";
        if (!r.email.matches(EMAIL_PATTERN)) return "Invalid email: " + r.email;
        if (r.password.length() < 6) return "Password too short (min 6 chars)";
        return null;
    }

    /**
     * Generate avatar URL from the person's name
     */
    private String avatarUrl(String name) {
        return "https://ui-avatars.com/api/?name="
                + name.replace(" ", "+")
                + "&background=7B1C2E&color=fff&size=128";
    }

    /**
     * Friendly auth error messages
     */
    private String authError(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException)
            return "Email already registered";
        if (e instanceof FirebaseAuthWeakPasswordException)
            return "Weak password: " + e.getMessage();
        if (e instanceof FirebaseAuthInvalidCredentialsException)
            return "Invalid email format";
        Log.e(TAG, "Auth FAILED: " + e.getClass().getSimpleName()
                + " | " + e.getMessage());
        return e.getMessage();
    }

    /**
     * Open a BufferedReader from a file URI
     */
    private BufferedReader openReader(Uri fileUri) throws Exception {
        InputStream is = context.getContentResolver().openInputStream(fileUri);
        if (is == null) throw new Exception("Cannot open file — check permissions");
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    /**
     * Await latch with 30s timeout
     */
    private void await(CountDownLatch latch, String email) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                Log.e(TAG, "Timeout for " + email);
            }
        } catch (InterruptedException ignored) {
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────
    // Secondary FirebaseApp — admin session stays untouched
    // ─────────────────────────────────────────────────────────────────────
    private FirebaseAuth getSecondaryAuth() {
        for (FirebaseApp app : FirebaseApp.getApps(context)) {
            if (app.getName().equals(SECONDARY_APP)) {
                return FirebaseAuth.getInstance(app);
            }
        }
        FirebaseOptions options = FirebaseApp.getInstance().getOptions();
        FirebaseApp secondaryApp =
                FirebaseApp.initializeApp(context, options, SECONDARY_APP);
        return FirebaseAuth.getInstance(secondaryApp);
    }

    // ── Callback interface ─────────────────────────────────────────────────
    public interface ImportCallback {
        void onRowProcessed(int done, int total,
                            String email, boolean success, String error);

        void onComplete(int succeeded, int failed, int total);

        void onError(String error);
    }

    private interface StringConsumer {
        void accept(String value);
    }

    // ── Student row ───────────────────────────────────────────────────────
    private static class StudentRow {
        final String sapId, rollNo, name, email, password, branch;

        StudentRow(String sapId, String rollNo, String name,
                   String email, String password, String branch) {
            this.sapId = sapId.trim();
            this.rollNo = rollNo.trim();
            this.name = name.trim();
            this.email = email.trim();
            this.password = password.trim();
            this.branch = branch.trim();
        }
    }

    // ── Mentor row ────────────────────────────────────────────────────────
    private static class MentorRow {
        final String mentorId, name, email, password;

        MentorRow(String mentorId, String name, String email, String password) {
            this.mentorId = mentorId.trim();
            this.name = name.trim();
            this.email = email.trim();
            this.password = password.trim();
        }
    }
}