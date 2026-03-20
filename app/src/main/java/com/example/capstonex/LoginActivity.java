package com.example.capstonex;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * LoginActivity — CapstonX
 * <p>
 * NOTE: Auto-login (already signed-in user) is handled by SplashActivity.
 * This activity is ONLY shown when the user is NOT logged in.
 * Do not add an auto-login check here — it would cause a redundant
 * DB read on every cold start.
 * <p>
 * ── Authentication Flow ───────────────────────────────────────────────────
 * Step 1 — Validate fields (empty check + email format)
 * Step 2 — Firebase Auth signInWithEmailAndPassword
 * Step 3 — Read Users/{uid}/role from Realtime DB
 * Step 4 — Role verification: DB role MUST match selected toggle
 * Mismatch → sign out + clear error dialog
 * Step 5 — Save refreshed FCM token to Users/{uid}/fcmToken
 * Step 6 — Navigate to correct dashboard
 * <p>
 * ── XML IDs (activity_login.xml) ─────────────────────────────────────────
 * toggleRole, btnStudent, btnMentor, btnAdmin
 * etEmail, etPassword, btnLogin, tvForgotPassword
 */
public class LoginActivity extends BaseActivity {

    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";

    // ── Views ──────────────────────────────────────────────────────────────
    private MaterialButtonToggleGroup toggleRole;
    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin;

    // ── Firebase ───────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupEdgeToEdge(findViewById(android.R.id.content));

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference().child("Users");

        bindViews();
        setListeners();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Bind views
    // ─────────────────────────────────────────────────────────────────────
    private void bindViews() {
        toggleRole = findViewById(R.id.toggleRole);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        // TextInputLayout wraps each EditText — used for inline error messages
        tilEmail = (TextInputLayout) etEmail.getParent().getParent();
        tilPassword = (TextInputLayout) etPassword.getParent().getParent();
    }

    private void setListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Auto-clear errors as user starts typing
        etEmail.addTextChangedListener(
                new SimpleTextWatcher(() -> tilEmail.setError(null)));
        etPassword.addTextChangedListener(
                new SimpleTextWatcher(() -> tilPassword.setError(null)));

