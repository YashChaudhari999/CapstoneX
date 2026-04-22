package com.example.capstonex;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

/**
 * FirebaseHelper — CapstonX
 *
 * Centralizes all common Firebase Realtime Database operations.
 * Eliminates repetitive ValueEventListener boilerplate scattered across
 * 15+ activities in the project.
 *
 * Usage:
 *   FirebaseHelper fb = FirebaseHelper.getInstance();
 *
 *   // Single read:
 *   fb.readOnce("Users/" + uid, snap -> { ... }, err -> { ... });
 *
 *   // Atomic multi-path write:
 *   fb.atomicUpdate(updates, () -> showSuccess(), err -> showError(err));
 *
 *   // Atomic counter (race-free group ID):
 *   fb.runCounterTransaction("Counters/IT2026", newVal -> { ... }, err -> { ... });
 */
public class FirebaseHelper {

    private static FirebaseHelper instance;
    private final DatabaseReference root;

    private FirebaseHelper() {
        root = FirebaseDatabase.getInstance(AppConstants.REALTIME_DB_URL).getReference();
    }

    public static FirebaseHelper getInstance() {
        if (instance == null) instance = new FirebaseHelper();
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Single-event read (fire-once, no lingering listener)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Reads the node at {@code path} once and delivers the result.
     *
     * @param path      Firebase path relative to root (e.g. "Users/uid123")
     * @param onSuccess Delivers the DataSnapshot (never null; may be !exists())
     * @param onError   Delivers a human-readable error string, or null if unused
     */
    public void readOnce(String path,
                         OnSuccessCallback<DataSnapshot> onSuccess,
                         OnErrorCallback onError) {
        root.child(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                onSuccess.onResult(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (onError != null) onError.onError(error.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Permanent real-time listener (caller owns remove)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Attaches a permanent ValueEventListener and returns it so the caller
     * can remove it in onDestroy() to avoid memory leaks.
     *
     * Example:
     *   listener = fb.listen("Groups", snap -> { ... }, err -> { ... });
     *   // In onDestroy:
     *   fb.removeListener("Groups", listener);
     */
    public ValueEventListener listen(String path,
                                     OnSuccessCallback<DataSnapshot> onSuccess,
                                     OnErrorCallback onError) {
        ValueEventListener listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot s) { onSuccess.onResult(s); }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                if (onError != null) onError.onError(e.getMessage());
            }
        };
        root.child(path).addValueEventListener(listener);
        return listener;
    }

    /** Remove a previously registered permanent listener. Call in onDestroy(). */
    public void removeListener(String path, ValueEventListener listener) {
        if (listener != null) root.child(path).removeEventListener(listener);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Convenience read methods
    // ─────────────────────────────────────────────────────────────────────

    public void getUserData(String uid,
                            OnSuccessCallback<DataSnapshot> onSuccess,
                            OnErrorCallback onError) {
        readOnce(AppConstants.NODE_USERS + "/" + uid, onSuccess, onError);
    }

    public void getUserGroupId(String uid,
                                OnSuccessCallback<String> onSuccess,
                                OnErrorCallback onError) {
        readOnce(AppConstants.NODE_USERS + "/" + uid + "/groupId", snap -> {
            String gid = snap.exists() ? snap.getValue(String.class) : null;
            onSuccess.onResult(gid);
        }, onError);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Write helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Executes a multi-path atomic write using updateChildren().
     * All paths succeed or all fail — equivalent to a Realtime DB transaction.
     */
    public void atomicUpdate(Map<String, Object> updates,
                             Runnable onSuccess,
                             OnErrorCallback onError) {
        root.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (onSuccess != null) onSuccess.run();
            } else {
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "Write failed";
                if (onError != null) onError.onError(msg);
            }
        });
    }

    /**
     * Writes a single value to {@code path}.
     */
    public void setValue(String path, Object value,
                         Runnable onSuccess, OnErrorCallback onError) {
        root.child(path).setValue(value).addOnCompleteListener(task -> {
            if (task.isSuccessful()) { if (onSuccess != null) onSuccess.run(); }
            else {
                String msg = task.getException() != null
                        ? task.getException().getMessage() : "Write failed";
                if (onError != null) onError.onError(msg);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Atomic server-side counter (race-free group ID generation)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Atomically increments an integer counter at {@code counterPath}.
     * Delivers the NEW counter value in {@code onSuccess}.
     *
     * Use case: Group ID generation — replaces the read-max+1 pattern that
     * suffers from a race condition when two admins create groups simultaneously.
     *
     * Example:
     *   fb.runCounterTransaction("Counters/IT2026",
     *       newVal -> executeCreation("IT2026_" + newVal, memberDocs),
     *       err -> showError(err));
     */
    public void runCounterTransaction(String counterPath,
                                      OnSuccessCallback<Integer> onSuccess,
                                      OnErrorCallback onError) {
        root.child(counterPath).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer val = currentData.getValue(Integer.class);
                currentData.setValue(val == null ? 1 : val + 1);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed,
                                   DataSnapshot currentData) {
                if (!committed || error != null) {
                    if (onError != null)
                        onError.onError(error != null ? error.getMessage() : "Transaction failed");
                    return;
                }
                Integer newVal = currentData != null
                        ? currentData.getValue(Integer.class) : null;
                if (newVal == null) {
                    if (onError != null) onError.onError("Counter value missing");
                    return;
                }
                onSuccess.onResult(newVal);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Key generation
    // ─────────────────────────────────────────────────────────────────────

    /** Generates a new Firebase push key under {@code path}. Never null in practice. */
    public String generatePushKey(String path) {
        String key = root.child(path).push().getKey();
        return key != null ? key : String.valueOf(System.currentTimeMillis());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Callback interfaces
    // ─────────────────────────────────────────────────────────────────────

    public interface OnSuccessCallback<T> {
        void onResult(T result);
    }

    public interface OnErrorCallback {
        void onError(String message);
    }
}
