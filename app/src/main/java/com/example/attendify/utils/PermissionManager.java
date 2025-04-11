package com.example.attendify.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.attendify.R;
import com.example.attendify.ui.common.PermissionDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    public static final int PERMISSION_REQUEST_CODE = 123;
    
    // List of permissions the app needs
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
    };
    
    // List of permissions that require Android Q+ for background location
    private static final String[] BACKGROUND_LOCATION_PERMISSION = {
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
    };
    
    // Permission descriptions for dialog messages
    private static final String LOCATION_PERMISSION_RATIONALE = 
            "Location permission is required to detect when you arrive at the office. " +
            "This helps in automatic attendance marking.";
    
    private static final String BACKGROUND_LOCATION_RATIONALE = 
            "Background location permission allows the app to detect your presence even when the app is not active. " +
            "This is essential for accurate attendance tracking.";
    
    /**
     * Check if all required permissions are granted
     */
    public static boolean hasAllRequiredPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        // For Android Q+, check background location permission separately
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        
        return true;
    }
    
    /**
     * Check if basic location permissions are granted
     */
    public static boolean hasBasicLocationPermissions(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if background location permission is granted (Android Q+)
     */
    public static boolean hasBackgroundLocationPermission(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // If not Android Q+, no background permission needed
    }
    
    /**
     * Request all permissions needed by the app
     */
    public static void requestAllPermissions(Activity activity) {
        List<String> permissionsToRequest = new ArrayList<>();
        
        // Check which permissions need to be requested
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        
        // Request permissions if any are missing
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity, 
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }
    
    /**
     * Request background location permission separately (for Android Q+)
     */
    public static void requestBackgroundLocationPermission(Activity activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity, 
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                
                ActivityCompat.requestPermissions(activity,
                        BACKGROUND_LOCATION_PERMISSION,
                        PERMISSION_REQUEST_CODE + 1);
            }
        }
    }
    
    /**
     * Show rationale dialogs for permissions using custom dialog fragment
     * @return true if dialog was shown, false if permissions already granted
     */
    public static boolean showLocationPermissionRationale(FragmentActivity activity) {
        // Don't show dialog if permissions already granted
        if (hasBasicLocationPermissions(activity)) {
            return false;
        }
        
        PermissionDialogFragment dialog = PermissionDialogFragment.newInstance(
                "Location Permission Required",
                LOCATION_PERMISSION_RATIONALE,
                R.drawable.ic_location
        );
        
        dialog.setPermissionDialogListener(new PermissionDialogFragment.PermissionDialogListener() {
            @Override
            public void onPermissionGranted() {
                requestAllPermissions(activity);
            }
            
            @Override
            public void onPermissionDenied() {
                // Do nothing, user denied permission
            }
        });
        
        dialog.show(activity.getSupportFragmentManager(), "permission_dialog");
        return true;
    }
    
    /**
     * Show background location rationale dialog using custom dialog fragment
     * @return true if dialog was shown, false if permission already granted
     */
    public static boolean showBackgroundLocationRationale(FragmentActivity activity) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Don't show dialog if background permission already granted
            if (hasBackgroundLocationPermission(activity)) {
                return false;
            }
            
            PermissionDialogFragment dialog = PermissionDialogFragment.newInstance(
                    "Background Location Permission",
                    BACKGROUND_LOCATION_RATIONALE,
                    R.drawable.ic_location_background
            );
            
            dialog.setPermissionDialogListener(new PermissionDialogFragment.PermissionDialogListener() {
                @Override
                public void onPermissionGranted() {
                    requestBackgroundLocationPermission(activity);
                }
                
                @Override
                public void onPermissionDenied() {
                    // Do nothing, user denied permission
                }
            });
            
            dialog.show(activity.getSupportFragmentManager(), "background_permission_dialog");
            return true;
        }
        return false;
    }
    
    /**
     * Show dialog directing user to app settings when they have permanently denied permissions
     */
    public static void showAppSettingsDialog(final FragmentActivity activity) {
        PermissionDialogFragment dialog = PermissionDialogFragment.newInstance(
                "Permissions Required",
                "Some required permissions have been permanently denied. " +
                "Please enable them in the app settings.",
                R.drawable.ic_settings
        );
        
        dialog.setPermissionDialogListener(new PermissionDialogFragment.PermissionDialogListener() {
            @Override
            public void onPermissionGranted() {
                // Open app settings
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            }
            
            @Override
            public void onPermissionDenied() {
                // Do nothing, user denied going to settings
            }
        });
        
        dialog.show(activity.getSupportFragmentManager(), "settings_dialog");
    }
    
    /**
     * Handle permission request results
     */
    public static boolean handlePermissionResult(FragmentActivity activity, int requestCode, @NonNull String[] permissions,
                                              @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE || requestCode == PERMISSION_REQUEST_CODE + 1) {
            boolean allGranted = true;
            
            // Check if all requested permissions were granted
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    
                    // Check if user clicked "Never ask again"
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions[i])) {
                        // Permission permanently denied, show settings dialog
                        showAppSettingsDialog(activity);
                        return false;
                    }
                }
            }
            
            // If regular permissions granted but background location needed
            if (allGranted && requestCode == PERMISSION_REQUEST_CODE && 
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Only show background dialog if that permission isn't already granted
                if (!hasBackgroundLocationPermission(activity)) {
                    showBackgroundLocationRationale(activity);
                }
            }
            
            return allGranted;
        }
        
        return false;
    }
} 