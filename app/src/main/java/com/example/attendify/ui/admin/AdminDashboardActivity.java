package com.example.attendify.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.core.view.WindowCompat;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.example.attendify.R;
import com.example.attendify.databinding.ActivityAdminDashboardBinding;
import com.example.attendify.fragments.admin.EmployeeManagementFragment;
import com.example.attendify.ui.auth.AuthActivity;
import com.example.attendify.viewmodel.AuthViewModel;
import com.example.attendify.viewmodel.OfficeViewModel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

public class AdminDashboardActivity extends AppCompatActivity {
    private static final String TAG = "AdminDashboardActivity";
    private ActivityAdminDashboardBinding binding;
    private AuthViewModel authViewModel;
    private OfficeViewModel officeViewModel;
    private AdminPagerAdapter pagerAdapter;

    // Define action for geofence transition broadcasts
    public static final String ACTION_GEOFENCE_TRANSITION = "com.example.attendify.ACTION_GEOFENCE_TRANSITION";
    
    // BroadcastReceiver to handle geofence transitions
    private BroadcastReceiver geofenceTransitionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Enable edge-to-edge display
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }

            // Initialize Firestore for offline capability
            com.example.attendify.utils.NetworkUtils.configureFirestoreOfflineSupport();
            
            // Check for network connectivity
            if (!com.example.attendify.utils.NetworkUtils.isNetworkAvailable(this)) {
                Toast.makeText(this, "Network unavailable. Some features may not work properly.", 
                        Toast.LENGTH_LONG).show();
                Log.w(TAG, "Network unavailable on startup. App will use cached data if available.");
            }
            
            // Check for Google Play Services
            if (!com.example.attendify.utils.NetworkUtils.isGooglePlayServicesAvailable(this)) {
                Toast.makeText(this, "Google Play Services unavailable. Some features may not work properly.",
                        Toast.LENGTH_LONG).show();
                Log.w(TAG, "Google Play Services unavailable");
            }

            // Initialize ViewModels
            try {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
                officeViewModel = new ViewModelProvider(this).get(OfficeViewModel.class);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing ViewModels: " + e.getMessage());
                Toast.makeText(this, "Error initializing application. Please try again.", 
                        Toast.LENGTH_SHORT).show();
            }

        // Set up ViewPager and TabLayout
        setupViewPager();
            
            // Set up Bottom Navigation
            setupBottomNavigation();

        // Set up observers
        setupObservers();

            // Register for geofence transition broadcasts
            registerGeofenceTransitionReceiver();
        } catch (Exception e) {
            Log.e(TAG, "Fatal error during AdminDashboardActivity initialization: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading dashboard. Please restart the app.", 
                    Toast.LENGTH_LONG).show();
            // Fall back to auth activity if startup fails
            startActivity(new Intent(this, AuthActivity.class));
            finish();
        }
    }

    private void setupViewPager() {
        try {
        pagerAdapter = new AdminPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
            
            // Add page change limit to prevent loading all fragments at once
            binding.viewPager.setOffscreenPageLimit(1);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> tab.setText(pagerAdapter.getPageTitle(position))
        ).attach();

        // Handle FAB visibility
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // Show FAB only on Offices tab
                    binding.addOfficeFab.setVisibility(position == 3 ? View.VISIBLE : View.GONE);
                    
                    // Log page change for debugging
                    Log.d(TAG, "ViewPager page selected: " + position + " - " + pagerAdapter.getPageTitle(position));
            }
        });

        // Set up FAB click listener
        binding.addOfficeFab.setOnClickListener(v -> {
                try {
                    // Open the add office dialog directly from the activity
                    AddOfficeDialogFragment.newInstance()
                        .show(getSupportFragmentManager(), "add_office");
                } catch (Exception e) {
                    Log.e(TAG, "Error showing add office dialog: " + e.getMessage());
                    Toast.makeText(this, "Cannot open dialog right now. Please try again.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up ViewPager: " + e.getMessage());
            Toast.makeText(this, "Error initializing dashboard views", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.navigation_dashboard) {
                // Show the dashboard tabs and view pager
                showDashboardContent(true);
                return true;
            }
            else if (itemId == R.id.navigation_employees) {
                // Launch employee management
                showDashboardContent(false);
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new EmployeeManagementFragment())
                    .commit();
                return true;
            }
            else if (itemId == R.id.navigation_departments) {
                // Launch department management
                showDashboardContent(false);
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new DepartmentManagementFragment())
                    .commit();
                return true;
            }
            else if (itemId == R.id.navigation_offices) {
                // Show the offices tab in the viewpager
                showDashboardContent(true);
                binding.viewPager.setCurrentItem(3, true); // Offices tab
                return true;
            }
            else if (itemId == R.id.navigation_settings) {
                // Launch settings
                showDashboardContent(false);
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new AdminSettingsFragment())
                    .commit();
                return true;
            }
            
            return false;
        });
        
        // Set default selection
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_dashboard);
    }
    
    private void showDashboardContent(boolean showDashboard) {
        if (showDashboard) {
            binding.tabLayout.setVisibility(View.VISIBLE);
            binding.viewPager.setVisibility(View.VISIBLE);
            
            // Hide fragment container if it exists
            View fragmentContainer = findViewById(R.id.fragmentContainer);
            if (fragmentContainer != null) {
                fragmentContainer.setVisibility(View.GONE);
            }
        } else {
            binding.tabLayout.setVisibility(View.GONE);
            binding.viewPager.setVisibility(View.GONE);
            
            // Ensure fragment container exists
            View fragmentContainer = findViewById(R.id.fragmentContainer);
            if (fragmentContainer == null) {
                // Add fragment container dynamically if it doesn't exist in layout
                addFragmentContainer();
            } else {
                fragmentContainer.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void addFragmentContainer() {
        // This is a fallback if the fragment container isn't in the layout
        // In a real implementation, you would include it in the layout XML
        View root = binding.getRoot();
        if (root instanceof androidx.coordinatorlayout.widget.CoordinatorLayout) {
            androidx.fragment.app.FragmentContainerView container = new androidx.fragment.app.FragmentContainerView(this);
            container.setId(R.id.fragmentContainer);
            
            androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params = 
                new androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams(
                    androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams.MATCH_PARENT
                );
            params.setBehavior(new androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior<View>() {
                @Override
                public boolean layoutDependsOn(@NonNull androidx.coordinatorlayout.widget.CoordinatorLayout parent, 
                                              @NonNull View child, 
                                              @NonNull View dependency) {
                    return dependency instanceof com.google.android.material.appbar.AppBarLayout;
                }

                @Override
                public boolean onDependentViewChanged(@NonNull androidx.coordinatorlayout.widget.CoordinatorLayout parent, 
                                                     @NonNull View child, 
                                                     @NonNull View dependency) {
                    if (dependency instanceof com.google.android.material.appbar.AppBarLayout) {
                        child.setY(dependency.getBottom());
                        return true;
                    }
                    return false;
                }
            });
            params.setMargins(0, 0, 0, (int) getResources().getDimension(R.dimen.bottom_nav_height));
            
            ((androidx.coordinatorlayout.widget.CoordinatorLayout) root).addView(container, params);
        }
    }

    private void setupObservers() {
        // Observe current user
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user == null) {
                // User logged out, return to auth screen
                startActivity(new Intent(this, AuthActivity.class));
                finish();
            }
        });
        
        // Observe office changes
        officeViewModel.getOfficesLiveData().observe(this, offices -> {
            // If we're on the office management tab, refresh the current fragment
            if (binding.viewPager.getCurrentItem() == 3) {
                Fragment currentFragment = getSupportFragmentManager()
                    .findFragmentByTag("f" + binding.viewPager.getCurrentItem());
                if (currentFragment instanceof OfficeManagementFragment) {
                    ((OfficeManagementFragment) currentFragment).refreshOffices();
                }
            }
        });
    }

    private void registerGeofenceTransitionReceiver() {
        geofenceTransitionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // When a geofence transition occurs, refresh the LiveAttendanceFragment if it's visible
                if (binding.viewPager.getCurrentItem() == 0) {
                    // Get the LiveAttendanceFragment and refresh its data
                    Fragment currentFragment = getSupportFragmentManager()
                            .findFragmentByTag("f" + binding.viewPager.getCurrentItem());
                    if (currentFragment instanceof LiveAttendanceFragment) {
                        ((LiveAttendanceFragment) currentFragment).refreshAttendanceData();
                    }
                }
            }
        };
        
        // Register the receiver with LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).registerReceiver(
                geofenceTransitionReceiver,
                new IntentFilter(ACTION_GEOFENCE_TRANSITION)
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_admin_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            authViewModel.logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        // Unregister the receiver
        if (geofenceTransitionReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(geofenceTransitionReceiver);
        }
        
        super.onDestroy();
        binding = null;
    }
}