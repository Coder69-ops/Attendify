package com.example.attendify.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Utility class for network operations and connectivity checks
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    /**
     * Check if the device has an active internet connection
     * @param context Application context
     * @return true if connected, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }
        
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) 
                    context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network availability: " + e.getMessage(), e);
        }
        
        return false;
    }
    
    /**
     * Check if Google Play Services is available and up-to-date
     * @param context Application context
     * @return true if available, false otherwise
     */
    public static boolean isGooglePlayServicesAvailable(Context context) {
        if (context == null) {
            return false;
        }
        
        try {
            GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
            return resultCode == ConnectionResult.SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "Error checking Google Play Services: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Configure Firestore for offline support to improve app reliability
     */
    public static void configureFirestoreOfflineSupport() {
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
            Log.d(TAG, "Firestore offline support configured successfully");
        } catch (Exception e) {
            Log.w(TAG, "Failed to configure Firestore offline support: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determine if the error message is related to network connectivity issues
     * @param errorMessage Error message to analyze
     * @return true if it's a network-related error
     */
    public static boolean isNetworkError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        
        String lowerCaseError = errorMessage.toLowerCase();
        return lowerCaseError.contains("network") || 
               lowerCaseError.contains("internet") ||
               lowerCaseError.contains("connection") ||
               lowerCaseError.contains("unavailable") ||
               lowerCaseError.contains("timeout") ||
               lowerCaseError.contains("unable to resolve host");
    }
} 