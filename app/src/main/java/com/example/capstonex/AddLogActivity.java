package com.example.capstonex;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddLogActivity extends AppCompatActivity {

    TextInputEditText etWorkDone;
    CalendarView etFromDate, etToDate;
    MaterialButton btnCancel, btnAddEntry;
    String fromDate, toDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_log_entry);

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        }
//        toolbar.setNavigationOnClickListener(v -> finish());

        etFromDate = findViewById(R.id.etFromDate);
        etToDate = findViewById(R.id.etToDate);
        etWorkDone = findViewById(R.id.etWorkDone);
        btnCancel = findViewById(R.id.btnCancel);
        btnAddEntry = findViewById(R.id.btnAddEntry);

        btnAddEntry.setOnClickListener(v -> {
            etFromDate.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                @Override
                public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                    // Month is 0-indexed, so we add 1
                    int actualMonth = month + 1;

                    // Format the selected date (e.g., "DD/MM/YYYY")
                    fromDate = dayOfMonth + "/" + actualMonth + "/" + year;
                }
            });
            etToDate.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                @Override
                public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                    // Month is 0-indexed, so we add 1
                    int actualMonth = month + 1;

                    // Format the selected date (e.g., "DD/MM/YYYY")
                    toDate = dayOfMonth + "/" + actualMonth + "/" + year;
                }
            });
//            String toDate = etToDate.getText().toString().trim();
            String workDone = etWorkDone.getText().toString().trim();

            if (fromDate.isEmpty() || toDate.isEmpty() || workDone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Logic to save log entry goes here
            Toast.makeText(this, "Log entry added successfully", Toast.LENGTH_SHORT).show();
            finish();
        });


    }
}
