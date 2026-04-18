package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;

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

public class LoginActivity extends BaseActivity {

    private static final String DB_URL = "https://capstonex-8b885-default-rtdb.firebaseio.com";

    private MaterialButtonToggleGroup toggleRole;
    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(DB_URL).getReference().child("Users");

        bindViews();
        setListeners();
    }

    private void bindViews() {
        toggleRole = findViewById(R.id.toggleRole);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        tilEmail = (TextInputLayout) etEmail.getParent().getParent();
        tilPassword = (TextInputLayout) etPassword.getParent().getParent();
    }

    private void setListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        etEmail.addTextChangedListener(new SimpleTextWatcher(() -> tilEmail.setError(null)));
        etPassword.addTextChangedListener(new SimpleTextWatcher(() -> tilPassword.setError(null)));

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                tilEmail.setError("Enter your email first");
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Enter a valid email address");
            } else {
                sendPasswordReset(email);
            }
        });
    }

    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            valid = false;
        }

        if (!valid) return;

        setLoadingState(true);
        signIn(email, password, getSelectedRole());
    }

    private void signIn(String email, String password, String selectedRole) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    verifyRoleAndNavigate(uid, selectedRole);
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    handleAuthError(e);
                });
    }

    private void verifyRoleAndNavigate(String uid, String selectedRole) {
        usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoadingState(false);
                if (!snapshot.exists()) {
                    if ("admin".equals(selectedRole)) {
                        saveFcmToken(uid, () -> navigateTo(AdminDashboardActivity.class));
                    } else {
                        mAuth.signOut();
                        showErrorDialog("Account Not Set Up", "Your account has not been configured yet.");
                    }
                    return;
                }

                String dbRole = snapshot.child("role").getValue(String.class);
                if (dbRole == null) dbRole = "student";

                if (!dbRole.equalsIgnoreCase(selectedRole)) {
                    mAuth.signOut();
                    showErrorDialog("Wrong Role", "This account is registered as " + dbRole);
                    return;
                }

                String finalRole = dbRole;
                saveFcmToken(uid, () -> navigateByRole(finalRole));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoadingState(false);
                mAuth.signOut();
            }
        });
    }

    private void saveFcmToken(String uid, Runnable onDone) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                usersRef.child(uid).child("fcmToken").setValue(task.getResult()).addOnCompleteListener(t -> onDone.run());
            } else {
                onDone.run();
            }
        });
    }

    private void navigateByRole(String role) {
        if ("mentor".equalsIgnoreCase(role)) navigateTo(MentorDashboardActivity.class);
        else if ("admin".equalsIgnoreCase(role)) navigateTo(AdminDashboardActivity.class);
        else navigateTo(StudentDashboardActivity.class);
    }

    private void navigateTo(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void sendPasswordReset(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v -> showErrorDialog("Email Sent", "Reset link sent to " + email))
                .addOnFailureListener(e -> showErrorDialog("Failed", e.getMessage()));
    }

    private void handleAuthError(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException) tilEmail.setError("No account found");
        else if (e instanceof FirebaseAuthInvalidCredentialsException)
            tilPassword.setError("Wrong password");
        else showErrorDialog("Login Failed", e.getMessage());
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }

    private String getSelectedRole() {
        int id = toggleRole.getCheckedButtonId();
        if (id == R.id.btnMentor) return "mentor";
        if (id == R.id.btnAdmin) return "admin";
        return "student";
    }

    private void setLoadingState(boolean loading) {
        btnLogin.setEnabled(!loading);
        btnLogin.setText(loading ? "Signing in..." : getString(R.string.btn_sign_in));
    }

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
