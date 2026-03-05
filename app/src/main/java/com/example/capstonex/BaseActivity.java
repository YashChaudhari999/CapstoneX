package com.example.capstonex;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Global Fix for Status Bar:
        // Instead of using EdgeToEdge (which makes the bar transparent and causes overlap),
        // we set a solid brand color for the status bar area.
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorTopBar));

        // Ensure status bar icons (clock, battery) are white/light for the dark maroon background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = window.getDecorView();
            int flags = decor.getSystemUiVisibility();
            // Clear the LIGHT_STATUS_BAR flag to ensure white icons
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decor.setSystemUiVisibility(flags);
        }
    }

    // Keep these methods empty to prevent errors in existing activities
    protected void setupEdgeToEdge(View view) {}
    protected void applySystemWindowInsets(View view) {}
    protected void setupToolbarInsets(View view) {}
}
