package com.example.attendify.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Observer;

import com.example.attendify.model.Attendance;
import com.example.attendify.repository.AttendanceRepository;
import com.example.attendify.repository.AttendanceRepository.AttendanceSummary;
import com.example.attendify.util.SingleLiveEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Calendar;
import java.util.ArrayList;

import com.google.firebase.firestore.FirebaseFirestore;

public class AttendanceViewModel extends ViewModel {
    private final AttendanceRepository attendanceRepository;
    private final MutableLiveData<List<Attendance>> attendanceHistoryLiveData;
    private final MutableLiveData<List<Attendance>> liveAttendanceData;
    private final MutableLiveData<AttendanceSummary> monthlySummaryLiveData;
    private final MutableLiveData<AttendanceSummary> dailySummaryLiveData;
    private final SingleLiveEvent<String> errorLiveData;
    private final MutableLiveData<Boolean> loadingLiveData;
    
    // Add new LiveData for location status updates
    private final MutableLiveData<Boolean> locationUpdateLiveData;
    private final MutableLiveData<Attendance> activeAttendanceLiveData;

    private final MutableLiveData<List<Attendance>> recentAttendanceLiveData;

    public AttendanceViewModel() {
        attendanceRepository = AttendanceRepository.getInstance();
        attendanceHistoryLiveData = new MutableLiveData<>();
        liveAttendanceData = new MutableLiveData<>();
        monthlySummaryLiveData = new MutableLiveData<>();
        dailySummaryLiveData = new MutableLiveData<>();
        errorLiveData = new SingleLiveEvent<>();
        loadingLiveData = new MutableLiveData<>(false);
        
        // Initialize new LiveData
        locationUpdateLiveData = new MutableLiveData<>();
        activeAttendanceLiveData = new MutableLiveData<>();

        recentAttendanceLiveData = new MutableLiveData<>();
        refreshAttendanceData();
    }

    public LiveData<List<Attendance>> getAttendanceHistoryLiveData() {
        return attendanceHistoryLiveData;
    }

    public LiveData<List<Attendance>> getLiveAttendanceData() {
        return liveAttendanceData;
    }

    public LiveData<AttendanceSummary> getMonthlySummaryLiveData() {
        return monthlySummaryLiveData;
    }

