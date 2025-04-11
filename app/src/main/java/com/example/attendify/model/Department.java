package com.example.attendify.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Department model class representing a department in the organization.
 */
public class Department {
    private String id;
    private String name;
    private String description;
    private String managerId; // UID of the department head
    private List<String> teamIds; // List of team IDs within this department
    private Date createdAt;
    private boolean active;

    // Required empty constructor for Firestore
    public Department() {
        // Required for Firestore
    }

    public Department(String name, String description) {
        this.name = name;
        this.description = description;
        this.teamIds = new ArrayList<>();
        this.active = true;
        this.createdAt = new Date();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("managerId", managerId);
        map.put("teamIds", teamIds);
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

    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public List<String> getTeamIds() {
        return teamIds;
    }

    public void setTeamIds(List<String> teamIds) {
        this.teamIds = teamIds;
    }

    public void addTeam(String teamId) {
        if (this.teamIds == null) {
            this.teamIds = new ArrayList<>();
        }
        this.teamIds.add(teamId);
    }

    public void removeTeam(String teamId) {
        if (this.teamIds != null) {
            this.teamIds.remove(teamId);
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