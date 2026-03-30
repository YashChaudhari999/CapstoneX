package com.example.capstonex;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

/**
 * ChangePasswordActivity — CapstonX
 * <p>
 * Flow:
 * 1. Pre-fills user's email from FirebaseAuth (read-only field)
 * 2. User taps "SEND RESET EMAIL"
 * 3. Firebase sends a password reset link to that email
 * 4. Success state shown (green checkmark + email confirmation)
 * 5. After 3 seconds → signs out → LoginActivity
 * so user can log back in fresh with their new password
 * <p>
 * New XML IDs (activity_change_password.xml):
 * change_password_root, toolbar,
 * tilResetEmail, etResetEmail,
 * btnUpdatePassword,
 * llSuccessState, tvSuccessMessage, tvSuccessSubtext
 */
public class ChangePasswordActivity extends BaseActivity {

    // ── Views ──────────────────────────────────────────────────────────────
    private TextInputLayout tilResetEmail;
    private TextInputEditText etResetEmail;
    private MaterialButton btnSend;
    private LinearLayout llSuccessState;
    private TextView tvSuccessMessage, tvSuccessSubtext;

    // ── Firebase ───────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setupEdgeToEdge(findViewById(R.id.change_password_root));

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }

        // ── Toolbar ────────────────────────────────────────────────────────
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("");
        toolbar.setNavigationOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                });

        // ── Bind views ─────────────────────────────────────────────────────
        tilResetEmail = findViewById(R.id.tilResetEmail);
        etResetEmail = findViewById(R.id.etResetEmail);
        btnSend = findViewById(R.id.btnUpdatePassword);
        llSuccessState = findViewById(R.id.llSuccessState);
        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);
        tvSuccessSubtext = findViewById(R.id.tvSuccessSubtext);

        // ── Pre-fill email — read-only, user just confirms it ──────────────
        String email = mAuth.getCurrentUser().getEmail();
        if (email != null) etResetEmail.setText(email);

        // ── Send button ────────────────────────────────────────────────────
        btnSend.setOnClickListener(v -> sendResetEmail());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Send password reset email via Firebase Auth
    // ─────────────────────────────────────────────────────────────────────
    private void sendResetEmail() {
        String email = etResetEmail.getText().toString().trim();

        if (email.isEmpty()) {
            tilResetEmail.setError("No email found. Please log out and log back in.");
            return;
        }

        tilResetEmail.setError(null);
        setLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    showSuccessState(email);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    new AlertDialog.Builder(this)
                            .setTitle("Failed to Send")
                            .setMessage(e.getMessage()
                                    + "\n\nMake sure you have an active internet connection.")
                            .setPositiveButton("OK", null)
                            .show();
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Show success UI then auto sign-out after 3 seconds
    // ─────────────────────────────────────────────────────────────────────
    private void showSuccessState(String email) {
        // Swap button for success card
        btnSend.setVisibility(View.GONE);
        llSuccessState.setVisibility(View.VISIBLE);

        tvSuccessMessage.setText("Reset Email Sent!");
        tvSuccessSubtext.setText(
                "A password reset link was sent to:\n" + email
                        + "\n\nOpen the link in your email to set a new password."
                        + "\n\nYou will be signed out now.");

        // Auto sign-out after 3 seconds
        btnSend.postDelayed(this::signOutAndGoToLogin, 6000);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Sign out → LoginActivity
    // ─────────────────────────────────────────────────────────────────────
    private void signOutAndGoToLogin() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Loading state
    // ─────────────────────────────────────────────────────────────────────
    private void setLoading(boolean loading) {
        btnSend.setEnabled(!loading);
        btnSend.setText(loading ? "Sending…" : "SEND RESET EMAIL");
    }
}