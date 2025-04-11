package com.example.attendify.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a user in the system
 */
public class User implements Serializable {
    
    private String id;
    private String fullName;
    private String email;
    private String employeeId;
    private String password; // Note: This should be handled more securely in a real app
    private String role;
    private String office;
    private String department;
    private String team;
    private String manager;
    private boolean isActive = true;
    private boolean isManager = false;
    private boolean isApproved = false;
    private String profileImageUrl;
    private long createdAt;
    private long lastLogin;
    private Map<String, Boolean> permissions = new HashMap<>();

    /**
     * Default constructor required for Firestore
     */
    public User() {
        // Default constructor required for Firestore
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Constructor with essential fields
     */
    public User(String id, String fullName, String email, String employeeId) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.employeeId = employeeId;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Convert this User object to a Map, suitable for Firestore storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("fullName", fullName);
        map.put("email", email);
        map.put("employeeId", employeeId);
        map.put("role", role);
        map.put("office", office);
        map.put("department", department);
        map.put("team", team);
        map.put("manager", manager);
        map.put("isActive", isActive);
        map.put("isManager", isManager);
        map.put("isApproved", isApproved);
        map.put("profileImageUrl", profileImageUrl);
        map.put("createdAt", createdAt);
        map.put("lastLogin", lastLogin);
        map.put("permissions", permissions);
        return map;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOffice() {
        return office;
    }

    public void setOffice(String office) {
        this.office = office;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isManager() {
        return isManager;
    }

    public void setManager(boolean manager) {
        isManager = manager;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions = permissions;
    }

    /**
     * Add a permission to this user
     */
    public void addPermission(String permission) {
        this.permissions.put(permission, true);
    }

    /**
     * Remove a permission from this user
     */
    public void removePermission(String permission) {
        this.permissions.put(permission, false);
    }

    /**
     * Check if user has a specific permission
     */
    public boolean hasPermission(String permission) {
        Boolean hasPermission = permissions.get(permission);
        return hasPermission != null && hasPermission;
    }

    /**
     * Check if this user is an admin
     */
    public boolean isAdmin() {
        return "Admin".equalsIgnoreCase(role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return fullName;
    }
} 