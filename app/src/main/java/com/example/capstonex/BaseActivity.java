package com.example.capstonex;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
    }

    /**
     * Standard method to handle window insets.
     * Renamed to handle various references in different activity files.
     */
    protected void applySystemWindowInsets(View rootView) {
        setupEdgeToEdge(rootView);
    }

    /**
     * Alias for applySystemWindowInsets to ensure all activities compile.
     */
    protected void setupEdgeToEdge(View rootView) {
        if (rootView == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
