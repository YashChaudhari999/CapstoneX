package com.example.capstonex;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class LoginActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        applySystemWindowInsets(findViewById(android.R.id.content));

        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButtonToggleGroup toggleRole = findViewById(R.id.toggleRole);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                Intent intent;
                int checkedId = toggleRole.getCheckedButtonId();

                if (checkedId == R.id.btnMentor) {
                    intent = new Intent(LoginActivity.this, MentorDashboardActivity.class);
                } else if (checkedId == R.id.btnAdmin) {
                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                }

                startActivity(intent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left);
                } else {
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }
                finish();
            });
        }
    }
}
