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
 * FIX: Explicitly pass the Realtime DB URL so Firebase connects correctly.
 * Without this, FirebaseDatabase.getInstance() may silently fail to
 * resolve the database and writes never reach the server.
 * <p>
 * DB URL: https://capstonex-8b885-default-rtdb.firebaseio.com
 * <p>
 * ── Realtime DB structure ─────────────────────────────────────────────────
 * Users/{uid}/
 * uid, email, role, name, sapId,
 * profileImageUrl, status, fcmToken, createdAt, createdBy
 */
public class UserImportHelper {

    private static final String TAG = "UserImportHelper";
    private static final String SECONDARY_APP = "capstonex_importer";

    // ── YOUR Realtime DB URL — explicit so Firebase always connects ────────
    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";

    private static final String DEFAULT_AVATAR =
            "https://ui-avatars.com/api/?background=7B1C2E&color=fff&size=128&name=User";
    private static final String EMAIL_PATTERN =
            "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    private final Context context;
    private final DatabaseReference usersRef;

    public UserImportHelper(Context context) {
        this.context = context.getApplicationContext();

        // ── FIX: Pass DB URL explicitly ────────────────────────────────────
        // FirebaseDatabase.getInstance() alone doesn't always resolve the URL.
        // Using getInstance(url) guarantees the correct database is targeted.
        this.usersRef = FirebaseDatabase
                .getInstance(DB_URL)
                .getReference()
                .child("Users");

        Log.d(TAG, "Realtime DB ref initialised → " + DB_URL + "/Users");
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public entry point
    // ─────────────────────────────────────────────────────────────────────
    public void importFromFile(Uri fileUri, String role, ImportCallback callback) {
        new Thread(() -> {
            android.os.Handler main =
                    new android.os.Handler(android.os.Looper.getMainLooper());
            try {
                List<UserRow> rows = parseCsv(fileUri);
                Log.d(TAG, "Parsed " + rows.size() + " rows from CSV");

                if (rows.isEmpty()) {
                    main.post(() -> callback.onError(
                            "No valid rows found.\n" +
                                    "Expected: email,password,name,sapId,photoUrl"));
                    return;
                }

                FirebaseAuth secondaryAuth;
                try {
                    secondaryAuth = getSecondaryAuth();
                } catch (Exception e) {
                    main.post(() -> callback.onError(
                            "Firebase setup error: " + e.getMessage()));
                    return;
                }

                processRows(rows, role, secondaryAuth, callback);

            } catch (Exception e) {
                Log.e(TAG, "Import error: " + e.getMessage(), e);
                main.post(() -> callback.onError(
                        "Failed to read file: " + e.getMessage()));
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Parse CSV
    // ─────────────────────────────────────────────────────────────────────
    private List<UserRow> parseCsv(Uri fileUri) throws Exception {
        List<UserRow> rows = new ArrayList<>();

        InputStream is = context.getContentResolver().openInputStream(fileUri);
        if (is == null) throw new Exception("Cannot open file — check permissions");

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        String line;
        boolean firstLine = true;
        int lineNum = 0;

        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (firstLine) line = line.replace("\uFEFF", ""); // strip Excel BOM
            line = line.trim();
            if (line.isEmpty()) continue;

            // Auto-detect & skip header row
            if (firstLine && line.toLowerCase().startsWith("email")) {
                Log.d(TAG, "Skipping header row: " + line);
                firstLine = false;
                continue;
            }
            firstLine = false;

            String[] cols = line.split(",", -1);
            if (cols.length < 4) {
                Log.w(TAG, "Row " + lineNum + " skipped — only "
                        + cols.length + " col(s): " + line);
                continue;
            }

            String email = cols[0].trim();
            String password = cols[1].trim();
            String name = cols[2].trim();
            String sapId = cols[3].trim();
            String photoUrl = cols.length >= 5 ? cols[4].trim() : "";

            rows.add(new UserRow(email, password, name, sapId, photoUrl));
            Log.d(TAG, "Row " + lineNum + " parsed → " + email);
        }

        reader.close();
        return rows;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Process all rows sequentially
    // ─────────────────────────────────────────────────────────────────────
    private void processRows(List<UserRow> rows, String role,
                             FirebaseAuth secondaryAuth,
                             ImportCallback callback) {

        android.os.Handler main =
                new android.os.Handler(android.os.Looper.getMainLooper());

        int total = rows.size(), succeeded = 0, failed = 0;

        for (int i = 0; i < rows.size(); i++) {
            UserRow row = rows.get(i);
            final int pos = i + 1;

            Log.d(TAG, "Processing " + pos + "/" + total + " → " + row.email);

            // ── Step 1: Validate ──────────────────────────────────────────
            String validationError = validate(row);
            if (validationError != null) {
                failed++;
                final String err = validationError;
                Log.w(TAG, "Validation failed: " + err);
                main.post(() -> callback.onRowProcessed(
                        pos, total, row.email, false, err));
                continue;
            }

            // ── Step 2: Firebase Auth (secondary app) ─────────────────────
            final boolean[] rowSuccess = {false};
            final String[] rowError = {null};
            CountDownLatch latch = new CountDownLatch(1);

            secondaryAuth
                    .createUserWithEmailAndPassword(row.email, row.password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        Log.d(TAG, "✓ Auth created: " + row.email + " uid=" + uid);
                        secondaryAuth.signOut();

                        // ── Step 3: Save to Realtime DB ───────────────────────
                        saveToRealtimeDB(uid, row, role,
                                () -> {
                                    rowSuccess[0] = true;
                                    latch.countDown();
                                },
                                err -> {
                                    rowError[0] = err;
                                    latch.countDown();
                                }
                        );
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            rowError[0] = "Email already registered";
                        } else if (e instanceof FirebaseAuthWeakPasswordException) {
                            rowError[0] = "Weak password: " + e.getMessage();
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            rowError[0] = "Invalid email format";
                        } else {
                            rowError[0] = e.getMessage();
                            Log.e(TAG, "✗ Auth FAILED: " + row.email
                                    + " | " + e.getClass().getSimpleName()
                                    + " | " + e.getMessage());
                        }
                        latch.countDown();
                    });

            try {
                if (!latch.await(30, TimeUnit.SECONDS)) {
                    rowError[0] = "Timeout — check internet connection";
                    Log.e(TAG, "Timeout for " + row.email);
                }
            } catch (InterruptedException ignored) {
            }

            if (rowSuccess[0]) {
                succeeded++;
                main.post(() -> callback.onRowProcessed(
                        pos, total, row.email, true, null));
            } else {
                failed++;
                final String err = rowError[0] != null
                        ? rowError[0] : "Unknown error";
                main.post(() -> callback.onRowProcessed(
                        pos, total, row.email, false, err));
            }
        }

        final int s = succeeded, f = failed;
        main.post(() -> callback.onComplete(s, f, total));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────
    private String validate(UserRow row) {
        if (row.email.isEmpty()) return "Email is empty";
        if (row.password.isEmpty()) return "Password is empty";
        if (row.name.isEmpty()) return "Name is empty";
        if (row.sapId.isEmpty()) return "SAP ID is empty";
        if (!row.email.matches(EMAIL_PATTERN))
            return "Invalid email: " + row.email;
        if (row.password.length() < 6)
            return "Password too short (min 6 chars)";
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Save to Realtime DB → Users/{uid}
    // ─────────────────────────────────────────────────────────────────────
    private void saveToRealtimeDB(String uid, UserRow row, String role,
                                  Runnable onSuccess, StringConsumer onFailure) {

        String finalPhoto = (row.photoUrl == null || row.photoUrl.isEmpty())
                ? DEFAULT_AVATAR : row.photoUrl;

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("email", row.email);
        userMap.put("role", role);            // "student" | "mentor"
        userMap.put("name", row.name);
        userMap.put("sapId", row.sapId);
        userMap.put("profileImageUrl", finalPhoto);
        userMap.put("status", "active");
        userMap.put("fcmToken", "");
        userMap.put("createdAt", System.currentTimeMillis());
        userMap.put("createdBy", "admin_import");

        Log.d(TAG, "Saving to Realtime DB: Users/" + uid);

        usersRef.child(uid).setValue(userMap)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✓ Realtime DB saved: Users/" + uid);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Realtime DB FAILED: Users/" + uid
                            + " | " + e.getMessage());
                    onFailure.accept("Auth OK but DB save failed: " + e.getMessage());
                });
    }

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

    // ── One parsed CSV row ────────────────────────────────────────────────
    private static class UserRow {
        final String email, password, name, sapId, photoUrl;

        UserRow(String email, String password,
                String name, String sapId, String photoUrl) {
            this.email = email.trim();
            this.password = password.trim();
            this.name = name.trim();
            this.sapId = sapId.trim();
            this.photoUrl = photoUrl.trim();
        }
    }
}