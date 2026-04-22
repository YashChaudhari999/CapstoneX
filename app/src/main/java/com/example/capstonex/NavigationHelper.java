package com.example.capstonex;

import android.app.Activity;
import android.content.Intent;

/**
 * NavigationHelper — CapstonX
 *
 * Centralizes all Intent-based navigation patterns.
 * Eliminates 15+ copy-pasted navigation blocks across activities.
 *
 * Usage:
 *   NavigationHelper.navigateTo(this, ProfileActivity.class);
 *   NavigationHelper.navigateAndClearStack(this, LoginActivity.class);
 *   NavigationHelper.navigateByRole(this, "mentor");
 */
public final class NavigationHelper {

    private NavigationHelper() {} // utility class — no instantiation

    // ─────────────────────────────────────────────────────────────────────
    // Standard forward navigation (slide in from right)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Navigate to a new activity with a right-slide animation.
     * Back stack is preserved — user can press back to return.
     */
    public static void navigateTo(Activity from, Class<?> to) {
        from.startActivity(new Intent(from, to));
        from.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Navigate to a new activity and avoid stacking duplicates.
     * Uses FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP.
     */
    public static void navigateSingleTop(Activity from, Class<?> to) {
        Intent intent = new Intent(from, to);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        from.startActivity(intent);
        from.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Login/Dashboard transitions (clear back stack)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Navigate to a destination and clear the entire back stack.
     * Use for: Login → Dashboard, Logout → Login.
     * The current activity is finished and cannot be returned to.
     */
    public static void navigateAndClearStack(Activity from, Class<?> to) {
        Intent intent = new Intent(from, to);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(intent);
        from.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        from.finish();
    }

    /**
     * Logout transition — slide left (reverse direction).
     * Navigates to LoginActivity and clears the back stack.
     */
    public static void logout(Activity from) {
        Intent intent = new Intent(from, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        from.startActivity(intent);
        from.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        from.finish();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Back navigation
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Navigate back with a reverse slide animation (left direction).
     */
    public static void navigateBack(Activity from) {
        from.finish();
        from.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Role-based routing
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Navigate to the correct dashboard based on the user's role string.
     * Clears the back stack — used after login / splash.
     *
     * @param from      Current activity
     * @param role      "student" | "mentor" | "admin" (case-insensitive)
     */
    public static void navigateByRole(Activity from, String role) {
        Class<?> target = StudentDashboardActivity.class; // safe default
        if (AppConstants.ROLE_MENTOR.equalsIgnoreCase(role)) {
            target = MentorDashboardActivity.class;
        } else if (AppConstants.ROLE_ADMIN.equalsIgnoreCase(role)) {
            target = AdminDashboardActivity.class;
        }
        navigateAndClearStack(from, target);
    }
}
