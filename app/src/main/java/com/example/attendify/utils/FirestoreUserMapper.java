package com.example.attendify.utils;

import com.example.attendify.model.User;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

/**
 * Utility class to handle mapping User model from Firestore
 */
public class FirestoreUserMapper {

    /**
     * Convert from DocumentSnapshot to User model
     * Handles potential field name differences in the database
     */
    public static User documentToUser(DocumentSnapshot document) {
        if (document == null || !document.exists()) {
            return null;
        }

        User user = new User();
        
        // Set basic fields
        user.setUid(document.getId());
        user.setEmail(document.getString("email"));
        user.setName(document.getString("name") != null ? 
                document.getString("name") : document.getString("fullName"));
        user.setRole(document.getString("role"));
        
        // Handle different field names
        user.setOfficeId(document.getString("officeId") != null ?
                document.getString("officeId") : document.getString("office"));
        user.setDepartmentId(document.getString("departmentId") != null ?
                document.getString("departmentId") : document.getString("department"));
        user.setTeamId(document.getString("teamId") != null ?
                document.getString("teamId") : document.getString("team"));
        user.setManagerId(document.getString("managerId") != null ?
                document.getString("managerId") : document.getString("manager"));
        
        // Handle boolean fields
        if (document.contains("isApproved")) {
            user.setApproved(document.getBoolean("isApproved"));
        } else if (document.contains("status")) {
            user.setStatus(document.getString("status"));
        }
        
        if (document.contains("isManager")) {
            user.setManager(document.getBoolean("isManager"));
        }
        
        if (document.contains("active")) {
            user.setActive(document.getBoolean("active"));
        }
        
        // Handle profile image
        user.setProfileImageUrl(document.getString("profileImageUrl"));
        
        // Handle dates
        if (document.contains("createdAt")) {
            Object createdAt = document.get("createdAt");
            if (createdAt instanceof Date) {
                user.setCreatedAt((Date) createdAt);
            } else if (createdAt instanceof Long) {
                user.setCreatedAt(new Date((Long) createdAt));
            }
        }
        
        if (document.contains("lastLogin")) {
            Object lastLogin = document.get("lastLogin");
            if (lastLogin instanceof Date) {
                user.setLastLogin((Date) lastLogin);
            } else if (lastLogin instanceof Long) {
                user.setLastLogin(new Date((Long) lastLogin));
            }
        }
        
        return user;
    }
} 