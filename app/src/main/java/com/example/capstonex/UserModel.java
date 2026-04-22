package com.example.capstonex;

public class UserModel {
    private String uid;
    private String name;
    private String email;
    private String role;
    private String domain; // Specific for mentors
    private String groupId; // Specific for students
    private boolean hasGroup;

    public UserModel() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public boolean isHasGroup() { return hasGroup; }
    public void setHasGroup(boolean hasGroup) { this.hasGroup = hasGroup; }
}
