package com.example.capstonex;

import com.google.firebase.Timestamp;

public class NotificationModel {
    private String id;
    private String title;
    private String message;
    private Timestamp timestamp;
    private boolean read;

    public NotificationModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
