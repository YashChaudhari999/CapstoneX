package com.example.capstonex;

import com.google.firebase.Timestamp;

public class DocumentModel {
    private String id;
    private String title;
    private String type;
    private String downloadUrl;
    private Timestamp timestamp;

    public DocumentModel() {}

    public DocumentModel(String id, String title, String type, String downloadUrl, Timestamp timestamp) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.downloadUrl = downloadUrl;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
