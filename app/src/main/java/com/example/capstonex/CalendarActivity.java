package com.example.capstonex;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

public class CalendarActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        setupEdgeToEdge(findViewById(R.id.calendar_root));

        MaterialCalendarView calendarView = findViewById(R.id.calendarView);
        MaterialCardView cvEventDetails = findViewById(R.id.cvEventDetails);
        TextView tvEventTitle = findViewById(R.id.tvEventTitle);

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            cvEventDetails.setVisibility(View.VISIBLE);
            tvEventTitle.setText("Deadline: " + date.getDay() + "/" + (date.getMonth()+1));
        });

        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }
}
