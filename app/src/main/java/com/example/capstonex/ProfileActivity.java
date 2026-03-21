package com.example.capstonex;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

/**
 * ProfileActivity — CapstonX
 * <p>
 * Fixes applied:
 * FIX 1 — ic_close → ic_back (ic_close does not exist in project drawables)
 * State B FAB uses ic_back as the "undo / cancel" icon.
 * <p>
 * FIX 2 — onBackPressed() replaced with OnBackPressedDispatcher
 * (onBackPressed is deprecated; back gestures no longer trigger it)
 * <p>
 * FIX 3 — super.onBackPressed() removed (dispatcher handles the call chain)
 */
public class ProfileActivity extends BaseActivity {

    private static final String DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";
    private static final int PICK_IMAGE_REQUEST = 1;

    // ── Views ──────────────────────────────────────────────────────────────
    private ImageView ivProfilePicture, ivToolbarAvatar;
    private FloatingActionButton fabEditImage;
    private TextView tvPhotoHint;
    private TextInputEditText etName, etDept, etEmail, etSapId;
    private MaterialButton btnSave, btnChangePassword, btnLogout;

    // ── Firebase ───────────────────────────────────────────────────────────
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    // ── Photo state ────────────────────────────────────────────────────────
    private String savedPhotoUrl;
    private Uri pendingImageUri;
    private boolean hasPendingPhoto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setupEdgeToEdge(findViewById(R.id.profile_root));

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference().child("Users").child(uid);

