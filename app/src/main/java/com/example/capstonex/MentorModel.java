package com.example.capstonex;

import java.util.List;

public class MentorModel {
    private String uid;
    private String name;
    private String email;
    private String mentorId;
    private String profileImageUrl;
    private String domain;
    private List<String> assignedGroupIds;

    public MentorModel() {
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMentorId() {
        return mentorId;
    }

    public void setMentorId(String mentorId) {
        this.mentorId = mentorId;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<String> getAssignedGroupIds() {
        return assignedGroupIds;
    }

    public void setAssignedGroupIds(List<String> assignedGroupIds) {
        this.assignedGroupIds = assignedGroupIds;
    }
}
