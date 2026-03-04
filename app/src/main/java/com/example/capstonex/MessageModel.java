package com.example.capstonex;

import com.google.firebase.Timestamp;

public class MessageModel {
    private String id;
    private String senderId;
    private String message;
    private Timestamp timestamp;

    public MessageModel() {}

    public MessageModel(String senderId, String message, Timestamp timestamp) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
