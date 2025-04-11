package com.example.attendify.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User model class representing a user in the system.
 * Can be either an admin or an employee.
 */
public class User {
    private String uid;
    private String email;
    private String name;
    private String displayName;
    private String role; // "admin", "manager", "supervisor", "employee"
    private String employeeId;
    private String officeId;
    private String departmentId; // New field for department association
    private String teamId; // New field for team association
    private String profileImageUrl;
    private String status; // "pending", "approved", "rejected" (legacy)
    private boolean isApproved; // New field matching database
    private Map<String, Double> location; // Changed from GeoPoint to Map
    private boolean active;
    private Date createdAt;
    private Date lastLogin; // Track last login time
    private String managerId; // ID of the user's manager
    private boolean isManager; // Flag if user manages anyone

    // Required empty constructor for Firestore
    public User() {
        // Required for Firestore
    }

    // Constructor used in our app
    public User(String email, String name, String role, String officeId) {
        this.email = email;
        this.name = name;
        this.role = role;
        this.officeId = officeId;
        this.status = "pending"; // Legacy field
        this.isApproved = false; // New field matching database
        this.active = true;
        this.location = new HashMap<>();
        this.isManager = false;
        this.createdAt = new Date();
    }

    // Constructor matching what's used in UserRepository
    public User(String uid, String name, String email, String role, boolean isApproved, String officeId) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.isApproved = isApproved;
        this.status = isApproved ? "approved" : "pending"; // Legacy field
        this.officeId = officeId;
        this.active = true;
        this.location = new HashMap<>();
        this.isManager = false;
        this.createdAt = new Date();
    }
    
    // New constructor with department and team
    public User(String uid, String name, String email, String role, boolean isApproved, 
                String officeId, String departmentId, String teamId) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.isApproved = isApproved;
        this.status = isApproved ? "approved" : "pending"; // Legacy field
        this.officeId = officeId;
        this.departmentId = departmentId;
        this.teamId = teamId;
        this.active = true;
        this.location = new HashMap<>();
        this.isManager = false;
        this.createdAt = new Date();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("email", email);
        map.put("name", name);
        map.put("displayName", displayName);
        map.put("role", role);
        map.put("employeeId", employeeId);
        map.put("officeId", officeId);
        map.put("departmentId", departmentId);
        map.put("teamId", teamId);
        map.put("profileImageUrl", profileImageUrl);
        map.put("status", status); // Legacy field
        map.put("isApproved", isApproved); // New field matching database
        map.put("location", location);
        map.put("active", active);
        map.put("managerId", managerId);
        map.put("isManager", isManager);
        if (createdAt != null) map.put("createdAt", createdAt);
        if (lastLogin != null) map.put("lastLogin", lastLogin);
        return map;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }
    
    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        // Also update isApproved to keep both fields in sync
        this.isApproved = "approved".equals(status);
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
        // Also update status to keep both fields in sync
        if (approved) {
            this.status = "approved";
        } else if (this.status == null || this.status.equals("approved")) {
            this.status = "pending";
        }
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public String getManagerId() {
        return managerId;
    }

    public void setManagerId(String managerId) {
        this.managerId = managerId;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setManager(boolean manager) {
        isManager = manager;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Map<String, Double> getLocation() {
        return location;
    }

    public void setLocation(Map<String, Double> location) {
        this.location = location;
    }
    
    public void updateLocation(double latitude, double longitude) {
        if (this.location == null) {
            this.location = new HashMap<>();
        }
        this.location.put("latitude", latitude);
        this.location.put("longitude", longitude);
    }
    
    // Helper methods
    public boolean isAdmin() {
        return "admin".equals(role);
    }
    
    public boolean isEmployee() {
        return "employee".equals(role);
    }
    
    // New helper methods for enhanced roles
    public boolean hasManagerRole() {
        return "manager".equals(role);
    }
    
    public boolean isSupervisor() {
        return "supervisor".equals(role);
    }
    
    public boolean isPending() {
        return !isApproved;
    }
    
    // Legacy method
    public boolean isRejected() {
        return "rejected".equals(status);
    }
} 