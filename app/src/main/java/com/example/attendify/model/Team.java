package com.example.attendify.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Team model class representing a team within a department.
 */
public class Team {
    private String id;
    private String name;
    private String description;
    private String departmentId;
    private String leaderId; // UID of the team leader
    private List<String> memberIds; // List of UIDs of team members
    private Date createdAt;
    private boolean active;

    // Required empty constructor for Firestore
    public Team() {
        // Required for Firestore
    }

    public Team(String name, String description, String departmentId) {
        this.name = name;
        this.description = description;
        this.departmentId = departmentId;
        this.memberIds = new ArrayList<>();
        this.active = true;
        this.createdAt = new Date();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("departmentId", departmentId);
        map.put("leaderId", leaderId);
        map.put("memberIds", memberIds);
        map.put("createdAt", createdAt);
        map.put("active", active);
        return map;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public void addMember(String userId) {
        if (this.memberIds == null) {
            this.memberIds = new ArrayList<>();
        }
        if (!this.memberIds.contains(userId)) {
            this.memberIds.add(userId);
        }
    }

    public void removeMember(String userId) {
        if (this.memberIds != null) {
            this.memberIds.remove(userId);
        }
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
} 