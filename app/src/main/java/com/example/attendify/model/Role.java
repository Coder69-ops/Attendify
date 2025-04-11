package com.example.attendify.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Role model class representing user roles and permissions.
 */
public class Role {
    private String id;
    private String name;
    private String description;
    private List<String> permissions;
    private Date createdAt;
    private boolean active;
    
    // Define common permission strings
    public static final String PERM_MANAGE_USERS = "manage_users";
    public static final String PERM_MANAGE_DEPARTMENTS = "manage_departments";
    public static final String PERM_MANAGE_TEAMS = "manage_teams";
    public static final String PERM_MANAGE_ROLES = "manage_roles";
    public static final String PERM_VIEW_ATTENDANCE = "view_attendance";
    public static final String PERM_EDIT_ATTENDANCE = "edit_attendance";
    public static final String PERM_APPROVE_REQUESTS = "approve_requests";
    public static final String PERM_MANAGE_OFFICES = "manage_offices";
    public static final String PERM_VIEW_REPORTS = "view_reports";
    public static final String PERM_EXPORT_DATA = "export_data";
    
    // Predefined roles
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_MANAGER = "manager";
    public static final String ROLE_SUPERVISOR = "supervisor";
    public static final String ROLE_EMPLOYEE = "employee";

    // Required empty constructor for Firestore
    public Role() {
        // Required for Firestore
    }

    public Role(String name, String description) {
        this.name = name;
        this.description = description;
        this.permissions = new ArrayList<>();
        this.active = true;
        this.createdAt = new Date();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        map.put("permissions", permissions);
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

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public void addPermission(String permission) {
        if (this.permissions == null) {
            this.permissions = new ArrayList<>();
        }
        if (!this.permissions.contains(permission)) {
            this.permissions.add(permission);
        }
    }

    public void removePermission(String permission) {
        if (this.permissions != null) {
            this.permissions.remove(permission);
        }
    }

    public boolean hasPermission(String permission) {
        return this.permissions != null && this.permissions.contains(permission);
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
    
    /**
     * Creates an admin role with all permissions.
     */
    public static Role createAdminRole() {
        Role adminRole = new Role(ROLE_ADMIN, "Administrator with full system access");
        adminRole.addPermission(PERM_MANAGE_USERS);
        adminRole.addPermission(PERM_MANAGE_DEPARTMENTS);
        adminRole.addPermission(PERM_MANAGE_TEAMS);
        adminRole.addPermission(PERM_MANAGE_ROLES);
        adminRole.addPermission(PERM_VIEW_ATTENDANCE);
        adminRole.addPermission(PERM_EDIT_ATTENDANCE);
        adminRole.addPermission(PERM_APPROVE_REQUESTS);
        adminRole.addPermission(PERM_MANAGE_OFFICES);
        adminRole.addPermission(PERM_VIEW_REPORTS);
        adminRole.addPermission(PERM_EXPORT_DATA);
        return adminRole;
    }
    
    /**
     * Creates a manager role with department management permissions.
     */
    public static Role createManagerRole() {
        Role managerRole = new Role(ROLE_MANAGER, "Department manager with team management capabilities");
        managerRole.addPermission(PERM_MANAGE_TEAMS);
        managerRole.addPermission(PERM_VIEW_ATTENDANCE);
        managerRole.addPermission(PERM_EDIT_ATTENDANCE);
        managerRole.addPermission(PERM_APPROVE_REQUESTS);
        managerRole.addPermission(PERM_VIEW_REPORTS);
        managerRole.addPermission(PERM_EXPORT_DATA);
        return managerRole;
    }
    
    /**
     * Creates a supervisor role with limited management permissions.
     */
    public static Role createSupervisorRole() {
        Role supervisorRole = new Role(ROLE_SUPERVISOR, "Team supervisor with attendance management");
        supervisorRole.addPermission(PERM_VIEW_ATTENDANCE);
        supervisorRole.addPermission(PERM_APPROVE_REQUESTS);
        supervisorRole.addPermission(PERM_VIEW_REPORTS);
        return supervisorRole;
    }
    
    /**
     * Creates a basic employee role with minimal permissions.
     */
    public static Role createEmployeeRole() {
        Role employeeRole = new Role(ROLE_EMPLOYEE, "Regular employee with basic permissions");
        return employeeRole;
    }
} 