package com.example.attendify.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.attendify.R;
import com.example.attendify.ui.auth.AuthActivity;
import com.example.attendify.utils.PermissionManager;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1500; // 1.5 seconds
    private boolean permissionsRequested = false;
    private boolean readyToProceed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        // Hide system UI for a more immersive splash screen
        hideSystemUI();
        
        // Start a delayed timer for minimum splash display
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Mark that we're ready to proceed after minimum display time
            readyToProceed = true;
            
            // If permissions were already handled or not requested, we can proceed now
            if (!permissionsRequested) {
                navigateToAuthActivity();
            }
        }, SPLASH_DELAY);
        
        // Check if all permissions are already granted
        if (PermissionManager.hasAllRequiredPermissions(this)) {
            // Permissions already granted, will proceed after splash delay
        } else {
            // First, check if basic location permissions are granted
            if (!PermissionManager.hasBasicLocationPermissions(this)) {
                permissionsRequested = PermissionManager.showLocationPermissionRationale(this);
            } 
            // Then check if background location permission is needed
            else if (!PermissionManager.hasBackgroundLocationPermission(this)) {
                permissionsRequested = PermissionManager.showBackgroundLocationRationale(this);
            }
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void navigateToAuthActivity() {
        if (readyToProceed) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // Let the permission manager handle the result
        boolean result = PermissionManager.handlePermissionResult(this, requestCode, permissions, grantResults);
        
        // Now that we've handled the permission request, navigate forward
        navigateToAuthActivity();
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }
} 