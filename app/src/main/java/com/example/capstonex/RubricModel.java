package com.example.capstonex;

public class RubricModel {
    private String id;
    private String criteria;
    private int maxMarks;

    public RubricModel() {}

    public RubricModel(String criteria, int maxMarks) {
        this.criteria = criteria;
        this.maxMarks = maxMarks;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }
    public int getMaxMarks() { return maxMarks; }
    public void setMaxMarks(int maxMarks) { this.maxMarks = maxMarks; }
}
