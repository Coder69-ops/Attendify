package com.example.attendify.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * PerformanceMetrics model class for tracking employee attendance performance metrics.
 */
public class PerformanceMetrics {
    private String id;
    private String userId;
    private String userName; // Cached for query efficiency
    
    private Date periodStart; // Start of the period (e.g., month)
    private Date periodEnd; // End of the period
    
    private int totalWorkDays; // Total scheduled work days in period
    private int presentDays; // Days present
    private int absentDays; // Days absent
    private int lateDays; // Days when checked in late
    private int leaveCount; // Number of leave days taken
    
    private double averageHoursWorked; // Average hours worked per day
    private double punctualityScore; // Score from 0-100 based on timely check-ins
    private double consistencyScore; // Score from 0-100 based on regular attendance
    private double overallScore; // Weighted overall performance score
    
    private Date updatedAt; // Last update timestamp
    
    // Required empty constructor for Firestore
    public PerformanceMetrics() {
        // Required for Firestore
    }
    
    public PerformanceMetrics(String userId, String userName, Date periodStart, Date periodEnd) {
        this.userId = userId;
        this.userName = userName;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.updatedAt = new Date();
    }
    
    /**
     * Calculates the overall score based on various metrics.
     * Called when saving to ensure score is up to date.
     */
    public void calculateOverallScore() {
        // Calculate punctuality score
        if (totalWorkDays > 0) {
            double onTimeDays = totalWorkDays - lateDays;
            punctualityScore = (onTimeDays / totalWorkDays) * 100;
        }
        
        // Calculate consistency score
        if (totalWorkDays > 0) {
            double attendedDays = presentDays;
            consistencyScore = (attendedDays / totalWorkDays) * 100;
        }
        
        // Calculate overall score (weighted average)
        overallScore = (punctualityScore * 0.4) + (consistencyScore * 0.6);
        
        // Update timestamp
        updatedAt = new Date();
    }
    
    public Map<String, Object> toMap() {
        // Ensure scores are calculated before saving
        calculateOverallScore();
        
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("periodStart", periodStart);
        map.put("periodEnd", periodEnd);
        map.put("totalWorkDays", totalWorkDays);
        map.put("presentDays", presentDays);
        map.put("absentDays", absentDays);
        map.put("lateDays", lateDays);
        map.put("leaveCount", leaveCount);
        map.put("averageHoursWorked", averageHoursWorked);
        map.put("punctualityScore", punctualityScore);
        map.put("consistencyScore", consistencyScore);
        map.put("overallScore", overallScore);
        map.put("updatedAt", updatedAt);
        return map;
    }
    
    // Getters and setters
    
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
    
    public Date getPeriodStart() {
        return periodStart;
    }
    
    public void setPeriodStart(Date periodStart) {
        this.periodStart = periodStart;
    }
    
    public Date getPeriodEnd() {
        return periodEnd;
    }
    
    public void setPeriodEnd(Date periodEnd) {
        this.periodEnd = periodEnd;
    }
    
    public int getTotalWorkDays() {
        return totalWorkDays;
    }
    
    public void setTotalWorkDays(int totalWorkDays) {
        this.totalWorkDays = totalWorkDays;
    }
    
    public int getPresentDays() {
        return presentDays;
    }
    
    public void setPresentDays(int presentDays) {
        this.presentDays = presentDays;
    }
    
    public int getAbsentDays() {
        return absentDays;
    }
    
    public void setAbsentDays(int absentDays) {
        this.absentDays = absentDays;
    }
    
    public int getLateDays() {
        return lateDays;
    }
    
    public void setLateDays(int lateDays) {
        this.lateDays = lateDays;
    }
    
    public int getLeaveCount() {
        return leaveCount;
    }
    
    public void setLeaveCount(int leaveCount) {
        this.leaveCount = leaveCount;
    }
    
    public double getAverageHoursWorked() {
        return averageHoursWorked;
    }
    
    public void setAverageHoursWorked(double averageHoursWorked) {
        this.averageHoursWorked = averageHoursWorked;
    }
    
    public double getPunctualityScore() {
        return punctualityScore;
    }
    
    public void setPunctualityScore(double punctualityScore) {
        this.punctualityScore = punctualityScore;
    }
    
    public double getConsistencyScore() {
        return consistencyScore;
    }
    
    public void setConsistencyScore(double consistencyScore) {
        this.consistencyScore = consistencyScore;
    }
    
    public double getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
} 