package com.example.capstonex;

import com.google.firebase.Timestamp;

public class MeetingModel {
    private String id;
    private String title;
    private String link;
    private Timestamp dateTime;
    private String notes;

    public MeetingModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public Timestamp getDateTime() { return dateTime; }
    public void setDateTime(Timestamp dateTime) { this.dateTime = dateTime; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
