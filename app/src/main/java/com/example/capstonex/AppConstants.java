package com.example.capstonex;

/**
 * AppConstants — CapstonX
 *
 * Single source of truth for all app-wide constant values.
 * Replace every hardcoded duplicate with a reference to this class.
 *
 * Usage:
 *   FirebaseDatabase.getInstance(AppConstants.REALTIME_DB_URL).getReference()
 */
public final class AppConstants {

    private AppConstants() {} // utility class — no instantiation

    // ── Firebase ──────────────────────────────────────────────────────────
    public static final String REALTIME_DB_URL =
            "https://capstonex-8b885-default-rtdb.firebaseio.com";

    // ── Cloudinary ────────────────────────────────────────────────────────
    /** Unsigned upload preset name configured in the Cloudinary dashboard. */
    public static final String CLOUDINARY_UPLOAD_PRESET = "Massanger";

    // ── User roles ────────────────────────────────────────────────────────
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_MENTOR  = "mentor";
    public static final String ROLE_ADMIN   = "admin";

    // ── Firebase node keys ────────────────────────────────────────────────
    public static final String NODE_USERS             = "Users";
    public static final String NODE_GROUPS            = "Groups";
    public static final String NODE_NOTIFICATIONS     = "Notifications";
    public static final String NODE_TOPIC_APPROVALS   = "TopicApprovals";
    public static final String NODE_LOGBOOK           = "Logbook";
    public static final String NODE_DOMAINS           = "Domains";
    public static final String NODE_COUNTERS          = "Counters";

    // ── Timing ────────────────────────────────────────────────────────────
    public static final long SPLASH_DELAY_MS = 2000L;
    public static final long PASSWORD_RESET_SIGNOUT_DELAY_MS = 6000L;
}
