package com.example.attendify.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.attendify.databinding.ActivityAuthBinding;
import com.example.attendify.model.User;
import com.example.attendify.ui.admin.AdminDashboardActivity;
import com.example.attendify.ui.employee.EmployeeDashboardActivity;
import com.example.attendify.utils.PermissionManager;
import com.example.attendify.viewmodel.AuthViewModel;
import com.example.attendify.viewmodel.AuthViewModel.NavigationTarget;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding binding;
    private AuthViewModel viewModel;
    private AuthPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Set up ViewPager
        setupViewPager();

        // Set up observers
        setupObservers();
        
        // Check and request any missing permissions
        checkAndRequestPermissions();
    }
    
    private void checkAndRequestPermissions() {
        // Only check for permissions if we don't have all of them already
        if (!PermissionManager.hasAllRequiredPermissions(this)) {
            // First check if basic location permissions are needed
            if (!PermissionManager.hasBasicLocationPermissions(this)) {
                PermissionManager.showLocationPermissionRationale(this);
            } 
            // Then check if background location permission is needed
            else if (!PermissionManager.hasBackgroundLocationPermission(this)) {
                PermissionManager.showBackgroundLocationRationale(this);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // Let the permission manager handle the result
        PermissionManager.handlePermissionResult(this, requestCode, permissions, grantResults);
    }

    private void setupViewPager() {
        pagerAdapter = new AuthPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(position == 0 ? "Login" : "Register")
        ).attach();
        
        // Disable swipe to change pages if needed
        // binding.viewPager.setUserInputEnabled(false);
    }

    private void setupObservers() {
        // Observe navigation events
        viewModel.getNavigationLiveData().observe(this, event -> {
            if (event != null) {
                handleNavigation(event.getTarget());
            }
        });
    }

    private void handleNavigation(NavigationTarget target) {
        Intent intent;
        switch (target) {
            case ADMIN_DASHBOARD:
                intent = new Intent(this, AdminDashboardActivity.class);
                startActivity(intent);
                finish();
                break;
            case EMPLOYEE_DASHBOARD:
                intent = new Intent(this, EmployeeDashboardActivity.class);
                startActivity(intent);
                finish();
                break;
            case PENDING_APPROVAL:
                intent = new Intent(this, PendingApprovalActivity.class);
                startActivity(intent);
                finish();
                break;
            default:
                // Already on auth screen, do nothing
                break;
        }
    }
    
    public void navigateBasedOnUserStatus(User user) {
        if (user == null) {
            return;
        }
        
        if (!user.isApproved()) {
            // Any unapproved user goes to pending approval
            handleNavigation(NavigationTarget.PENDING_APPROVAL);
            return;
        }
        
        // Only approved users reach this point
        if (user.isAdmin()) {
            // Approved admins go to admin dashboard
            handleNavigation(NavigationTarget.ADMIN_DASHBOARD);
        } else {
            // Approved non-admin users go to employee dashboard
            handleNavigation(NavigationTarget.EMPLOYEE_DASHBOARD);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}