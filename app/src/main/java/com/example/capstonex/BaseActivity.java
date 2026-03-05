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

        // This is the global fix for the status bar color.
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // Set status bar to your app's maroon brand color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorTopBar));

        // Ensure status bar icons (time, wifi, etc.) are light/white for readability
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = window.getDecorView();
            int flags = decor.getSystemUiVisibility();
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decor.setSystemUiVisibility(flags);
        }
    }

    // The following methods are now empty as they are no longer needed
    // but are kept to prevent compilation errors in existing activities.
    protected void setupEdgeToEdge(View view) {}
    protected void applySystemWindowInsets(View view) {}
}
