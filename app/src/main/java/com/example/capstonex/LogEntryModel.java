package com.example.capstonex;

public class LogEntryModel {
    private String id;
    private String studentUid;
    private String groupId;
    private String workDone;
    private String fromDate;
    private String toDate;
    private String documentUrl;
    private String documentName;
    private long timestamp;
    private boolean isSigned;

    public LogEntryModel() {
        // Required for Firebase
    }

    public LogEntryModel(String id, String studentUid, String groupId, String workDone, String fromDate, String toDate, String documentUrl, String documentName, long timestamp) {
        this.id = id;
        this.studentUid = studentUid;
        this.groupId = groupId;
        this.workDone = workDone;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.documentUrl = documentUrl;
        this.documentName = documentName;
        this.timestamp = timestamp;
        this.isSigned = false;
    }

    public String getId() { return id; }
    public String getStudentUid() { return studentUid; }
    public String getGroupId() { return groupId; }
    public String getWorkDone() { return workDone; }
    public String getFromDate() { return fromDate; }
    public String getToDate() { return toDate; }
    public String getDocumentUrl() { return documentUrl; }
    public String getDocumentName() { return documentName; }
    public long getTimestamp() { return timestamp; }
    public boolean isSigned() { return isSigned; }

    public void setSigned(boolean signed) { isSigned = signed; }
}