        bindViews();
        registerBackHandler();   // FIX 2 — use OnBackPressedDispatcher
        setListeners();
        loadProfile();
    }

    // ─────────────────────────────────────────────────────────────────────
    // FIX 2 — Back gesture / button handled via OnBackPressedDispatcher
    // ─────────────────────────────────────────────────────────────────────
    private void registerBackHandler() {
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (hasPendingPhoto) {
                            // Warn before discarding unsaved photo
                            new AlertDialog.Builder(ProfileActivity.this)
                                    .setTitle("Discard Photo?")
                                    .setMessage("You have an unsaved photo change. Discard it?")
                                    .setPositiveButton("Discard", (d, w) -> {
                                        undoPendingPhoto();
                                        setEnabled(false);
                                        getOnBackPressedDispatcher().onBackPressed();
                                    })
                                    .setNegativeButton("Keep", null)
                                    .show();
                        } else {
                            // No pending photo — just go back normally
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Bind views
    // ─────────────────────────────────────────────────────────────────────
    private void bindViews() {
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        ivToolbarAvatar = findViewById(R.id.ivToolbarAvatar);
        fabEditImage = findViewById(R.id.fabEditImage);
        tvPhotoHint = findViewById(R.id.tvPhotoHint);
        etName = findViewById(R.id.etProfileName);
        etDept = findViewById(R.id.etProfileDept);
        etEmail = findViewById(R.id.etProfileEmail);
        etSapId = findViewById(R.id.etProfileSapId);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        setPhotoState(false); // default State A
    }

    // ─────────────────────────────────────────────────────────────────────
    // Listeners
    // ─────────────────────────────────────────────────────────────────────
    private void setListeners() {
        // Toolbar back arrow — triggers the same back dispatcher
        findViewById(R.id.toolbar).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        // Notifications
        findViewById(R.id.ivNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // FAB — pick photo (State A) or undo pending photo (State B)
        fabEditImage.setOnClickListener(v -> {
            if (hasPendingPhoto) undoPendingPhoto();
            else openImagePicker();
        });

        // Confirm upload
        btnSave.setOnClickListener(v -> uploadAndSave());

        // Change password
        btnChangePassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ChangePasswordActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        // Logout
        btnLogout.setOnClickListener(v -> logout());
    }

    // ─────────────────────────────────────────────────────────────────────
    // Load profile from Realtime DB — read-only display
    // ─────────────────────────────────────────────────────────────────────
    private void loadProfile() {
        if (mAuth.getCurrentUser() != null)
            etEmail.setText(mAuth.getCurrentUser().getEmail());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                setText(etName, snapshot.child("name").getValue(String.class));

                // dept or branch (students use "branch" from CSV import)
                String dept = snapshot.child("dept").getValue(String.class);
                if (dept == null || dept.isEmpty())
                    dept = snapshot.child("branch").getValue(String.class);
                setText(etDept, dept);

                // SAP ID — students only, hidden for mentors/admins
                String sapId = snapshot.child("sapId").getValue(String.class);
                String rollNo = snapshot.child("rollNo").getValue(String.class);
                if (sapId != null && !sapId.isEmpty()) {
                    String display = (rollNo != null && !rollNo.isEmpty())
                            ? sapId + "  ·  " + rollNo : sapId;
                    setText(etSapId, display);
                    findViewById(R.id.tilSapId).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.tilSapId).setVisibility(View.GONE);
                }

                // Profile photo
                savedPhotoUrl = snapshot.child("profileImageUrl")
                        .getValue(String.class);
                loadPhoto(savedPhotoUrl);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileActivity.this,
                        "Could not load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────
    // Image picker
    // ─────────────────────────────────────────────────────────────────────
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(intent, "Select Profile Picture"),
                PICK_IMAGE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST
                && resultCode == RESULT_OK
                && data != null && data.getData() != null) {

            pendingImageUri = data.getData();

            // Show local preview immediately — not saved yet
            ivProfilePicture.setImageURI(pendingImageUri);
            if (ivToolbarAvatar != null)
                ivToolbarAvatar.setImageURI(pendingImageUri);

            setPhotoState(true); // → State B
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Undo — discard pending photo, restore saved photo
    // ─────────────────────────────────────────────────────────────────────
    private void undoPendingPhoto() {
        pendingImageUri = null;
        loadPhoto(savedPhotoUrl);
        setPhotoState(false); // → State A
        Toast.makeText(this, "Photo change discarded", Toast.LENGTH_SHORT).show();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Upload to Cloudinary → save URL to Realtime DB
    // ─────────────────────────────────────────────────────────────────────
    private void uploadAndSave() {
        if (pendingImageUri == null) return;
        setBusy(true);

        MediaManager.get()
                .upload(pendingImageUri)
                .unsigned("Massanger")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String id) {
                    }

                    @Override
                    public void onProgress(String id, long b, long t) {
                    }

                    @Override
                    public void onSuccess(String id, Map resultData) {
                        String newUrl = resultData.get("secure_url").toString();

                        userRef.child("profileImageUrl").setValue(newUrl)
                                .addOnSuccessListener(v -> {
                                    savedPhotoUrl = newUrl;
                                    pendingImageUri = null;
                                    loadPhoto(newUrl);
                                    setPhotoState(false);
                                    setBusy(false);
                                    Toast.makeText(ProfileActivity.this,
                                            "Profile photo updated ✓",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    setBusy(false);
                                    Toast.makeText(ProfileActivity.this,
                                            "Upload OK but save failed: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    }

                    @Override
                    public void onError(String id, ErrorInfo error) {
                        setBusy(false);
                        loadPhoto(savedPhotoUrl); // restore on failure
                        setPhotoState(false);
                        Toast.makeText(ProfileActivity.this,
                                "Upload failed: " + error.getDescription(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onReschedule(String id, ErrorInfo error) {
                    }
                })
                .dispatch();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Photo state machine
    // ─────────────────────────────────────────────────────────────────────

    /**
     * State A (pending=false): FAB = ✏️ ic_plus,  button disabled
     * State B (pending=true):  FAB = ↩ ic_back,   button enabled
     * <p>
     * FIX 1: ic_back used instead of ic_close (ic_close doesn't exist)
     */
    private void setPhotoState(boolean pending) {
        hasPendingPhoto = pending;
        if (pending) {
            // State B — photo picked, not yet saved
            fabEditImage.setImageResource(R.drawable.ic_back); // FIX 1
            tvPhotoHint.setText("Tap ↩ to discard changes");
            btnSave.setEnabled(true);
            btnSave.setAlpha(1.0f);
        } else {
            // State A — no pending change
            fabEditImage.setImageResource(R.drawable.ic_plus);
            tvPhotoHint.setText("Tap ✏️ to change photo");
            btnSave.setEnabled(false);
            btnSave.setAlpha(0.5f);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Busy state during upload
    // ─────────────────────────────────────────────────────────────────────
    private void setBusy(boolean busy) {
        btnSave.setEnabled(!busy);
        btnSave.setText(busy ? "Uploading…" : "UPDATE PROFILE PHOTO");
        fabEditImage.setEnabled(!busy);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Load photo into both avatars via Glide
    // ─────────────────────────────────────────────────────────────────────
    private void loadPhoto(String url) {
        if (url == null || url.isEmpty()) return;
        Glide.with(this).load(url).placeholder(R.drawable.ic_person)
                .circleCrop().transition(DrawableTransitionOptions.withCrossFade())
                .into(ivProfilePicture);
        if (ivToolbarAvatar != null) {
            Glide.with(this).load(url).placeholder(R.drawable.ic_person)
                    .circleCrop().transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivToolbarAvatar);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Logout — confirmation dialog before signing out
    // ─────────────────────────────────────────────────────────────────────
    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setText(TextInputEditText f, String v) {
        if (v != null && !v.isEmpty()) f.setText(v);
    }
}