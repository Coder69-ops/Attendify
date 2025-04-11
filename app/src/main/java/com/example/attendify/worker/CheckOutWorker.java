package com.example.attendify.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker class for handling check-out operations when the device is offline
 * This ensures check-outs are eventually recorded when connectivity is restored
 */
public class CheckOutWorker extends Worker {
    private static final String TAG = "CheckOutWorker";
    private final FirebaseFirestore db;

    public CheckOutWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting check-out work task");
        
        try {
            // Extract data from input
            String userId = getInputData().getString("userId");
            String date = getInputData().getString("date");
            long checkOutTimeMs = getInputData().getLong("checkOutTime", System.currentTimeMillis());
            Date checkOutTime = new Date(checkOutTimeMs);
            
            if (userId == null || date == null) {
                Log.e(TAG, "Missing required fields for check-out");
                return Result.failure();
            }
            
            // Use a latch to wait for the Firestore operation
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicBoolean success = new AtomicBoolean(false);
            
            // First, get today's attendance document
            db.collection("attendance")
                .document(userId)
                .collection(date)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Found the attendance record, update it with check-out time
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        document.getReference()
                                .update("checkOutTime", checkOutTime)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Check-out recorded successfully");
                                    success.set(true);
                                    latch.countDown();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error recording check-out", e);
                                    latch.countDown();
                                });
                    } else {
                        // No check-in record found
                        Log.e(TAG, "No check-in record found for check-out");
                        latch.countDown();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding check-in record", e);
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
            Log.e(TAG, "Error in check-out worker", e);
            return Result.failure();
        }
    }
} 