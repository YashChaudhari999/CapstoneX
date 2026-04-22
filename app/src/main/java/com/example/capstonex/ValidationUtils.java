package com.example.capstonex;

import android.text.TextUtils;
import android.util.Patterns;

/**
 * ValidationUtils — CapstonX
 *
 * Centralizes all input-validation logic.
 * Eliminates repeated validation snippets scattered across activities.
 *
 * All methods are null-safe and return either:
 *   • null  → input is valid
 *   • String → human-readable error message to show in the UI
 */
public final class ValidationUtils {

    private ValidationUtils() {} // utility class — no instantiation

    // ── Constants ─────────────────────────────────────────────────────────
    private static final int MIN_SAP_LENGTH      = 8;
    private static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * Same regex used in UserImportHelper — kept consistent here.
     * Note: Patterns.EMAIL_ADDRESS is stricter and preferred for UI fields.
     */
    public static final String EMAIL_REGEX = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

    // ─────────────────────────────────────────────────────────────────────
    // SAP ID
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Returns true if the SAP ID is non-null, all-numeric, and ≥ 8 digits.
     * Used by GroupCreationActivity auto-fill trigger.
     */
    public static boolean isValidSapId(String sap) {
        if (sap == null || sap.trim().isEmpty()) return false;
        String t = sap.trim();
        return t.length() >= MIN_SAP_LENGTH && t.matches("\\d+");
    }

    /**
     * Returns null if valid, or an error message to display inline.
     */
    public static String validateSapId(String sap) {
        if (TextUtils.isEmpty(sap))          return "SAP ID is required";
        if (!sap.trim().matches("\\d+"))     return "SAP ID must contain only digits";
        if (sap.trim().length() < MIN_SAP_LENGTH)
            return "SAP ID must be at least " + MIN_SAP_LENGTH + " digits";
        return null; // valid
    }

    // ─────────────────────────────────────────────────────────────────────
    // Email
    // ─────────────────────────────────────────────────────────────────────

    /** Android's built-in pattern — stricter than the CSV regex. */
    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    public static String validateEmail(String email) {
        if (TextUtils.isEmpty(email))                                    return "Email is required";
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches())     return "Enter a valid email address";
        return null;
    }

    /**
     * Validates email against the relaxed CSV regex (mirrors UserImportHelper).
     * Use this only for CSV import validation; use validateEmail() for UI fields.
     */
    public static String validateEmailForCsv(String email) {
        if (TextUtils.isEmpty(email))              return "Email is empty";
        if (!email.matches(EMAIL_REGEX))           return "Invalid email: " + email;
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Password
    // ─────────────────────────────────────────────────────────────────────

    public static String validatePassword(String password) {
        if (TextUtils.isEmpty(password))                return "Password is required";
        if (password.length() < MIN_PASSWORD_LENGTH)    return "Password must be at least 6 characters";
        return null;
    }

    public static String validatePasswordMatch(String p1, String p2) {
        if (p1 == null || p2 == null || !p1.equals(p2)) return "Passwords do not match";
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Generic "required" check
    // ─────────────────────────────────────────────────────────────────────

    /** True if the value is non-null and non-blank. */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Returns null if valid (non-blank), or an error message for the given field name.
     */
    public static String validateRequired(String value, String fieldName) {
        if (TextUtils.isEmpty(value)) return fieldName + " is required";
        return null;
    }
}
