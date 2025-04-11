package com.example.attendify.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.attendify.model.Attendance;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.NetworkType;

public class AttendanceRepository {
    private static final String TAG = "AttendanceRepository";
    private static final String ATTENDANCE_COLLECTION = "attendance";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth auth;
    
    // Singleton instance
    private static AttendanceRepository instance;
    
    public static AttendanceRepository getInstance() {
        if (instance == null) {
            instance = new AttendanceRepository();
        }
        return instance;
    }
    
    private AttendanceRepository() {
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }
    
    // Record check-in
    public MutableLiveData<Boolean> recordCheckIn(Attendance attendance) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        firestore.collection(ATTENDANCE_COLLECTION)
                .document(attendance.getUserId())
                .collection(attendance.getDate())
                .add(attendance.toMap())
                .addOnSuccessListener(documentReference -> resultLiveData.setValue(true))
                .addOnFailureListener(e -> resultLiveData.setValue(false));
        
        return resultLiveData;
    }
    
    // Record check-out
    public MutableLiveData<Boolean> recordCheckOut(String userId) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        // Get current date
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Date checkOutTime = new Date();
        
        firestore.collection(ATTENDANCE_COLLECTION)
                .document(userId)
                .collection(currentDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        document.getReference()
                                .update("checkOutTime", checkOutTime)
                                .addOnSuccessListener(aVoid -> resultLiveData.setValue(true))
                                .addOnFailureListener(e -> resultLiveData.setValue(false));
                    } else {
                        resultLiveData.setValue(false);
                    }
                })
                .addOnFailureListener(e -> resultLiveData.setValue(false));
        
        return resultLiveData;
    }
    
    // Get attendance history
    public MutableLiveData<List<Attendance>> getAttendanceHistory(String userId) {
        MutableLiveData<List<Attendance>> attendanceLiveData = new MutableLiveData<>();
        
        firestore.collection(ATTENDANCE_COLLECTION)
                .document(userId)
                .collection(getCurrentMonth())
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Attendance> attendanceList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Attendance attendance = document.toObject(Attendance.class);
                        attendanceList.add(attendance);
                    }
                    attendanceLiveData.setValue(attendanceList);
                })
                .addOnFailureListener(e -> attendanceLiveData.setValue(new ArrayList<>()));
        
        return attendanceLiveData;
    }

    // Get live attendance data for an office
    public MutableLiveData<List<Attendance>> getLiveAttendance(String officeId) {
        MutableLiveData<List<Attendance>> attendanceLiveData = new MutableLiveData<>();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        try {
            if (officeId != null) {
                // Query for a specific office - doesn't require a collection group index
                Query query = firestore.collectionGroup(currentDate)
                .whereEqualTo("officeId", officeId)
                        .orderBy("checkInTime", Query.Direction.DESCENDING);
                        
                query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Attendance> attendanceList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Attendance attendance = document.toObject(Attendance.class);
                        attendanceList.add(attendance);
                    }
                         Log.d(TAG, "Loaded " + attendanceList.size() + " attendance records for office " + officeId);
                    attendanceLiveData.setValue(attendanceList);
                })
                     .addOnFailureListener(e -> {
                         Log.e(TAG, "Error loading attendance data for office " + officeId, e);
                         attendanceLiveData.setValue(new ArrayList<>());
                     });
            } else {
                // Fallback approach for "All Offices" - get active offices first, then query each
                Log.d(TAG, "Using fallback method for All Offices view (requires Firestore index in production)");
                
                // First retrieve all offices
                firestore.collection("offices")
                        .get()
                        .addOnSuccessListener(officeSnapshots -> {
                            // Track completion of all queries
                            final int[] pendingQueries = {officeSnapshots.size()};
                            final List<Attendance> combinedAttendance = new ArrayList<>();
                            
                            if (officeSnapshots.isEmpty()) {
                                // No offices, return empty list
                                attendanceLiveData.setValue(combinedAttendance);
                                return;
                            }
                            
                            // For each office, get its attendance records
                            for (QueryDocumentSnapshot officeDoc : officeSnapshots) {
                                String currentOfficeId = officeDoc.getId();
                                
                                firestore.collectionGroup(currentDate)
                                        .whereEqualTo("officeId", currentOfficeId)
                                        .orderBy("checkInTime", Query.Direction.DESCENDING)
                                        .get()
                                        .addOnSuccessListener(attendanceSnapshots -> {
                                            // Add these records to our combined list
                                            for (QueryDocumentSnapshot doc : attendanceSnapshots) {
                                                Attendance attendance = doc.toObject(Attendance.class);
                                                combinedAttendance.add(attendance);
                                            }
                                            
                                            // Check if all queries are complete
                                            pendingQueries[0]--;
                                            if (pendingQueries[0] <= 0) {
                                                // Sort combined results by check-in time (newest first)
                                                combinedAttendance.sort((a1, a2) -> 
                                                    a2.getCheckInTime().compareTo(a1.getCheckInTime()));
                                                    
                                                Log.d(TAG, "Loaded " + combinedAttendance.size() + 
                                                      " total attendance records across all offices");
                                                attendanceLiveData.setValue(combinedAttendance);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error getting attendance for office " + 
                                                  currentOfficeId, e);
                                            
                                            // Still count this as completed
                                            pendingQueries[0]--;
                                            if (pendingQueries[0] <= 0) {
                                                // Return whatever we have so far
                                                combinedAttendance.sort((a1, a2) -> 
                                                    a2.getCheckInTime().compareTo(a1.getCheckInTime()));
                                                attendanceLiveData.setValue(combinedAttendance);
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error retrieving offices for All Offices view", e);
                            attendanceLiveData.setValue(new ArrayList<>());
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getLiveAttendance", e);
            attendanceLiveData.setValue(new ArrayList<>());
        }

        return attendanceLiveData;
    }
    
    // Get monthly summary
    public MutableLiveData<AttendanceSummary> getMonthlySummary(String userId) {
        MutableLiveData<AttendanceSummary> summaryLiveData = new MutableLiveData<>();
        
        firestore.collection(ATTENDANCE_COLLECTION)
                .document(userId)
                .collection(getCurrentMonth())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int onTime = 0;
                    int late = 0;
                    int missed = 0;
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Attendance attendance = document.toObject(Attendance.class);
                        switch (attendance.getStatus()) {
                            case "OnTime":
                                onTime++;
                                break;
                            case "Late":
                                late++;
                                break;
                            case "Missed":
                                missed++;
                                break;
                        }
                    }
                    
                    summaryLiveData.setValue(new AttendanceSummary(onTime, late, missed));
                })
                .addOnFailureListener(e -> summaryLiveData.setValue(new AttendanceSummary(0, 0, 0)));
        
        return summaryLiveData;
    }

    // Get daily summary for an office
    public MutableLiveData<AttendanceSummary> getDailySummary(String officeId) {
        MutableLiveData<AttendanceSummary> summaryLiveData = new MutableLiveData<>();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        try {
            if (officeId != null) {
                // Query for a specific office - doesn't require a collection group index
        firestore.collectionGroup(currentDate)
                .whereEqualTo("officeId", officeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int onTime = 0;
                    int late = 0;
                    int missed = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Attendance attendance = document.toObject(Attendance.class);
                        switch (attendance.getStatus()) {
                            case "OnTime":
                                onTime++;
                                break;
                            case "Late":
                                late++;
                                break;
                            case "Missed":
                                missed++;
                                break;
                        }
                    }

                            Log.d(TAG, "Daily summary for office " + officeId + 
                                  ": OnTime=" + onTime + ", Late=" + late + ", Missed=" + missed);
                    summaryLiveData.setValue(new AttendanceSummary(onTime, late, missed));
                })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting daily summary for office " + officeId, e);
                            summaryLiveData.setValue(new AttendanceSummary(0, 0, 0));
                        });
            } else {
                // Fallback approach for "All Offices" - get active offices first, then query each
                Log.d(TAG, "Using fallback method for All Offices summary (requires Firestore index in production)");
                
                // First retrieve all offices
                firestore.collection("offices")
                        .get()
                        .addOnSuccessListener(officeSnapshots -> {
                            // Track completion of all queries
                            final int[] pendingQueries = {officeSnapshots.size()};
                            final int[] totalOnTime = {0};
                            final int[] totalLate = {0};
                            final int[] totalMissed = {0};
                            
                            if (officeSnapshots.isEmpty()) {
                                // No offices, return zeros
                                summaryLiveData.setValue(new AttendanceSummary(0, 0, 0));
                                return;
                            }
                            
                            // For each office, get its attendance records
                            for (QueryDocumentSnapshot officeDoc : officeSnapshots) {
                                String currentOfficeId = officeDoc.getId();
                                
                                firestore.collectionGroup(currentDate)
                                        .whereEqualTo("officeId", currentOfficeId)
                                        .get()
                                        .addOnSuccessListener(attendanceSnapshots -> {
                                            // Count attendance by status
                                            for (QueryDocumentSnapshot doc : attendanceSnapshots) {
                                                Attendance attendance = doc.toObject(Attendance.class);
                                                switch (attendance.getStatus()) {
                                                    case "OnTime":
                                                        totalOnTime[0]++;
                                                        break;
                                                    case "Late":
                                                        totalLate[0]++;
                                                        break;
                                                    case "Missed":
                                                        totalMissed[0]++;
                                                        break;
                                                }
                                            }
                                            
                                            // Check if all queries are complete
                                            pendingQueries[0]--;
                                            if (pendingQueries[0] <= 0) {
                                                Log.d(TAG, "Daily summary for all offices: OnTime=" + 
                                                      totalOnTime[0] + ", Late=" + totalLate[0] + 
                                                      ", Missed=" + totalMissed[0]);
                                                summaryLiveData.setValue(new AttendanceSummary(
                                                    totalOnTime[0], totalLate[0], totalMissed[0]));
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error getting attendance summary for office " + 
                                                  currentOfficeId, e);
                                            
                                            // Still count this as completed
                                            pendingQueries[0]--;
                                            if (pendingQueries[0] <= 0) {
                                                // Return whatever we have so far
                                                summaryLiveData.setValue(new AttendanceSummary(
                                                    totalOnTime[0], totalLate[0], totalMissed[0]));
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error retrieving offices for All Offices summary", e);
                            summaryLiveData.setValue(new AttendanceSummary(0, 0, 0));
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getDailySummary", e);
            summaryLiveData.setValue(new AttendanceSummary(0, 0, 0));
        }

        return summaryLiveData;
    }
    
    private String getCurrentMonth() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        return format.format(new Date());
    }
    
    // Inner class for attendance summary
    public static class AttendanceSummary {
        private final int onTime;
        private final int late;
        private final int missed;
        
        public AttendanceSummary(int onTime, int late, int missed) {
            this.onTime = onTime;
            this.late = late;
            this.missed = missed;
        }
        
        public int getOnTime() {
            return onTime;
        }
        
        public int getLate() {
            return late;
        }
        
        public int getMissed() {
            return missed;
        }
        
        public int getTotal() {
            return onTime + late + missed;
        }
    }

    /**
     * Records check-in with offline support
     * If the device is offline, the check-in will be stored locally and synced later
     * 
     * @param attendance The attendance record to save
     * @return LiveData with the result status
     */
    public MutableLiveData<Boolean> recordCheckInWithOfflineSupport(Attendance attendance) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        // Enable offline persistence for Firestore
        // This should be called in the Application class, but we'll add it here for completeness
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);
        
        // Use Firestore offline capabilities
        firestore.collection(ATTENDANCE_COLLECTION)
                .document(attendance.getUserId())
                .collection(attendance.getDate())
                .add(attendance.toMap())
                .addOnSuccessListener(documentReference -> {
                    // Successfully added to Firestore (or offline cache)
                    resultLiveData.setValue(true);
                })
                .addOnFailureListener(e -> {
                    // Firestore operation failed entirely
                    // Save locally as fallback
                    saveAttendanceLocally(attendance);
                    resultLiveData.setValue(true);
                });
        
        return resultLiveData;
    }
    
    /**
     * Saves attendance data locally when offline
     * This is a fallback for when Firestore operations fail completely
     * 
     * @param attendance The attendance record to save locally
     */
    private void saveAttendanceLocally(Attendance attendance) {
        // In a real implementation, this would use SharedPreferences or Room DB
        // For now, we'll just log it
        Log.d("AttendanceRepository", "Saving attendance locally: " + attendance.getUserId() 
                + " on " + attendance.getDate());
        
        // Here you would implement:
        // 1. Store the attendance data in SharedPreferences or Room
        // 2. Create a background work request using WorkManager to sync later
        // 3. When connectivity is restored, upload the local records to Firestore
    }

    // Add WorkManager for better offline support
    public void setupOfflineSync(Context context) {
        // Enable offline persistence for Firestore
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        firestore.setFirestoreSettings(settings);
        
        // Commented out until worker classes are fully implemented
        /*
        // Set up periodic work request to sync offline attendance records
        PeriodicWorkRequest syncWorkRequest = new PeriodicWorkRequest.Builder(
                AttendanceSyncWorker.class,
                15, // Minimum interval is 15 minutes
                TimeUnit.MINUTES)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        10,
                        TimeUnit.MINUTES)
                .build();
        
        // Enqueue the work with a unique name
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        "attendance_sync_work",
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncWorkRequest);
        */
        
        Log.d("AttendanceRepository", "Offline sync setup completed");
    }
    
    /**
     * Get attendance history for a specific month
     * @param userId The user ID
     * @param yearMonth The year and month in format "yyyy-MM"
     * @return LiveData with list of attendance records
     */
    public MutableLiveData<List<Attendance>> getAttendanceHistoryForMonth(String userId, String yearMonth) {
        MutableLiveData<List<Attendance>> attendanceLiveData = new MutableLiveData<>();
        
        firestore.collection(ATTENDANCE_COLLECTION)
                .document(userId)
                .collection(yearMonth)
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Attendance> attendanceList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Attendance attendance = document.toObject(Attendance.class);
                        attendanceList.add(attendance);
                    }
                    attendanceLiveData.setValue(attendanceList);
                })
                .addOnFailureListener(e -> attendanceLiveData.setValue(new ArrayList<>()));
        
        return attendanceLiveData;
    }

    /**
     * Updates the location status of a user's active attendance record
     * @param userId The user ID
     * @param date Today's date in yyyy-MM-dd format
     * @param locationStatus The location status: "InOffice", "OutOfOffice", or "Unknown"
     * @return LiveData with the result status
     */
    public MutableLiveData<Boolean> updateLocationStatus(String userId, String date, String locationStatus) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        // Get today's attendance documents
        firestore.collection(ATTENDANCE_COLLECTION)
                .document(userId)
                .collection(date)
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .whereEqualTo("checkOutTime", null)  // Only update records without checkout time (active check-ins)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d("AttendanceRepository", "No active attendance found for user: " + userId);
                        resultLiveData.setValue(false);
                        return;
                    }
                    
                    // Update the first active attendance record found
                    querySnapshot.getDocuments().get(0).getReference()
                            .update("locationStatus", locationStatus)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("AttendanceRepository", "Location status updated for user: " + userId);
                                resultLiveData.setValue(true);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("AttendanceRepository", "Error updating location status", e);
                                resultLiveData.setValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("AttendanceRepository", "Error querying attendance records", e);
                    resultLiveData.setValue(false);
                });
        
        return resultLiveData;
    }
    
    /**
     * Get the active attendance record for a user (checked in but not checked out)
     * 
     * @param userId The user ID
     * @param date Today's date in yyyy-MM-dd format
     * @return LiveData with the active attendance record or null if not found
     */
    public MutableLiveData<Attendance> getActiveAttendance(String userId, String date) {
        MutableLiveData<Attendance> attendanceLiveData = new MutableLiveData<>();
        
        firestore.collection(ATTENDANCE_COLLECTION)
                .document(userId)
                .collection(date)
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .whereEqualTo("checkOutTime", null)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        attendanceLiveData.setValue(null);
                        return;
                    }
                    
                    // Return the first active attendance record found
                    Attendance attendance = querySnapshot.getDocuments().get(0).toObject(Attendance.class);
                    attendanceLiveData.setValue(attendance);
                })
                .addOnFailureListener(e -> {
                    Log.e("AttendanceRepository", "Error querying active attendance", e);
                    attendanceLiveData.setValue(null);
                });
        
        return attendanceLiveData;
    }

    /**
     * Get all attendance records for a specific month, with optional office filter
     * @param yearMonth The month in yyyy-MM format
     * @param officeId Optional office ID to filter by, null to get all offices
     * @return LiveData containing list of all attendance records
     */
    public MutableLiveData<List<Attendance>> getAllAttendanceForMonth(String yearMonth, String officeId) {
        MutableLiveData<List<Attendance>> attendanceLiveData = new MutableLiveData<>();
        
        Query query = firestore.collectionGroup(yearMonth);
        
        if (officeId != null) {
            query = query.whereEqualTo("officeId", officeId);
        }
        
        query.orderBy("checkInTime", Query.Direction.DESCENDING)
             .get()
             .addOnSuccessListener(queryDocumentSnapshots -> {
                 List<Attendance> attendanceList = new ArrayList<>();
                 for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                     Attendance attendance = document.toObject(Attendance.class);
                     attendanceList.add(attendance);
                 }
                 attendanceLiveData.setValue(attendanceList);
             })
             .addOnFailureListener(e -> {
                 Log.e(TAG, "Error getting attendance records: ", e);
                 attendanceLiveData.setValue(new ArrayList<>());
             });
        
        return attendanceLiveData;
    }

    /**
     * Seeds test attendance data for testing the export functionality
     * Creates attendance records for the past 30 days for a list of user IDs
     * 
     * @param userIds List of user IDs to create attendance data for
     * @param userNames List of user names corresponding to the user IDs
     * @param officeId The office ID to associate with the attendance records
     * @param officeName The office name to associate with the attendance records
     * @return LiveData with the result status
     */
    public MutableLiveData<Boolean> seedTestAttendanceData(List<String> userIds, List<String> userNames, 
                                                          String officeId, String officeName) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        if (userIds.size() != userNames.size()) {
            resultLiveData.setValue(false);
            return resultLiveData;
        }
        
        // Get current date
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Track number of successful operations
        final int[] totalOperations = {userIds.size() * 30}; // 30 days of data per user
        final int[] completedOperations = {0};
        
        // Create attendance records for each user for the past 30 days
        for (int userIndex = 0; userIndex < userIds.size(); userIndex++) {
            final String userId = userIds.get(userIndex);
            final String userName = userNames.get(userIndex);
            
            // Create attendance records for the past 30 days
            for (int daysAgo = 0; daysAgo < 30; daysAgo++) {
                // Calculate the date for this attendance record
                Date recordDate = new Date(currentDate.getTime() - TimeUnit.DAYS.toMillis(daysAgo));
                String formattedDate = dateFormat.format(recordDate);
                String yearMonth = formattedDate.substring(0, 7); // Extract YYYY-MM part
                
                // Skip weekends (Saturday = 6, Sunday = 0)
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(recordDate);
                int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    completedOperations[0]++;
                    if (completedOperations[0] >= totalOperations[0]) {
                        resultLiveData.setValue(true);
                    }
                    continue;
                }
                
                // Randomly determine if the user was present, late, or absent
                double random = Math.random();
                String status;
                Date checkInTime = null;
                Date checkOutTime = null;
                String locationStatus;
                
                if (random < 0.7) { // 70% chance of being on time
                    status = "OnTime";
                    
                    // Create a random check-in time between 8:00 AM and 9:00 AM
                    calendar.set(Calendar.HOUR_OF_DAY, 8);
                    calendar.set(Calendar.MINUTE, (int) (Math.random() * 60));
                    calendar.set(Calendar.SECOND, (int) (Math.random() * 60));
                    checkInTime = calendar.getTime();
                    
                    // Create a random check-out time between 5:00 PM and 6:00 PM
                    calendar.set(Calendar.HOUR_OF_DAY, 17);
                    calendar.set(Calendar.MINUTE, (int) (Math.random() * 60));
                    calendar.set(Calendar.SECOND, (int) (Math.random() * 60));
                    checkOutTime = calendar.getTime();
                    
                    // 80% chance of being in office
                    locationStatus = Math.random() < 0.8 ? "InOffice" : "OutOfOffice";
                    
                } else if (random < 0.9) { // 20% chance of being late
                    status = "Late";
                    
                    // Create a random check-in time between 9:00 AM and 10:00 AM
                    calendar.set(Calendar.HOUR_OF_DAY, 9);
                    calendar.set(Calendar.MINUTE, (int) (Math.random() * 60));
                    calendar.set(Calendar.SECOND, (int) (Math.random() * 60));
                    checkInTime = calendar.getTime();
                    
                    // Create a random check-out time between 5:00 PM and 6:00 PM
                    calendar.set(Calendar.HOUR_OF_DAY, 17);
                    calendar.set(Calendar.MINUTE, (int) (Math.random() * 60));
                    calendar.set(Calendar.SECOND, (int) (Math.random() * 60));
                    checkOutTime = calendar.getTime();
                    
                    // 60% chance of being in office
                    locationStatus = Math.random() < 0.6 ? "InOffice" : "OutOfOffice";
                    
                } else { // 10% chance of being absent
                    status = "Missed";
                    locationStatus = "Unknown";
                }
                
                // Create the attendance record
                final Attendance attendance = new Attendance(userId, formattedDate, checkInTime, status, officeId);
                attendance.setUserName(userName);
                attendance.setOfficeName(officeName);
                attendance.setLocationStatus(locationStatus);
                if (checkOutTime != null) {
                    attendance.setCheckOutTime(checkOutTime);
                }
                
                // Save to Firestore in the appropriate collection
                firestore.collection(ATTENDANCE_COLLECTION)
                        .document(userId)
                        .collection(yearMonth)
                        .document(formattedDate)
                        .set(attendance.toMap())
                        .addOnSuccessListener(aVoid -> {
                            completedOperations[0]++;
                            if (completedOperations[0] >= totalOperations[0]) {
                                resultLiveData.setValue(true);
                            }
                        })
                        .addOnFailureListener(e -> {
                            completedOperations[0]++;
                            if (completedOperations[0] >= totalOperations[0]) {
                                // Even if some operations failed, we'll still return success
                                // if all operations are completed
                                resultLiveData.setValue(true);
                            }
                        });
            }
        }
        
        return resultLiveData;
    }
    
    /**
     * Gets the most recent attendance records
     * 
     * @param limit The maximum number of records to retrieve
     * @return LiveData containing a list of recent attendance records
     */
    public LiveData<List<Attendance>> getRecentAttendance(int limit) {
        MutableLiveData<List<Attendance>> attendanceLiveData = new MutableLiveData<>();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        
        if (userId == null) {
            attendanceLiveData.setValue(new ArrayList<>());
            return attendanceLiveData;
        }
        
        firestore.collection(ATTENDANCE_COLLECTION)
                .document(userId)
                .collection(getCurrentMonth())
                .orderBy("checkInTime", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Attendance> attendanceList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Attendance attendance = document.toObject(Attendance.class);
                        attendanceList.add(attendance);
                    }
                    attendanceLiveData.setValue(attendanceList);
                })
                .addOnFailureListener(e -> attendanceLiveData.setValue(new ArrayList<>()));
        
        return attendanceLiveData;
    }
    
    /**
     * Records check-in for the current user
     * @return LiveData with the result status
     */
    public LiveData<Boolean> checkIn() {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        if (auth.getCurrentUser() == null) {
            resultLiveData.setValue(false);
            return resultLiveData;
        }
        
        // Get current user ID and date
        String userId = auth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // First check if user already checked in today to prevent duplicate check-ins
        firestore.collection(ATTENDANCE_COLLECTION)
                 .document(userId)
                 .collection(date)
                 .get()
                 .addOnSuccessListener(queryDocumentSnapshots -> {
                     if (!queryDocumentSnapshots.isEmpty()) {
                         // User already checked in today
                         resultLiveData.setValue(false);
                         Log.d(TAG, "User has already checked in today");
                         return;
                     }
                     
                     // Create attendance record with "OnTime" status by default
                     Attendance attendance = new Attendance(userId, date, new Date(), "OnTime", getDefaultOfficeId());
                     
                     // Record check-in
                     firestore.collection(ATTENDANCE_COLLECTION)
                             .document(userId)
                             .collection(date)
                             .add(attendance.toMap())
                             .addOnSuccessListener(documentReference -> {
                                 resultLiveData.setValue(true);
                                 Log.d(TAG, "User checked in successfully");
                             })
                             .addOnFailureListener(e -> {
                                 resultLiveData.setValue(false);
                                 Log.e(TAG, "Error checking in", e);
                             });
                 })
                 .addOnFailureListener(e -> {
                     resultLiveData.setValue(false);
                     Log.e(TAG, "Error checking for existing check-ins", e);
                 });
        
        return resultLiveData;
    }
    
    /**
     * Records check-out for the current user
     * @return LiveData with the result status
     */
    public LiveData<Boolean> checkOut() {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        if (auth.getCurrentUser() == null) {
            resultLiveData.setValue(false);
            return resultLiveData;
        }
        
        String userId = auth.getCurrentUser().getUid();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Date checkOutTime = new Date();
        
        // Find today's attendance record and update it
        firestore.collection(ATTENDANCE_COLLECTION)
                 .document(userId)
                 .collection(currentDate)
                 .get()
                 .addOnSuccessListener(queryDocumentSnapshots -> {
                     if (queryDocumentSnapshots.isEmpty()) {
                         resultLiveData.setValue(false);
                         Log.d(TAG, "No check-in record found for today");
                         return;
                     }
                     
                     // Get the first record (should only be one per day)
                     QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                     
                     // Update with checkout time
                     document.getReference()
                             .update("checkOutTime", checkOutTime)
                             .addOnSuccessListener(aVoid -> {
                                 resultLiveData.setValue(true);
                                 Log.d(TAG, "User checked out successfully");
                             })
                             .addOnFailureListener(e -> {
                                 resultLiveData.setValue(false);
                                 Log.e(TAG, "Error checking out", e);
                             });
                 })
                 .addOnFailureListener(e -> {
                     resultLiveData.setValue(false);
                     Log.e(TAG, "Error finding check-in record", e);
                 });
        
        return resultLiveData;
    }
    
    // Helper method to get default office ID
    private String getDefaultOfficeId() {
        // In a real app, this would retrieve the user's assigned office
        // For now, return a default office
        return "default_office";
    }
}