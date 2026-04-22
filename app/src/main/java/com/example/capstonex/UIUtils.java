package com.example.capstonex;

import android.app.ProgressDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

/**
 * UIUtils — CapstonX
 *
 * Centralizes all UI feedback: Toast, ProgressDialog (deprecated-safe),
 * AlertDialog, Snackbar, and button loading states.
 *
 * Eliminates 20+ scattered Toast/Dialog calls across activities.
 */
public final class UIUtils {

    private UIUtils() {} // utility class — no instantiation

    // ─────────────────────────────────────────────────────────────────────
    // Toast helpers
    // ─────────────────────────────────────────────────────────────────────

    public static void showShortToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showLongToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Snackbar helpers
    // ─────────────────────────────────────────────────────────────────────

    public static void showSnackbar(View root, String msg) {
        Snackbar.make(root, msg, Snackbar.LENGTH_SHORT).show();
    }

    public static void showSnackbarAction(View root, String msg,
                                          String actionText, Runnable action) {
        Snackbar.make(root, msg, Snackbar.LENGTH_LONG)
                .setAction(actionText, v -> action.run())
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────
    // ProgressDialog (safe wrapper — dismisses only if still showing)
    // Note: ProgressDialog is deprecated in API 26. Prefer a custom
    //       AlertDialog with a ProgressBar for new screens.
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Creates a non-cancelable ProgressDialog with the given message.
     * Call safeDismiss() to close it safely.
     */
    @SuppressWarnings("deprecation")
    public static ProgressDialog createProgressDialog(Context ctx, String message) {
        ProgressDialog pd = new ProgressDialog(ctx);
        pd.setMessage(message);
        pd.setCancelable(false);
        pd.setIndeterminate(true);
        return pd;
    }

    /**
     * Dismisses the dialog only if it is not null and is currently showing.
     * Prevents the "Activity has been destroyed" crash on config changes.
     */
    @SuppressWarnings("deprecation")
    public static void safeDismiss(ProgressDialog pd) {
        try {
            if (pd != null && pd.isShowing()) pd.dismiss();
        } catch (Exception ignored) {
            // Window already destroyed — safe to ignore
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // AlertDialog helpers
    // ─────────────────────────────────────────────────────────────────────

    /** Simple info / error dialog with an "OK" button. */
    public static void showInfoDialog(Context ctx, String title, String message) {
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Confirmation dialog.
     * Calls onConfirm() when the positive button is tapped.
     */
    public static void showConfirmDialog(Context ctx, String title, String message,
                                         String confirmLabel, Runnable onConfirm) {
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(confirmLabel, (d, w) -> onConfirm.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Two-option dialog (e.g., "Discard / Keep").
     */
    public static void showTwoOptionDialog(Context ctx, String title, String message,
                                            String positiveLabel, Runnable onPositive,
                                            String negativeLabel, Runnable onNegative) {
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveLabel, (d, w) -> onPositive.run())
                .setNegativeButton(negativeLabel, (d, w) -> {
                    if (onNegative != null) onNegative.run();
                })
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────
    // MaterialButton loading state
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sets a MaterialButton into loading or normal state.
     *
     * Loading: disabled, 70% alpha, shows loadingText.
     * Normal : enabled, full alpha, shows normalText.
     */
    public static void setButtonLoading(MaterialButton btn, boolean loading,
                                        String loadingText, String normalText) {
        if (btn == null) return;
        btn.setEnabled(!loading);
        btn.setText(loading ? loadingText : normalText);
        btn.setAlpha(loading ? 0.7f : 1.0f);
    }

    // ─────────────────────────────────────────────────────────────────────
    // View visibility shortcuts
    // ─────────────────────────────────────────────────────────────────────

    public static void show(View v)      { if (v != null) v.setVisibility(View.VISIBLE);  }
    public static void hide(View v)      { if (v != null) v.setVisibility(View.GONE);     }
    public static void invisible(View v) { if (v != null) v.setVisibility(View.INVISIBLE);}
}
