package com.example.attendify.service;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.attendify.model.Office;
import com.example.attendify.ui.admin.AdminDashboardActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.ArrayList;
import java.util.List;

public class GeofencingService {

    private static final String TAG = "GeofencingService";
    private static final long UPDATE_INTERVAL = 10 * 1000;  // 10 seconds
    private static final long FASTEST_UPDATE_INTERVAL = 5 * 1000; // 5 seconds
    private static final float GEOFENCE_RADIUS_IN_METERS = 100;

    private final Context context;
    private final GeofencingClient geofencingClient;
    private final FusedLocationProviderClient fusedLocationClient;
    private final LocationCallback locationCallback;
    private final LocationRequest locationRequest;

    private final MutableLiveData<Boolean> isInsideGeofenceLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Float> distanceToOfficeLiveData = new MutableLiveData<>();
    private final MutableLiveData<Location> currentLocationLiveData = new MutableLiveData<>();
    
    private Office currentOffice;
    private List<Office> availableOffices = new ArrayList<>();

    public GeofencingService(Context context) {
        this.context = context;
        geofencingClient = LocationServices.getGeofencingClient(context);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        // Create location request for continuous updates
        locationRequest = new LocationRequest.Builder(UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        // Define location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update current location
                    currentLocationLiveData.setValue(location);
                    
                    // Check if inside office geofence
                    if (currentOffice != null) {
                        checkIfInsideGeofence(location, currentOffice);
                    }
                }
            }
        };
    }

    public void setupGeofence(Office office) {
        if (office == null) return;
        
        this.currentOffice = office;
        
        // Check for permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Create geofence
        Geofence geofence = new Geofence.Builder()
                .setRequestId(office.getId())
                .setCircularRegion(
                        office.getLatitude(),
                        office.getLongitude(),
                        GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        // Create geofencing request
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        // Add geofence
        geofencingClient.addGeofences(geofencingRequest, null)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Geofence added successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to add geofence", e));

        // Get current location to check if already inside
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLocationLiveData.setValue(location);
                        checkIfInsideGeofence(location, office);
                    }
                });
    }
    
    public void updateAvailableOffices(List<Office> offices) {
        if (offices == null) {
            this.availableOffices = new ArrayList<>();
            Log.d(TAG, "Updated with null offices");
            return;
        }
        this.availableOffices = new ArrayList<>(offices);
        Log.d(TAG, "Updated with " + offices.size() + " offices");
        for (Office office : offices) {
            Log.d(TAG, "Available office: " + office.getName() + " (ID: " + office.getId() + ")");
        }
    }
    
    public void switchActiveOffice(Office newOffice) {
        if (newOffice == null) return;
        
        // Use the new office as current
        this.currentOffice = newOffice;
        
        // Update geofence
        setupGeofence(newOffice);
        
        // Check if inside with current location
        Location currentLocation = currentLocationLiveData.getValue();
        if (currentLocation != null) {
            checkIfInsideGeofence(currentLocation, newOffice);
        }
    }

    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    public void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
    
    public void refreshLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        // Get current location immediately
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener(location -> {
                if (location != null) {
                    currentLocationLiveData.setValue(location);
                    if (currentOffice != null) {
                        checkIfInsideGeofence(location, currentOffice);
                    }
                }
            });
    }

    private void checkIfInsideGeofence(Location location, Office office) {
        if (location == null || office == null) return;
        
        // Calculate distance to office
        float[] distance = new float[1];
        Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                office.getLatitude(), office.getLongitude(),
                distance
        );
        
        // Update distance LiveData
        distanceToOfficeLiveData.setValue(distance[0]);
        
        // Check if inside geofence radius
        boolean isInside = distance[0] <= GEOFENCE_RADIUS_IN_METERS;
        boolean wasInside = isInsideGeofenceLiveData.getValue() != null && isInsideGeofenceLiveData.getValue();
        
        // Only broadcast geofence transitions when the status changes
        if (isInside != wasInside) {
            broadcastGeofenceTransition(isInside);
        }
        
        // Update the LiveData value
        isInsideGeofenceLiveData.setValue(isInside);
    }
    
    /**
     * Broadcasts when an employee enters or leaves a geofenced area
     * This will notify any registered receivers (like AdminDashboardActivity)
     */
    private void broadcastGeofenceTransition(boolean isEntering) {
        Intent intent = new Intent(AdminDashboardActivity.ACTION_GEOFENCE_TRANSITION);
        // Add transition type and location information
        intent.putExtra("is_entering", isEntering);
        if (currentOffice != null) {
            intent.putExtra("office_id", currentOffice.getId());
            intent.putExtra("office_name", currentOffice.getName());
        }
        
        // Send the broadcast using LocalBroadcastManager
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        
        // Log the transition
        Log.d(TAG, "Geofence transition broadcast: " + (isEntering ? "ENTER" : "EXIT") + 
              " for office: " + (currentOffice != null ? currentOffice.getName() : "unknown"));
    }
    
    public float calculateDistanceToOffice(Office office, Location location) {
        if (office == null || location == null) return -1;
        
        float[] distance = new float[1];
        Location.distanceBetween(
                location.getLatitude(), location.getLongitude(),
                office.getLatitude(), office.getLongitude(),
                distance
        );
        return distance[0];
    }
    
    public List<Office> getAvailableOffices() {
        return availableOffices;
    }
    
    public Office getCurrentOffice() {
        return currentOffice;
    }

    public LiveData<Boolean> getIsInsideGeofenceLiveData() {
        return isInsideGeofenceLiveData;
    }

    public LiveData<Float> getDistanceToOfficeLiveData() {
        return distanceToOfficeLiveData;
    }
    
    public LiveData<Location> getCurrentLocationLiveData() {
        return currentLocationLiveData;
    }

    /**
     * Validates if the user is currently within the specified office geofence
     * Used as a security measure to prevent check-ins outside the office area
     * 
     * @param office The office to check against
     * @return true if user is inside office geofence, false otherwise
     */
    public boolean validateCheckInLocation(Office office) {
        Location currentLocation = getCurrentLocationLiveData().getValue();
        if (currentLocation == null) return false;
        
        float distance = calculateDistanceToOffice(office, currentLocation);
        return distance <= office.getRadius();
    }
}