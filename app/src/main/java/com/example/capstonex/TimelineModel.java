package com.example.capstonex;

public class TimelineModel {
    private String id;
    private String title;
    private String dueDate;
    private String status; // Pending, Completed, Overdue
    private int progress;

    public TimelineModel() {}

    public TimelineModel(String id, String title, String dueDate, String status, int progress) {
        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
        this.status = status;
        this.progress = progress;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
}