    public LiveData<AttendanceSummary> getDailySummaryLiveData() {
        return dailySummaryLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public void loadAttendanceHistory(String userId) {
        loadingLiveData.setValue(true);
        attendanceRepository.getAttendanceHistory(userId)
                .observeForever(list -> {
                    loadingLiveData.setValue(false);
                    attendanceHistoryLiveData.setValue(list);
                    if (list == null) {
                        errorLiveData.setValue("Failed to load attendance history");
                    }
                });
    }

    public void loadLiveAttendanceData(String officeId) {
        loadingLiveData.setValue(true);
        
        Log.d("AttendanceViewModel", "Loading live attendance data for " + 
              (officeId == null ? "all offices" : "office: " + officeId));
        
        attendanceRepository.getLiveAttendance(officeId)
                .observeForever(list -> {
                    loadingLiveData.setValue(false);
                    liveAttendanceData.setValue(list);
                    if (list == null) {
                        errorLiveData.setValue("Failed to load live attendance data");
                    }
                });
    }

    public void loadMonthlySummary(String userId) {
        loadingLiveData.setValue(true);
        attendanceRepository.getMonthlySummary(userId)
                .observeForever(summary -> {
                    loadingLiveData.setValue(false);
                    monthlySummaryLiveData.setValue(summary);
                    if (summary == null) {
                        errorLiveData.setValue("Failed to load monthly summary");
                    }
                });
    }

    public void loadDailySummary(String officeId) {
        loadingLiveData.setValue(true);
        
        Log.d("AttendanceViewModel", "Loading daily summary for " + 
              (officeId == null ? "all offices" : "office: " + officeId));
        
        attendanceRepository.getDailySummary(officeId)
                .observeForever(summary -> {
                    loadingLiveData.setValue(false);
                    dailySummaryLiveData.setValue(summary);
                    if (summary == null) {
                        errorLiveData.setValue("Failed to load daily summary");
                    }
                });
    }

    /**
     * Loads attendance history for a specific month
     * @param userId The user ID
     * @param yearMonth The year and month in format "yyyy-MM"
     */
    public void loadAttendanceHistoryForMonth(String userId, String yearMonth) {
        loadingLiveData.setValue(true);
        attendanceRepository.getAttendanceHistoryForMonth(userId, yearMonth)
                .observeForever(list -> {
                    attendanceHistoryLiveData.setValue(list);
                    loadingLiveData.setValue(false);
                });
    }

    /**
     * Records a check-in with a specified status based on entry time
     * This is used to track if the employee is on time or late
     *
     * @param userId The ID of the user checking in
     * @param officeId The ID of the office where check-in occurs
     * @param status The determined status ("OnTime" or "Late")
     */
    public void checkInWithStatus(String userId, String officeId, String status) {
        loadingLiveData.setValue(true);
        
        // Get current date in yyyy-MM-dd format
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Create attendance record with the determined status
        Attendance attendance = new Attendance(userId, date, new Date(), status, officeId);
        
        // Get user name and office name before saving the record
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // First check if user already checked in today to prevent duplicate check-ins
        db.collection("attendance")
          .document(userId)
          .collection(date)
          .get()
          .addOnSuccessListener(queryDocumentSnapshots -> {
              if (!queryDocumentSnapshots.isEmpty()) {
                  // User already checked in today
                 loadingLiveData.setValue(false);
                  errorLiveData.setValue("You have already checked in today");
                  return;
              }
              
              // Fetch user data to get the name
              db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    if (userSnapshot.exists()) {
                        String userName = userSnapshot.getString("name");
                        attendance.setUserName(userName);
                        
                        // Now fetch office data to get the office name
                        db.collection("offices")
                          .document(officeId)
                          .get()
                          .addOnSuccessListener(officeSnapshot -> {
                              if (officeSnapshot.exists()) {
                                  String officeName = officeSnapshot.getString("name");
                                  attendance.setOfficeName(officeName);
                                  
                                  // Now save the complete attendance record
                                  attendanceRepository.recordCheckIn(attendance)
                                      .observeForever(result -> {
                                         loadingLiveData.setValue(false);
                                          if (Boolean.FALSE.equals(result)) {
                                              errorLiveData.setValue("Failed to record check-in");
                             } else {
                                              loadAttendanceHistory(userId);
                                              loadMonthlySummary(userId);
                                          }
                                      });
                              } else {
                                  // Office not found, still save attendance but without office name
                                  attendanceRepository.recordCheckIn(attendance)
                                      .observeForever(result -> {
                                         loadingLiveData.setValue(false);
                                          if (Boolean.FALSE.equals(result)) {
                                              errorLiveData.setValue("Failed to record check-in");
                                          } else {
                                              loadAttendanceHistory(userId);
                                              loadMonthlySummary(userId);
                                          }
                                      });
                              }
                          })
                          .addOnFailureListener(e -> {
                              // Failed to get office, still save attendance but without office name
                              attendanceRepository.recordCheckIn(attendance)
                                  .observeForever(result -> {
                                      loadingLiveData.setValue(false);
                                      if (Boolean.FALSE.equals(result)) {
                                          errorLiveData.setValue("Failed to record check-in");
                                      } else {
                                         loadAttendanceHistory(userId);
                                          loadMonthlySummary(userId);
                                      }
                                  });
                          });
                    } else {
                        // User not found, continue with check-in without the user name
        attendanceRepository.recordCheckIn(attendance)
                .observeForever(result -> {
                    loadingLiveData.setValue(false);
                    if (Boolean.FALSE.equals(result)) {
                        errorLiveData.setValue("Failed to record check-in");
                    } else {
                        loadAttendanceHistory(userId);
                        loadMonthlySummary(userId);
                    }
                            });
                    }
                })
                .addOnFailureListener(e -> {
                    // Failed to get user data, continue with check-in without the user name
                    attendanceRepository.recordCheckIn(attendance)
                        .observeForever(result -> {
                            loadingLiveData.setValue(false);
                            if (Boolean.FALSE.equals(result)) {
                                errorLiveData.setValue("Failed to record check-in");
                             } else {
                                loadAttendanceHistory(userId);
                                loadMonthlySummary(userId);
                             }
                         });
                 });
           })
           .addOnFailureListener(e -> {
               // Failed to check for existing check-ins, proceed anyway
              loadingLiveData.setValue(false);
               errorLiveData.setValue("Error checking attendance records: " + e.getMessage());
                });
    }

    public void checkOut(String userId) {
        loadingLiveData.setValue(true);
        attendanceRepository.recordCheckOut(userId)
                .observeForever(result -> {
                    loadingLiveData.setValue(false);
                    if (Boolean.FALSE.equals(result)) {
                        errorLiveData.setValue("Failed to record check-out");
                    } else {
                        loadAttendanceHistory(userId);
                        loadMonthlySummary(userId);
                    }
                });
    }

    public void resetError() {
        errorLiveData.setValue(null);
    }

    /**
     * Updates the location status of a user's active attendance record
     * 
     * @param userId The user ID
     * @param date Today's date in yyyy-MM-dd format
     * @param locationStatus The location status: "InOffice", "OutOfOffice", or "Unknown"
     */
    public void updateLocationStatus(String userId, String date, String locationStatus) {
        loadingLiveData.setValue(true);
        
        Log.d("AttendanceViewModel", "Updating location status for user: " + userId + " to: " + locationStatus);
        
        attendanceRepository.updateLocationStatus(userId, date, locationStatus)
                .observeForever(new Observer<Boolean>() {
                    @Override
                    public void onChanged(Boolean success) {
                        loadingLiveData.setValue(false);
                        locationUpdateLiveData.setValue(success);
                        
                        if (!success) {
                            errorLiveData.setValue("Failed to update location status");
                        } else {
                            // Refresh live attendance data if this is successful
                            loadLiveAttendanceData(null);
                        }
                        
                        // Remove the observer
                        attendanceRepository.updateLocationStatus(userId, date, locationStatus).removeObserver(this);
                    }
                });
    }
    
    /**
     * Gets the active attendance record for a user
     * 
     * @param userId The user ID
     * @param date Today's date in yyyy-MM-dd format
     */
    public void getActiveAttendance(String userId, String date) {
        loadingLiveData.setValue(true);
        
        attendanceRepository.getActiveAttendance(userId, date)
                .observeForever(new Observer<Attendance>() {
                    @Override
                    public void onChanged(Attendance attendance) {
                        loadingLiveData.setValue(false);
                        activeAttendanceLiveData.setValue(attendance);
                        
                        // Remove the observer
                        attendanceRepository.getActiveAttendance(userId, date).removeObserver(this);
                    }
                });
    }
    
    /**
     * Get location update status LiveData
     */
    public LiveData<Boolean> getLocationUpdateLiveData() {
        return locationUpdateLiveData;
    }
    
    /**
     * Get active attendance LiveData
     */
    public LiveData<Attendance> getActiveAttendanceLiveData() {
        return activeAttendanceLiveData;
    }

    /**
     * Get all attendance records for a specific month
     * @param yearMonth The month in yyyy-MM format
     * @param officeId Optional office ID to filter by
     * @return LiveData containing list of attendance records
     */
    public LiveData<List<Attendance>> getAllAttendanceForMonth(String yearMonth, String officeId) {
        return attendanceRepository.getAllAttendanceForMonth(yearMonth, officeId);
    }

    /**
     * Get attendance history for a specific user and month
     * @param userId The user ID
     * @param yearMonth The month in yyyy-MM format
     * @return LiveData containing list of attendance records
     */
    public LiveData<List<Attendance>> getAttendanceHistoryForMonth(String userId, String yearMonth) {
        return attendanceRepository.getAttendanceHistoryForMonth(userId, yearMonth);
    }

    public LiveData<List<Attendance>> getRecentAttendance() {
        return recentAttendanceLiveData;
    }

    public void refreshAttendanceData() {
        attendanceRepository.getRecentAttendance(5) // Get last 5 attendance records
            .observeForever(attendances -> {
                recentAttendanceLiveData.setValue(attendances);
            });
    }

    public void checkIn() {
        attendanceRepository.checkIn()
            .observeForever(success -> {
                if (success) {
                    refreshAttendanceData();
                }
            });
    }

    public void checkOut() {
        attendanceRepository.checkOut()
            .observeForever(success -> {
                if (success) {
                    refreshAttendanceData();
                }
            });
    }
}