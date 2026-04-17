package com.example.capstonex;

import java.util.List;

public class MentorModel {
    private String id;
    private String name;
    private String domain;
    private List<String> assignedGroupIds;

    public MentorModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public List<String> getAssignedGroupIds() { return assignedGroupIds; }
    public void setAssignedGroupIds(List<String> assignedGroupIds) { this.assignedGroupIds = assignedGroupIds; }
}
