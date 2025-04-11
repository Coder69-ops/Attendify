package com.example.attendify.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.attendify.model.Attendance;
import com.example.attendify.model.Office;
import com.example.attendify.repository.AttendanceRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Service that tracks user location and updates their status in Firestore.
 * This service runs in the background and periodically updates the user's
 * location status based on their proximity to their assigned office.
 */
public class UserLocationService extends Service {
    private static final String TAG = "UserLocationService";
    
    // Location request parameters
    private static final long UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutes
    private static final long FASTEST_INTERVAL = 2 * 60 * 1000; // 2 minutes
    private static final float DEFAULT_GEOFENCE_RADIUS = 100; // 100 meters

    // Dependencies
    private FusedLocationProviderClient fusedLocationClient;
    private AttendanceRepository attendanceRepository;
    private FirebaseAuth firebaseAuth;
    
    // Tracking state
    private boolean isTracking = false;
    private LocationCallback locationCallback;
    private Office userOffice;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating UserLocationService");
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        attendanceRepository = AttendanceRepository.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        
        setupLocationCallback();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("office")) {
            userOffice = intent.getParcelableExtra("office");
            if (userOffice != null) {
                Log.d(TAG, "Starting location tracking for office: " + userOffice.getName());
                startLocationTracking();
            } else {
                Log.e(TAG, "Office data is missing in the intent");
            }
        } else if (intent != null && "STOP".equals(intent.getAction())) {
            stopLocationTracking();
            stopSelf();
        }
        
        // Restart service if killed
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    @Override
    public void onDestroy() {
        stopLocationTracking();
        super.onDestroy();
    }
    
    /**
     * Setup location callback for processing location updates
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    updateUserLocationStatus(location);
                }
            }
        };
    }
    
    /**
     * Start tracking the user's location
     */
    private void startLocationTracking() {
        if (isTracking) {
            Log.d(TAG, "Location tracking already started");
            return;
        }
        
        LocationRequest locationRequest = new LocationRequest.Builder(UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build();
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }
        
        fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper());
        
        isTracking = true;
        Log.d(TAG, "Location tracking started");
        
        // Force an immediate location update
        requestImmediate();
    }
    
    /**
     * Stop tracking the user's location
     */
    private void stopLocationTracking() {
        if (!isTracking) {
            return;
        }
        
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        Log.d(TAG, "Location tracking stopped");
    }
    
    /**
     * Request an immediate location update
     */
    public void requestImmediate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }
        
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        updateUserLocationStatus(location);
                    } else {
                        Log.e(TAG, "Failed to get immediate location");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting location", e));
    }
    
    /**
     * Update the user's location status in Firestore
     */
    private void updateUserLocationStatus(Location location) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null || userOffice == null) {
            Log.e(TAG, "User not logged in or office data missing");
            return;
        }
        
        String userId = currentUser.getUid();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Check if user is within the office geofence
        boolean isInOffice = isUserInOffice(location);
        String locationStatus = isInOffice ? "InOffice" : "OutOfOffice";
        
        Log.d(TAG, "Updating location status: " + locationStatus + " for user: " + userId);
        
        // Update the user's location status in Firestore
        attendanceRepository.updateLocationStatus(userId, today, locationStatus);
    }
    
    /**
     * Check if the user is within their office geofence
     */
    private boolean isUserInOffice(Location location) {
        if (userOffice == null || userOffice.getLatitude() == 0 || userOffice.getLongitude() == 0) {
            Log.e(TAG, "Invalid office location data");
            return false;
        }
        
        // Get the radius from the office (or use default)
        float radius = (float) (userOffice.getRadius() > 0 ? userOffice.getRadius() : DEFAULT_GEOFENCE_RADIUS);
        
        // Calculate distance between current location and office
        float[] results = new float[1];
        Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                userOffice.getLatitude(), userOffice.getLongitude(),
                results);
        
        float distance = results[0];
        Log.d(TAG, "Distance from office: " + distance + " meters, Office radius: " + radius + " meters");
        
        // User is in office if distance is less than the radius
        return distance <= radius;
    }
    
    /**
     * Check if the service is actively tracking location
     */
    public boolean isTracking() {
        return isTracking;
    }
} 