        // Forgot password
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                tilEmail.setError("Enter your email first");
                etEmail.requestFocus();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
            } else {
                sendPasswordReset(email);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Step 1 — Validate inputs
    // ─────────────────────────────────────────────────────────────────────
    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            etEmail.requestFocus();
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            if (valid) etPassword.requestFocus();
            valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            if (valid) etPassword.requestFocus();
            valid = false;
        }

        if (!valid) return;

        setLoading(true);
        signIn(email, password, getSelectedRole());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Step 2 — Firebase Auth
    // ─────────────────────────────────────────────────────────────────────
    private void signIn(String email, String password, String selectedRole) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    verifyRoleAndNavigate(uid, selectedRole); // → Step 3
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    handleAuthError(e);
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Step 3 + 4 — Read role from DB and verify against toggle
    // ─────────────────────────────────────────────────────────────────────
    private void verifyRoleAndNavigate(String uid, String selectedRole) {
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);

                if (!snapshot.exists()) {
                    // Auth account exists but no DB record (e.g. pre-created admin)
                    if ("admin".equals(selectedRole)) {
                        saveFcmToken(uid, () -> navigateTo(AdminDashboardActivity.class));
                    } else {
                        mAuth.signOut();
                        showErrorDialog("Account Not Set Up",
                                "Your account has not been configured yet.\n" +
                                        "Please contact your administrator.");
                    }
                    return;
                }

                String dbRole = snapshot.child("role").getValue(String.class);
                String name = snapshot.child("name").getValue(String.class);
                if (dbRole == null) dbRole = "student";
                if (name == null) name = "";

                // Step 4 — Role mismatch check
                if (!dbRole.equalsIgnoreCase(selectedRole)) {
                    mAuth.signOut();
                    showRoleMismatchError(selectedRole, dbRole);
                    return;
                }

                // Step 5 → 6 — Save FCM token then navigate
                final String finalRole = dbRole;
                saveFcmToken(uid, () -> navigateByRole(finalRole));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                showErrorDialog("Connection Error",
                        "Could not reach the database.\nCheck your internet connection.");
                mAuth.signOut();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Step 5 — Refresh FCM token
    // ─────────────────────────────────────────────────────────────────────
    private void saveFcmToken(String uid, Runnable onDone) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        usersRef.child(uid).child("fcmToken")
                                .setValue(task.getResult())
                                .addOnCompleteListener(t -> onDone.run());
                    } else {
                        onDone.run(); // token failed — still proceed
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Step 6 — Navigate to correct dashboard
    // ─────────────────────────────────────────────────────────────────────
    private void navigateByRole(String role) {
        switch (role.toLowerCase()) {
            case "mentor":
                navigateTo(MentorDashboardActivity.class);
                break;
            case "admin":
                navigateTo(AdminDashboardActivity.class);
                break;
            default:
                navigateTo(StudentDashboardActivity.class);
                break;
        }
    }

    private void navigateTo(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN,
                    R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
        finish();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Forgot password
    // ─────────────────────────────────────────────────────────────────────
    private void sendPasswordReset(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v ->
                        new AlertDialog.Builder(this)
                                .setTitle("Email Sent")
                                .setMessage("A password reset link has been sent to:\n" + email)
                                .setPositiveButton("OK", null)
                                .show())
                .addOnFailureListener(e ->
                        showErrorDialog("Failed", e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────
    // Error handling
    // ─────────────────────────────────────────────────────────────────────
    private void handleAuthError(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) {
            tilEmail.setError("No account found with this email");
            etEmail.requestFocus();
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            tilPassword.setError("Incorrect password");
            etPassword.setText("");
            etPassword.requestFocus();
        } else {
            showErrorDialog("Login Failed", e.getMessage());
        }
    }

    private void showRoleMismatchError(String selected, String actual) {
        showErrorDialog("Wrong Role Selected",
                "You selected \"" + capitalise(selected) + "\" but this account " +
                        "is registered as a \"" + capitalise(actual) + "\".\n\n" +
                        "Please select the correct role and try again.");
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────
    private String getSelectedRole() {
        if (toggleRole == null) return "student";
        int id = toggleRole.getCheckedButtonId();
        if (id == R.id.btnMentor) return "mentor";
        if (id == R.id.btnAdmin) return "admin";
        return "student";
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in…" : getString(R.string.btn_sign_in));
    }

    private String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // Minimal TextWatcher to avoid boilerplate
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable onChange;

        SimpleTextWatcher(Runnable r) {
            this.onChange = r;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int st, int c, int a) {
        }

        @Override
        public void onTextChanged(CharSequence s, int st, int b, int c) {
            onChange.run();
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {
        }
    }
}
//package com.example.capstonex;
//
//import android.content.Intent;
//import android.os.Build;
//import android.os.Bundle;
//import com.google.android.material.button.MaterialButton;
//import com.google.android.material.button.MaterialButtonToggleGroup;
//
//public class LoginActivity extends BaseActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_login);
//        setupEdgeToEdge(findViewById(android.R.id.content));
//
//        MaterialButton btnLogin = findViewById(R.id.btnLogin);
//        MaterialButtonToggleGroup toggleRole = findViewById(R.id.toggleRole);
//
//        if (btnLogin != null) {
//            btnLogin.setOnClickListener(v -> {
//                Intent intent;
//                int checkedId = toggleRole.getCheckedButtonId();
//
//                if (checkedId == R.id.btnMentor) {
//                    intent = new Intent(LoginActivity.this, MentorDashboardActivity.class);
//                } else if (checkedId == R.id.btnAdmin) {
//                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
//                } else {
//                    intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
//                }
//                startActivity(intent);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left);
//                } else {
//                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
//                }
//                finish();
//            });
//        }
//    }
//}
