package com.example.capstonex;

import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

public class AnalyticsActivity extends BaseActivity {

    private PieChart pieChart;
    private BarChart barChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);
        setupEdgeToEdge(findViewById(R.id.analytics_root));

        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);

        setupCharts();
        
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());
    }

    private void setupCharts() {
        // Pie Chart
        List<PieEntry> pieEntries = new ArrayList<>();
        pieEntries.add(new PieEntry(70f, "Completed"));
        pieEntries.add(new PieEntry(30f, "In Progress"));

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "Project Completion");
        pieDataSet.setColors(new int[]{Color.parseColor("#28A745"), Color.parseColor("#DC3545")});
        pieChart.setData(new PieData(pieDataSet));
        pieChart.invalidate();

        // Bar Chart
        List<BarEntry> barEntries = new ArrayList<>();
        barEntries.add(new BarEntry(1, 85f));
        barEntries.add(new BarEntry(2, 72f));
        barEntries.add(new BarEntry(3, 90f));

        BarDataSet barDataSet = new BarDataSet(barEntries, "Avg Review Marks");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        barChart.setData(new BarData(barDataSet));
        barChart.invalidate();
    }
}
