package com.example.capstonex;

import java.util.List;

public class GroupModel {
    private String id;
    private String groupName;
    private List<String> members;
    private String status;
    private String mentorId;

    public GroupModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }
}
