package com.example.attendify.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.attendify.model.Attendance;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker class for handling check-in operations when the device is offline
 * This ensures check-ins are eventually recorded when connectivity is restored
 */
public class CheckInWorker extends Worker {
    private static final String TAG = "CheckInWorker";
    private final FirebaseFirestore db;

    public CheckInWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting check-in work task");
        
        try {
            // Extract attendance data from input
            String userId = getInputData().getString("userId");
            String date = getInputData().getString("date");
            String status = getInputData().getString("status");
            String officeId = getInputData().getString("officeId");
            String userName = getInputData().getString("userName");
            String officeName = getInputData().getString("officeName");
            long checkInTimeMs = getInputData().getLong("checkInTime", System.currentTimeMillis());
            Date checkInTime = new Date(checkInTimeMs);
            
            if (userId == null || date == null) {
                Log.e(TAG, "Missing required fields for check-in");
                return Result.failure();
            }
            
            // Create attendance data map
            Map<String, Object> attendanceData = new HashMap<>();
            attendanceData.put("userId", userId);
            attendanceData.put("date", date);
            attendanceData.put("checkInTime", checkInTime);
            attendanceData.put("status", status);
            attendanceData.put("officeId", officeId);
            
            if (userName != null) {
                attendanceData.put("userName", userName);
            }
            
            if (officeName != null) {
                attendanceData.put("officeName", officeName);
            }
            
            // Use a latch to wait for the Firestore operation
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean success = new AtomicBoolean(false);
            
            // Save to Firestore
            db.collection("attendance")
                .document(userId)
                .collection(date)
                .add(attendanceData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Check-in recorded with ID: " + documentReference.getId());
                    success.set(true);
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error recording check-in", e);
                    latch.countDown();
                });
            
            // Wait for the operation to complete (with timeout)
            latch.await(30, TimeUnit.SECONDS);
            
            if (success.get()) {
                return Result.success();
            } else {
                return Result.retry();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in check-in worker", e);
            return Result.failure();
        }
    }
} 