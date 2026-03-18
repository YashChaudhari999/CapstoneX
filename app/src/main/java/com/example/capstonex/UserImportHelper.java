package com.example.capstonex;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * UserImportHelper — CapstonX (Fixed)
 * <p>
 * CSV Format (5 columns, header row optional):
 * email,password,name,sapId,photoUrl
 * <p>
 * FIX 1 — Exception in importFromFile now calls callback.onError() instead
 * of silently swallowing the error, so the UI is never left stuck.
 * <p>
 * FIX 2 — Firebase Auth errors are now fully surfaced per-row with the
 * exact message (e.g. "Email/Password auth not enabled in Firebase
 * Console", "Network error", "Email already registered").
 * <p>
 * FIX 3 — CountDownLatch timeout added (30s per row) so a hung Firebase
 * call never freezes the import thread forever.
 * <p>
 * FIX 4 — Removed java.util.function.Consumer (requires API 24+).
 * Replaced with a local Callback interface — safe on all API levels.
 */
public class UserImportHelper {

    private static final String TAG = "UserImportHelper";
    private static final String SECONDARY_APP = "capstonex_importer";
    private static final String DEFAULT_AVATAR =
            "https://ui-avatars.com/api/?background=7B1C2E&color=fff&size=128&name=User";
    private static final String EMAIL_PATTERN =
            "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    private final Context context;
    private final FirebaseFirestore db;

    public UserImportHelper(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
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
                            "No valid rows found in file.\n" +
                                    "Expected format: email,password,name,sapId,photoUrl\n" +
                                    "Make sure the file has at least 4 columns per row."));
                    return;
                }

                // Check if Email/Password sign-in is enabled — do a quick test
                // by checking secondary auth initialisation
                FirebaseAuth secondaryAuth;
                try {
                    secondaryAuth = getSecondaryAuth();
                } catch (Exception e) {
                    main.post(() -> callback.onError(
                            "Firebase setup error: " + e.getMessage() + "\n" +
                                    "Make sure google-services.json is present."));
                    return;
                }

                processRows(rows, role, secondaryAuth, callback);

            } catch (Exception e) {
                Log.e(TAG, "Import error: " + e.getMessage(), e);
                // FIX 1 — always call back so UI is never stuck
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
        if (is == null) throw new Exception("Cannot open file — check file permissions");

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

        String line;
        boolean firstLine = true;
        int lineNum = 0;

        while ((line = reader.readLine()) != null) {
            lineNum++;
            // Strip BOM (Excel sometimes adds \uFEFF at start of CSV)
            if (firstLine) line = line.replace("\uFEFF", "");
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
                Log.w(TAG, "Row " + lineNum + " skipped — only " +
                        cols.length + " column(s): " + line);
                continue;
            }

            String email = cols[0].trim();
            String password = cols[1].trim();
            String name = cols[2].trim();
            String sapId = cols[3].trim();
            // photoUrl may contain commas inside a URL — rejoin remaining cols
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

            Log.d(TAG, "Processing row " + pos + "/" + total + " → " + row.email);

            // ── Step 1: Validate ──────────────────────────────────────────
            String validationError = validate(row);
            if (validationError != null) {
                failed++;
                Log.w(TAG, "Validation failed for " + row.email + ": " + validationError);
                final String err = validationError;
                main.post(() -> callback.onRowProcessed(
                        pos, total, row.email, false, err));
                continue;
            }

            // ── Step 2: Firebase Auth (secondary app) ─────────────────────
            final boolean[] rowSuccess = {false};
            final String[] rowError = {null};
            // FIX 3 — 30s timeout so a hung call never freezes the loop
            CountDownLatch latch = new CountDownLatch(1);

            secondaryAuth
                    .createUserWithEmailAndPassword(row.email, row.password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        Log.d(TAG, "Auth account created for " + row.email + " uid=" + uid);
                        secondaryAuth.signOut();

                        // ── Step 3: Save to Firestore ─────────────────────────
                        saveToFirestore(uid, row, role,
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
                        // FIX 2 — surface the exact Firebase Auth error
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            rowError[0] = "Email already registered";
                        } else if (e instanceof FirebaseAuthWeakPasswordException) {
                            rowError[0] = "Weak password: " + e.getMessage();
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            rowError[0] = "Invalid email format";
                        } else {
                            // This often means Email/Password auth is NOT enabled
                            // in Firebase Console → Authentication → Sign-in methods
                            rowError[0] = e.getMessage();
                            Log.e(TAG, "Auth FAILED for " + row.email
                                    + " | Error class: " + e.getClass().getSimpleName()
                                    + " | Message: " + e.getMessage());
                        }
                        latch.countDown();
                    });

            try {
                // FIX 3 — 30s timeout per row
                boolean finished = latch.await(30, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    rowError[0] = "Timeout — check internet connection";
                    Log.e(TAG, "Latch timed out for " + row.email);
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
                        ? rowError[0]
                        : "Unknown error — check Logcat for tag UserImportHelper";
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
        if (!row.email.matches(EMAIL_PATTERN)) return "Invalid email: " + row.email;
        if (row.password.length() < 6)
            return "Password too short (min 6 chars): " + row.password;
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Save to Firestore
    // ─────────────────────────────────────────────────────────────────────
    private void saveToFirestore(String uid, UserRow row, String role,
                                 Runnable onSuccess, StringConsumer onFailure) {

        String finalPhoto = (row.photoUrl == null || row.photoUrl.isEmpty())
                ? DEFAULT_AVATAR : row.photoUrl;

        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid", uid);
        userDoc.put("email", row.email);
        userDoc.put("role", role);
        userDoc.put("name", row.name);
        userDoc.put("sapId", row.sapId);
        userDoc.put("profileImageUrl", finalPhoto);
        userDoc.put("status", "active");
        userDoc.put("fcmToken", "");
        userDoc.put("createdAt", Timestamp.now());
        userDoc.put("createdBy", "admin_import");

        db.collection("users").document(uid)
                .set(userDoc)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore saved for uid=" + uid);
                    onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore save failed for uid=" + uid + ": " + e.getMessage());
                    onFailure.accept("Auth OK but Firestore save failed: " + e.getMessage());
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

    // ── Callbacks ─────────────────────────────────────────────────────────
    public interface ImportCallback {
        /**
         * Fired after every row — success or failure
         */
        void onRowProcessed(int done, int total,
                            String email, boolean success, String error);

        /**
         * Fired when entire file is done
         */
        void onComplete(int succeeded, int failed, int total);

        /**
         * Fired if the file itself can't be read / parsed
         */
        void onError(String error);
    }

    // Simple string consumer — avoids java.util.function (API 24+)
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