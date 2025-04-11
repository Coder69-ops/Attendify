package com.example.attendify.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing an attendance record in the system
 * Firestore path: /attendance/{uid}/{date}
 */
public class Attendance {
    private String userId;
    private String userName;
    private String officeName;
    private String date; // yyyy-MM-dd format
    private Date checkInTime;
    private Date checkOutTime;
    private String status; // "OnTime", "Late", "Missed"
    private String officeId;
    private String locationStatus; // "InOffice", "OutOfOffice", "Unknown"

    // Default constructor required for Firestore
    public Attendance() {
    }

    public Attendance(String userId, String date, Date checkInTime, String status, String officeId) {
        this.userId = userId;
        this.date = date;
        this.checkInTime = checkInTime;
        this.status = status;
        this.officeId = officeId;
        this.locationStatus = "Unknown";
    }

    // Convert Attendance object to Firestore document
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("officeName", officeName);
        map.put("date", date);
        map.put("checkInTime", checkInTime);
        if (checkOutTime != null) {
            map.put("checkOutTime", checkOutTime);
        }
        map.put("status", status);
        map.put("officeId", officeId);
        map.put("locationStatus", locationStatus);
        return map;
    }

    // Getters and setters
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

    public String getOfficeName() {
        return officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Date getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Date checkInTime) {
        this.checkInTime = checkInTime;
    }

    public Date getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Date checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOfficeId() {
        return officeId;
    }

    public void setOfficeId(String officeId) {
        this.officeId = officeId;
    }

    public String getLocationStatus() {
        return locationStatus;
    }

    public void setLocationStatus(String locationStatus) {
        this.locationStatus = locationStatus;
    }

    public boolean isOnTime() {
        return "OnTime".equals(status);
    }

    public boolean isLate() {
        return "Late".equals(status);
    }

    public boolean isMissed() {
        return "Missed".equals(status);
    }

    public boolean isInOffice() {
        return "InOffice".equals(locationStatus);
    }

    public boolean isOutOfOffice() {
        return "OutOfOffice".equals(locationStatus);
    }

    public String getId() {
        return userId + "_" + date;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Attendance that = (Attendance) obj;
        
        if (userId != null ? !userId.equals(that.userId) : that.userId != null) return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) return false;
        if (officeName != null ? !officeName.equals(that.officeName) : that.officeName != null) return false;
        if (checkInTime != null ? !checkInTime.equals(that.checkInTime) : that.checkInTime != null) return false;
        if (checkOutTime != null ? !checkOutTime.equals(that.checkOutTime) : that.checkOutTime != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (officeId != null ? !officeId.equals(that.officeId) : that.officeId != null) return false;
        return locationStatus != null ? locationStatus.equals(that.locationStatus) : that.locationStatus == null;
    }

    @Override
    public int hashCode() {
        int result = userId != null ? userId.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (officeName != null ? officeName.hashCode() : 0);
        result = 31 * result + (checkInTime != null ? checkInTime.hashCode() : 0);
        result = 31 * result + (checkOutTime != null ? checkOutTime.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (officeId != null ? officeId.hashCode() : 0);
        result = 31 * result + (locationStatus != null ? locationStatus.hashCode() : 0);
        return result;
    }
}