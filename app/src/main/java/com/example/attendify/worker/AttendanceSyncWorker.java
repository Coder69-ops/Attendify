package com.example.attendify.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Worker class for syncing offline attendance records with Firestore
 * This runs periodically to ensure all attendance records are eventually synchronized
 */
public class AttendanceSyncWorker extends Worker {
    private static final String TAG = "AttendanceSyncWorker";
    private static final String PREF_NAME = "attendance_offline_storage";
    private static final String KEY_PENDING_RECORDS = "pending_attendance_records";
    
    private final FirebaseFirestore db;
    private final SharedPreferences sharedPreferences;

    public AttendanceSyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = FirebaseFirestore.getInstance();
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting attendance sync work");
        
        try {
            // Get pending records from SharedPreferences
            String pendingRecordsJson = sharedPreferences.getString(KEY_PENDING_RECORDS, null);
            if (pendingRecordsJson == null || pendingRecordsJson.isEmpty()) {
                Log.d(TAG, "No pending records to sync");
                return Result.success();
            }
            
            // Parse the JSON to get the list of pending records
            Gson gson = new Gson();
            Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> pendingRecords = gson.fromJson(pendingRecordsJson, type);
            
            if (pendingRecords.isEmpty()) {
                Log.d(TAG, "No pending records to sync after parsing");
                return Result.success();
            }
            
            Log.d(TAG, "Found " + pendingRecords.size() + " pending records to sync");
            
            // Create a countdown latch to wait for all operations
            final CountDownLatch latch = new CountDownLatch(pendingRecords.size());
            final AtomicInteger successCount = new AtomicInteger(0);
            final List<Map<String, Object>> remainingRecords = new ArrayList<>();
            
            // Process each pending record
            for (Map<String, Object> record : pendingRecords) {
                String recordType = (String) record.get("type");
                String userId = (String) record.get("userId");
                String date = (String) record.get("date");
                
                if (recordType == null || userId == null || date == null) {
                    Log.e(TAG, "Invalid record format: " + record);
                    latch.countDown();
                    continue;
                }
                
                if ("check-in".equals(recordType)) {
                    processCheckIn(record, latch, successCount, remainingRecords);
                } else if ("check-out".equals(recordType)) {
                    processCheckOut(record, latch, successCount, remainingRecords);
                } else {
                    Log.e(TAG, "Unknown record type: " + recordType);
                    latch.countDown();
                }
            }
            
            // Wait for all operations to complete
            latch.await(60, TimeUnit.SECONDS);
            
            // Update the shared preferences with remaining records
            if (remainingRecords.isEmpty()) {
                // All records synced successfully, clear the pending records
                sharedPreferences.edit().remove(KEY_PENDING_RECORDS).apply();
                Log.d(TAG, "All records synced successfully");
            } else {
                // Some records failed to sync, save them for the next attempt
                String remainingJson = gson.toJson(remainingRecords);
                sharedPreferences.edit().putString(KEY_PENDING_RECORDS, remainingJson).apply();
                Log.d(TAG, remainingRecords.size() + " records remaining to sync");
            }
            
            // Return success if at least one record was synced, otherwise retry
            if (successCount.get() > 0) {
                return Result.success();
            } else {
                return Result.retry();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in attendance sync worker", e);
            return Result.failure();
        }
    }
    
    private void processCheckIn(Map<String, Object> record, CountDownLatch latch, 
                               AtomicInteger successCount, List<Map<String, Object>> remainingRecords) {
        String userId = (String) record.get("userId");
        String date = (String) record.get("date");
        
        // Create a data map for the attendance record
        Map<String, Object> attendanceData = new HashMap<>(record);
        attendanceData.remove("type"); // Remove the record type
        
        // Save to Firestore
        db.collection("attendance")
            .document(userId)
            .collection(date)
            .add(attendanceData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Check-in synced with ID: " + documentReference.getId());
                successCount.incrementAndGet();
                latch.countDown();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error syncing check-in", e);
                remainingRecords.add(record);
                latch.countDown();
            });
    }
    
    private void processCheckOut(Map<String, Object> record, CountDownLatch latch,
                                AtomicInteger successCount, List<Map<String, Object>> remainingRecords) {
        String userId = (String) record.get("userId");
        String date = (String) record.get("date");
        Object checkOutTime = record.get("checkOutTime");
        
        // Find the attendance document to update
        db.collection("attendance")
            .document(userId)
            .collection(date)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    // Update the first document with check-out time
                    queryDocumentSnapshots.getDocuments().get(0).getReference()
                        .update("checkOutTime", checkOutTime)
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Check-out synced successfully");
                            successCount.incrementAndGet();
                            latch.countDown();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error syncing check-out", e);
                            remainingRecords.add(record);
                            latch.countDown();
                        });
                } else {
                    Log.e(TAG, "No check-in record found for check-out");
                    remainingRecords.add(record);
                    latch.countDown();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error finding check-in record for check-out", e);
                remainingRecords.add(record);
                latch.countDown();
            });
    }
} 