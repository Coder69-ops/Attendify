package com.example.attendify.model;

import com.google.firebase.firestore.DocumentId;

public class PendingApproval {
    @DocumentId
    private String id;
    private String userId;
    private String userName;
    private String officeId;
    private String officeName;
    private long timestamp;

    // Required empty constructor for Firestore
    public PendingApproval() {
    }

    public PendingApproval(String userId, String userName, String officeId, String officeName) {
        this.userId = userId;
        this.userName = userName;
        this.officeId = officeId;
        this.officeName = officeName;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getOfficeName() {
        return officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}