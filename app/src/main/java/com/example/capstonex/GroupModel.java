package com.example.capstonex;

import java.util.List;
import java.util.Map;

public class GroupModel {
    private String groupId;
    private String leaderUid;
    private List<String> memberUids;
    private List<Map<String, String>> memberDetails;
    private String status;
    private String mentorUid;
    private long createdAt;

    public GroupModel() {}

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public String getLeaderUid() { return leaderUid; }
    public void setLeaderUid(String leaderUid) { this.leaderUid = leaderUid; }

    public List<String> getMemberUids() { return memberUids; }
    public void setMemberUids(List<String> memberUids) { this.memberUids = memberUids; }

    public List<Map<String, String>> getMemberDetails() { return memberDetails; }
    public void setMemberDetails(List<Map<String, String>> memberDetails) { this.memberDetails = memberDetails; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMentorUid() { return mentorUid; }
    public void setMentorUid(String mentorUid) { this.mentorUid = mentorUid; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
