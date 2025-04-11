package com.example.attendify.ui.employee;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.core.view.WindowCompat;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;

import com.example.attendify.R;
import com.example.attendify.databinding.ActivityEmployeeDashboardBinding;
import com.example.attendify.model.Office;
import com.example.attendify.model.User;
import com.example.attendify.repository.AttendanceRepository.AttendanceSummary;
import com.example.attendify.service.GeofencingService;
import com.example.attendify.ui.auth.AuthActivity;
import com.example.attendify.viewmodel.AttendanceViewModel;
import com.example.attendify.viewmodel.AuthViewModel;
import com.example.attendify.viewmodel.OfficeViewModel;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EmployeeDashboardActivity extends AppCompatActivity implements OfficeSelectionDialogFragment.OnOfficeSelectedListener {

    private ActivityEmployeeDashboardBinding binding;
    private AuthViewModel authViewModel;
    private AttendanceViewModel attendanceViewModel;
    private OfficeViewModel officeViewModel;
    private GeofencingService geofencingService;
    private AttendanceAdapter attendanceAdapter;
    private User currentUser;
    private boolean isCheckedIn = false;
    private Office selectedOffice;
    private List<Office> availableOffices = new ArrayList<>();
    private static final String TAG = "EmployeeDashboard";
    private static final String PREFS_NAME = "AttendifyPrefs";
    private static final String SELECTED_OFFICE_KEY = "selectedOfficeId";

    // Permission launcher
    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean backgroundLocationGranted = result.getOrDefault(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION, false);

                if (fineLocationGranted != null && fineLocationGranted) {
                    if (backgroundLocationGranted != null && backgroundLocationGranted) {
                        startLocationTracking();
                    } else {
                        // Request background location permission
                        requestBackgroundLocationPermission();
                    }
                } else {
                    Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityEmployeeDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Employee Dashboard");
        }

        // Initialize ViewModels
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        attendanceViewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);
        officeViewModel = new ViewModelProvider(this).get(OfficeViewModel.class);

        // Initialize GeofencingService
        geofencingService = new GeofencingService(this);

        // Set up RecyclerView
        setupRecyclerView();
        
        // Set up current date
        updateCurrentDate();

        // Set up observers
        setupObservers();

        // Set up check-in button
        binding.checkInButton.setOnClickListener(v -> handleCheckInOut());
        
        // Set up click listeners for view all buttons
        binding.viewAllHistory.setOnClickListener(v -> openHistoryScreen());
        binding.viewAllStats.setOnClickListener(v -> openStatsScreen());
        
        // Set up change office button
        binding.changeOfficeButton.setOnClickListener(v -> showOfficeSelectionDialog());
        
        // Setup refresh location button
        binding.refreshLocationButton.setOnClickListener(v -> refreshLocation());
        
        // Set up bottom navigation
        setupBottomNavigation();

        // Check GPS status before requesting permissions
        checkGpsStatus();

        // Request location permissions
        requestLocationPermissions();
    }

    private void updateCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        binding.dateText.setText(currentDate);
    }
    
    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_history) {
                openHistoryScreen();
                return true;
            } else if (itemId == R.id.nav_profile) {
                openProfileScreen();
                return true;
            } else if (itemId == R.id.nav_settings) {
                openSettingsScreen();
                return true;
            }
            return false;
        });
        
        // Set home as selected by default
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
    }
    
    private void openHistoryScreen() {
        Intent intent = new Intent(this, AttendanceHistoryActivity.class);
        startActivity(intent);
    }
    
    private void openProfileScreen() {
        // Intent for profile screen would be implemented here
        Toast.makeText(this, "Profile screen coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void openStatsScreen() {
        // Intent for detailed stats screen would be implemented here
        Toast.makeText(this, "Detailed statistics coming soon", Toast.LENGTH_SHORT).show();
    }
    
    private void openSettingsScreen() {
        // Intent for settings screen would be implemented here
        Toast.makeText(this, "Settings screen coming soon", Toast.LENGTH_SHORT).show();
    }

    private void setupRecyclerView() {
        attendanceAdapter = new AttendanceAdapter();
        binding.attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.attendanceRecyclerView.setAdapter(attendanceAdapter);
    }

    private void setupObservers() {
        // Observe current user
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user == null) {
                // User logged out, return to auth screen
                startActivity(new Intent(this, AuthActivity.class));
                finish();
                return;
            }
            currentUser = user;
            updateWelcomeMessage();
            loadUserData();
        });

        // Observe office data
        officeViewModel.getOfficesLiveData().observe(this, offices -> {
            availableOffices = offices;
            
            // Check if we have a saved office preference
            String savedOfficeId = loadSelectedOfficeFromPrefs();
            if (savedOfficeId != null && !savedOfficeId.isEmpty()) {
                // Find the office from the available offices
                for (Office office : offices) {
                    if (office.getId().equals(savedOfficeId)) {
                        selectedOffice = office;
                        binding.selectedOfficeValue.setText(office.getName());
                        Log.d(TAG, "Loaded saved office: " + office.getName());
                        // Update geofencing for the selected office
                        geofencingService.switchActiveOffice(office);
                        break;
                    }
                }
            }
            
            binding.changeOfficeButton.setEnabled(!offices.isEmpty());
            
            geofencingService.updateAvailableOffices(offices);
            
            if (currentUser != null) {
                // If no office was selected from saved preferences, try to use the user's default office
                if (selectedOffice == null) {
                Office userOffice = offices.stream()
                        .filter(office -> office.getId().equals(currentUser.getOfficeId()))
                        .findFirst()
                        .orElse(null);

                if (userOffice != null) {
                        selectedOffice = userOffice;
                        updateSelectedOfficeUI();
                    geofencingService.setupGeofence(userOffice);
                    }
                }
            }
        });

        // Observe geofence status
        geofencingService.getIsInsideGeofenceLiveData().observe(this, isInside -> {
            // Allow check-in only when inside geofence AND not checked in
            // Allow check-out regardless of location when checked in
            binding.checkInButton.setEnabled((isInside && !isCheckedIn) || isCheckedIn);
            binding.geofenceStatusText.setText(isInside ? "Inside office area" : "Outside office area");
            
            Log.d(TAG, "Geofence status updated - Inside: " + isInside + 
                  ", Checked in: " + isCheckedIn + 
                  ", Button enabled: " + ((isInside && !isCheckedIn) || isCheckedIn));
        });

        // Observe distance to office
        geofencingService.getDistanceToOfficeLiveData().observe(this, distance -> {
            if (distance != null) {
                binding.distanceText.setText(String.format("%.0f meters", distance));
            }
        });

        // Observe attendance history
        attendanceViewModel.getAttendanceHistoryLiveData().observe(this, attendanceList -> {
            attendanceAdapter.updateAttendanceList(attendanceList);
            
            // Check if user is currently checked in (has a record for today without checkout time)
            boolean wasCheckedIn = isCheckedIn;
            isCheckedIn = attendanceList != null && !attendanceList.isEmpty() 
                    && attendanceList.get(0).getCheckOutTime() == null;
            
            Log.d("EmployeeDashboard", "Attendance LiveData updated: isCheckedIn changed from " 
                    + wasCheckedIn + " to " + isCheckedIn);
            
            updateCheckInStatus();
        });

        // Observe monthly summary
        attendanceViewModel.getMonthlySummaryLiveData().observe(this, this::updateDashboardStats);

        // Observe errors
        attendanceViewModel.getErrorLiveData().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                attendanceViewModel.resetError();
            }
        });
    }
    
    private void updateWelcomeMessage() {
        if (currentUser != null) {
            String firstName = currentUser.getName().split(" ")[0];
            binding.welcomeText.setText(String.format("Welcome, %s", firstName));
        }
    }

    private void loadUserData() {
        // Load attendance history
        attendanceViewModel.loadAttendanceHistory(currentUser.getUid());
        
        // Load monthly summary
        attendanceViewModel.loadMonthlySummary(currentUser.getUid());
        
        // Load office data
        officeViewModel.loadOffices();
        
        // Explicitly fetch check-in status to ensure UI is correct
        fetchCheckInStatus();
    }

    private void handleCheckInOut() {
        if (!isCheckedIn) {
            if (selectedOffice != null) {
                // Disable button to prevent double clicks
                binding.checkInButton.setEnabled(false);
                binding.checkInButton.setText("Processing...");
                
                // Check if network is available
                boolean isNetworkAvailable = isNetworkAvailable();
                
                // Get current time to compare with office entry time
                Calendar now = Calendar.getInstance();
                String entryTime = selectedOffice.getEntryTime(); // Format: "HH:mm"
                
                // Parse office entry time
                String[] entryTimeParts = entryTime.split(":");
                int entryHour = Integer.parseInt(entryTimeParts[0]);
                int entryMinute = Integer.parseInt(entryTimeParts[1]);
                
                // Create calendar for entry time today
                Calendar entryTimeToday = Calendar.getInstance();
                entryTimeToday.set(Calendar.HOUR_OF_DAY, entryHour);
                entryTimeToday.set(Calendar.MINUTE, entryMinute);
                entryTimeToday.set(Calendar.SECOND, 0);
                
                // Determine status: OnTime or Late
                String status = now.before(entryTimeToday) ? "OnTime" : "Late";
                
                Log.d("EmployeeDashboard", "Check-in status: " + status + 
                        ", Current time: " + now.getTime() + 
                        ", Entry time: " + entryTimeToday.getTime());
                
                // Check if within geofence as an extra security measure
                if (!geofencingService.validateCheckInLocation(selectedOffice)) {
                    binding.checkInButton.setEnabled(true);
                    binding.checkInButton.setText("Check In");
                    showErrorSnackbar("You must be inside the office area to check in");
                    return;
                }

                // Check GPS status
                if (!isGpsEnabled()) {
                    binding.checkInButton.setEnabled(true);
                    binding.checkInButton.setText("Check In");
                    showErrorSnackbar("GPS is required for accurate attendance tracking");
                    return;
                }
                
                // Show a confirmation dialog for check-in
                new MaterialAlertDialogBuilder(this)
                    .setTitle("Confirm Check-In")
                    .setMessage("You are about to check in to " + selectedOffice.getName() + 
                              "\nStatus: " + status)
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        // If offline, use our offline-capable method
                        if (!isNetworkAvailable) {
                            showStatusSnackbar("Network unavailable. Check-in will be synchronized when online.");
                        }
                        
                        Log.d("EmployeeDashboard", "Manual check-in initiated, setting isCheckedIn=true");
                        
                        // Use ViewModel checkInWithStatus method
                        attendanceViewModel.checkInWithStatus(currentUser.getUid(), 
                                              selectedOffice.getId(), status);
                        
                        // Manual status update - don't wait for LiveData
                        isCheckedIn = true;
                        updateCheckInStatus();
                        
                        // Show success animation
                        showCheckInSuccessAnimation();
                        
                        // Force refresh attendance history
                        attendanceViewModel.loadAttendanceHistory(currentUser.getUid());
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        // Re-enable check-in button if cancelled
                        binding.checkInButton.setEnabled(true);
                        binding.checkInButton.setText("Check In");
                    })
                    .setCancelable(false)
                    .show();
            } else {
                showErrorSnackbar("Please select an office first");
            }
        } else {
            // Disable button to prevent double clicks
            binding.checkInButton.setEnabled(false);
            binding.checkInButton.setText("Processing...");
            
            // Check if network is available
            boolean isNetworkAvailable = isNetworkAvailable();
            
            // Show a confirmation dialog for check-out
            new MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Check-Out")
                .setMessage("You are about to check out. Do you want to continue?")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    // If offline, use our offline-capable method
                    if (!isNetworkAvailable) {
                        showStatusSnackbar("Network unavailable. Check-out will be synchronized when online.");
                    }
                    
                    Log.d("EmployeeDashboard", "Manual check-out initiated, setting isCheckedIn=false");
                    
                    // Use ViewModel check-out method
            attendanceViewModel.checkOut(currentUser.getUid());
                    
                    // Manual status update - don't wait for LiveData
                    isCheckedIn = false;
                    updateCheckInStatus();
                    
                    // Show success animation
                    showCheckOutSuccessAnimation();
                    
                    // Force refresh attendance history
                    attendanceViewModel.loadAttendanceHistory(currentUser.getUid());
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Re-enable check-out button if cancelled
                    binding.checkInButton.setEnabled(true);
                    binding.checkInButton.setText("Check Out");
                })
                .setCancelable(false)
                .show();
        }
    }
    
    /**
     * Checks if the device has an active network connection
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    /**
     * Checks if GPS is enabled on the device
     */
    private boolean isGpsEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
    /**
     * Shows an error snackbar with red background
     */
    private void showErrorSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getColor(R.color.status_missed));
        snackbar.setTextColor(Color.WHITE);
        snackbar.show();
    }
    
    /**
     * Shows a status snackbar with blue background
     */
    private void showStatusSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getColor(R.color.status_late));
        snackbar.setTextColor(Color.WHITE);
        snackbar.show();
    }

    private void updateCheckInStatus() {
        Log.d("EmployeeDashboard", "Updating check-in status UI: isCheckedIn=" + isCheckedIn);
        
        binding.checkInButton.setText(isCheckedIn ? "Check Out" : "Check In");
        binding.checkStatusValue.setText(isCheckedIn ? "Checked In" : "Not Checked In");
        binding.checkStatusValue.setTextColor(getColor(isCheckedIn ? 
                R.color.status_on_time : R.color.status_missed));
        
        // Enable/disable button based on check-in status and location
        boolean isInsideOffice = Boolean.TRUE.equals(
                geofencingService.getIsInsideGeofenceLiveData().getValue());
        
        Log.d("EmployeeDashboard", "Inside office: " + isInsideOffice + 
                ", Button will be enabled: " + (isInsideOffice || isCheckedIn));
                
        binding.checkInButton.setEnabled(isInsideOffice || isCheckedIn);
        
        // Update button color based on check-in status
        binding.checkInButton.setBackgroundTintList(getColorStateList(
                isCheckedIn ? R.color.status_late : R.color.status_on_time));
        
        // Show network status
        updateNetworkStatusDisplay(isNetworkAvailable());
        
        // Ensure loading indicator is hidden
        binding.progressBar.setVisibility(View.GONE);
    }
    
    private void showCheckInConfirmation() {
        // Get current time formatted nicely
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());
        
        // Create and show snackbar
        Snackbar snackbar = Snackbar.make(binding.getRoot(), 
                "Successfully checked in at " + currentTime, 
                Snackbar.LENGTH_LONG);
        
        snackbar.setBackgroundTint(getColor(R.color.status_on_time));
        snackbar.setTextColor(Color.WHITE);
        snackbar.show();
        
        // Animate the status card
        binding.statusCard.setAlpha(0.7f);
        binding.statusCard.animate()
                .alpha(1.0f)
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(300)
                .withEndAction(() -> {
                    binding.statusCard.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200);
                });
    }
    
    private void showCheckOutConfirmation() {
        // Create and show snackbar for check-out
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        
        String currentTime = timeFormat.format(new Date());
        String currentDate = dateFormat.format(new Date());
        
        Snackbar snackbar = Snackbar.make(binding.getRoot(), 
                "Checked out at " + currentTime + "\nThank you for your work today!", 
                Snackbar.LENGTH_LONG);
        
        snackbar.setBackgroundTint(getColor(R.color.status_late));
        snackbar.setTextColor(Color.WHITE);
        snackbar.show();
        
        // Show a confirmation dialog with summary
        new MaterialAlertDialogBuilder(this)
                .setTitle("Check-out Successful")
                .setMessage("You have successfully checked out for " + currentDate + 
                          "\n\nSee you tomorrow!")
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    private void updateDashboardStats(AttendanceSummary summary) {
        // Update statistics cards
        binding.onTimeCount.setText(String.valueOf(summary.getOnTime()));
        binding.lateCount.setText(String.valueOf(summary.getLate()));
        binding.missedCount.setText(String.valueOf(summary.getMissed()));
        
        // Update pie chart
        updatePieChart(summary);
    }

    private void updatePieChart(AttendanceSummary summary) {
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(summary.getOnTime(), "On Time"));
        entries.add(new PieEntry(summary.getLate(), "Late"));
        entries.add(new PieEntry(summary.getMissed(), "Missed"));

        PieDataSet dataSet = new PieDataSet(entries, "Attendance");
        dataSet.setColors(
                getColor(R.color.status_on_time),
                getColor(R.color.status_late),
                getColor(R.color.status_missed)
        );
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        binding.attendancePieChart.setData(pieData);
        binding.attendancePieChart.getDescription().setEnabled(false);
        binding.attendancePieChart.animateY(1000);
        binding.attendancePieChart.invalidate();
    }

    private void requestLocationPermissions() {
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
        });
    }

    private void requestBackgroundLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            // Show rationale and then request
            Toast.makeText(this,
                    "Background location is needed for attendance tracking",
                    Toast.LENGTH_LONG).show();
        }
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
        });
    }

    private void startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingService.startLocationUpdates();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_employee_dashboard, menu);
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
        super.onDestroy();
        geofencingService.stopLocationUpdates();
        binding = null;
    }

    /**
     * Shows the office selection dialog if there are available offices
     */
    private void showOfficeSelectionDialog() {
        Log.d("EmployeeDashboard", "Showing office selection dialog with " + 
                (availableOffices != null ? availableOffices.size() : 0) + " offices");
        
        // Force load offices if not already loaded
        if (availableOffices == null || availableOffices.isEmpty()) {
            Toast.makeText(this, "Loading offices...", Toast.LENGTH_SHORT).show();
            officeViewModel.loadOffices();
            return;
        }
        
        // Create a copy of the list to prevent concurrent modification
        List<Office> officesToShow = new ArrayList<>(availableOffices);
        
        // Debug logging
        for (Office office : officesToShow) {
            Log.d("EmployeeDashboard", "Office to show in dialog: " + office.getName());
        }
        
        Location currentLocation = geofencingService.getCurrentLocationLiveData().getValue();
        
        // Create and show the dialog
        OfficeSelectionDialogFragment dialogFragment = OfficeSelectionDialogFragment.newInstance(
                officesToShow, selectedOffice, currentLocation);
        dialogFragment.show(getSupportFragmentManager(), "OfficeSelectionDialog");
    }
    
    /**
     * Refreshes the user's location and updates UI accordingly
     */
    private void refreshLocation() {
        Toast.makeText(this, "Refreshing location...", Toast.LENGTH_SHORT).show();
        geofencingService.refreshLocationUpdates();
    }
    
    /**
     * Updates the UI to show the selected office
     */
    private void updateSelectedOfficeUI() {
        if (selectedOffice != null) {
            Log.d("EmployeeDashboard", "Updating UI for selected office: " + selectedOffice.getName());
            binding.selectedOfficeValue.setText(selectedOffice.getName());
            binding.selectedOfficeValue.setTextColor(getColor(R.color.status_on_time)); // Green color for selected office
            binding.checkInButton.setEnabled(true);
        } else {
            binding.selectedOfficeValue.setText("No office selected");
            binding.selectedOfficeValue.setTextColor(getColor(R.color.status_missed)); // Red color for no selection
            binding.checkInButton.setEnabled(false);
        }
    }
    
    /**
     * Called when an office is selected from the dialog
     */
    @Override
    public void onOfficeSelected(Office office) {
        selectedOffice = office;
        binding.selectedOfficeValue.setText(office.getName());
        
        // Save the selected office to SharedPreferences
        saveSelectedOfficeToPrefs(office.getId());
        
        // Show confirmation to user
        Toast.makeText(this, "Office set to: " + office.getName(), Toast.LENGTH_SHORT).show();
        
        // Update geofencing for the selected office
        geofencingService.switchActiveOffice(office);
        
        // Format distance string based on current location
        calculateAndDisplayDistanceToOffice();
    }
    
    /**
     * Calculates and displays the distance to the currently selected office
     */
    private void calculateAndDisplayDistanceToOffice() {
        if (selectedOffice == null) return;
        
        Location currentLocation = geofencingService.getCurrentLocationLiveData().getValue();
        if (currentLocation != null) {
            float distance = geofencingService.calculateDistanceToOffice(selectedOffice, currentLocation);
            if (distance >= 0) {
                String distanceText = formatDistance(distance);
                binding.distanceText.setText(distanceText);
            }
        }
    }
    
    /**
     * Formats distance in meters to a human-readable string
     */
    private String formatDistance(float distanceInMeters) {
        if (distanceInMeters < 1000) {
            return String.format(Locale.getDefault(), "%.0f meters", distanceInMeters);
        } else {
            return String.format(Locale.getDefault(), "%.1f km", distanceInMeters / 1000);
        }
    }

    /**
     * Checks if GPS is enabled and shows a dialog if it's not
     * This prevents users from trying to check in without proper location services
     */
    private void checkGpsStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        
        if (!gpsEnabled) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("GPS Required")
                    .setMessage("Please enable GPS to use check-in functionality.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    /**
     * Updates the attendance status display with visual indicators
     */
    private void updateAttendanceStatusDisplay() {
        if (currentUser == null) return;
        
        // Get today's date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        // Fetch latest attendance for today to update status
        attendanceViewModel.loadAttendanceHistory(currentUser.getUid());
        
        // Set up observer for loading state
        attendanceViewModel.getLoadingLiveData().observe(this, isLoading -> {
            if (isLoading) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
        
        // Display any errors
        attendanceViewModel.getErrorLiveData().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showErrorSnackbar(error);
                attendanceViewModel.resetError();
            }
        });
    }
    
    /**
     * Displays network connection status
     * @param isConnected true if network is connected, false otherwise
     */
    private void updateNetworkStatusDisplay(boolean isConnected) {
        if (isConnected) {
            binding.networkStatusText.setText("Online");
            binding.networkStatusText.setTextColor(getColor(R.color.status_on_time));
            binding.networkStatusIndicator.setImageResource(R.drawable.ic_network_connected);
        } else {
            binding.networkStatusText.setText("Offline (data will sync later)");
            binding.networkStatusText.setTextColor(getColor(R.color.status_late));
            binding.networkStatusIndicator.setImageResource(R.drawable.ic_network_disconnected);
        }
    }
    
    /**
     * Shows check-in success animation
     */
    private void showCheckInSuccessAnimation() {
        // Get current time formatted nicely
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());
        
        // Update UI with success state
        binding.checkStatusValue.setText("Checked In");
        binding.checkStatusValue.setTextColor(getColor(R.color.status_on_time));
        
        // Create and show snackbar
        Snackbar snackbar = Snackbar.make(binding.getRoot(), 
                "Successfully checked in at " + currentTime, 
                Snackbar.LENGTH_LONG);
        
        snackbar.setBackgroundTint(getColor(R.color.status_on_time));
        snackbar.setTextColor(Color.WHITE);
        snackbar.show();
        
        // Animate the status card with a bounce effect
        binding.statusCard.setAlpha(0.7f);
        binding.statusCard.animate()
                .alpha(1.0f)
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(300)
                .withEndAction(() -> {
                    binding.statusCard.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200);
                });
        
        // Update the check-in button
        binding.checkInButton.setText("Check Out");
        binding.checkInButton.setEnabled(true);
        binding.checkInButton.setBackgroundTintList(getColorStateList(R.color.status_late));
    }
    
    /**
     * Shows check-out success animation
     */
    private void showCheckOutSuccessAnimation() {
        // Get formatted time
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());
        
        // Update UI with checkout state
        binding.checkStatusValue.setText("Not Checked In");
        binding.checkStatusValue.setTextColor(getColor(R.color.status_missed));
        
        // Create and show snackbar
        Snackbar snackbar = Snackbar.make(binding.getRoot(), 
                "Successfully checked out at " + currentTime, 
                Snackbar.LENGTH_LONG);
        
        snackbar.setBackgroundTint(getColor(R.color.status_late));
        snackbar.setTextColor(Color.WHITE);
        snackbar.show();
        
        // Animate the status card
        binding.statusCard.animate()
                .alpha(0.7f)
                .setDuration(200)
                .withEndAction(() -> {
                    binding.statusCard.animate()
                            .alpha(1.0f)
                            .setDuration(200);
                });
        
        // Update the check-in button
        binding.checkInButton.setText("Check In");
        boolean isInsideOffice = Boolean.TRUE.equals(geofencingService.getIsInsideGeofenceLiveData().getValue());
        binding.checkInButton.setEnabled(isInsideOffice);
        binding.checkInButton.setBackgroundTintList(getColorStateList(R.color.status_on_time));
    }
    
    /**
     * Performs refresh of all dashboard data
     */
    private void refreshDashboard() {
        // Update network status
        updateNetworkStatusDisplay(isNetworkAvailable());
        
        // Refresh location
        refreshLocation();
        
        // Update attendance data
        if (currentUser != null) {
            attendanceViewModel.loadAttendanceHistory(currentUser.getUid());
            attendanceViewModel.loadMonthlySummary(currentUser.getUid());
        }
        
        // Update selected office display
        updateSelectedOfficeUI();
        
        // Show refreshed notification
        Snackbar.make(binding.getRoot(), "Dashboard refreshed", Snackbar.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Refresh dashboard data when returning to the activity
        refreshDashboard();
        
        // Start location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            geofencingService.startLocationUpdates();
        }
        
        // Always update check-in status when resuming
        if (currentUser != null) {
            fetchCheckInStatus();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Stop location updates when the activity is not visible
        geofencingService.stopLocationUpdates();
    }

    /**
     * Explicitly fetches the current check-in status from Firestore
     * This ensures the UI always reflects the accurate database state
     */
    private void fetchCheckInStatus() {
        if (currentUser == null) return;
        
        // Show loading state
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Get today's date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        
        Log.d("EmployeeDashboard", "Explicitly fetching check-in status for user: " + currentUser.getUid());
        
        // Query Firestore directly to check if user is checked in
        FirebaseFirestore.getInstance()
            .collection("attendance")
            .document(currentUser.getUid())
            .collection(today)
            .get()
            .addOnSuccessListener(documents -> {
                boolean wasCheckedIn = isCheckedIn;
                
                // User is checked in if there's a document for today with null checkOutTime
                isCheckedIn = false;
                for (DocumentSnapshot doc : documents) {
                    if (doc.get("checkOutTime") == null) {
                        isCheckedIn = true;
                        break;
                    }
                }
                
                Log.d("EmployeeDashboard", "Explicit fetch completed. Check-in status: " + 
                      (wasCheckedIn ? "was checked in" : "was not checked in") + " → " +
                      (isCheckedIn ? "is checked in" : "is not checked in"));
                
                // Update UI immediately with accurate status
                updateCheckInStatus();
                binding.progressBar.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> {
                Log.e("EmployeeDashboard", "Failed to fetch check-in status: " + e.getMessage());
                // On failure, don't change status but hide loading
                binding.progressBar.setVisibility(View.GONE);
                showErrorSnackbar("Failed to check attendance status");
            });
    }

    /**
     * Saves the selected office ID to SharedPreferences
     * @param officeId The ID of the selected office
     */
    private void saveSelectedOfficeToPrefs(String officeId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SELECTED_OFFICE_KEY, officeId);
        editor.apply();
        Log.d(TAG, "Saved office ID to preferences: " + officeId);
    }
    
    /**
     * Loads the selected office ID from SharedPreferences
     * @return The saved office ID or null if none exists
     */
    private String loadSelectedOfficeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String officeId = prefs.getString(SELECTED_OFFICE_KEY, null);
        Log.d(TAG, "Loaded office ID from preferences: " + officeId);
        return officeId;
    }
}