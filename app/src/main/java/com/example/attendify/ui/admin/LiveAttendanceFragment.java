package com.example.attendify.ui.admin;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.attendify.R;
import com.example.attendify.databinding.FragmentLiveAttendanceBinding;
import com.example.attendify.model.Attendance;
import com.example.attendify.model.Office;
import com.example.attendify.viewmodel.AttendanceViewModel;
import com.example.attendify.viewmodel.OfficeViewModel;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.List;

public class LiveAttendanceFragment extends Fragment {

    private static final String TAG = "LiveAttendanceFragment";

    private FragmentLiveAttendanceBinding binding;
    private AttendanceViewModel attendanceViewModel;
    private OfficeViewModel officeViewModel;
    private LiveAttendanceAdapter adapter;
    private String selectedOfficeId = null;
    private boolean showOnlyInOffice = false;
    private boolean isDataLoaded = false;

    // Add handler for periodic updates
    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = this::refreshAttendanceData;
    private static final long UPDATE_INTERVAL = 30000; // 30 seconds

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            binding = FragmentLiveAttendanceBinding.inflate(inflater, container, false);
            return binding.getRoot();
        } catch (Exception e) {
            Log.e(TAG, "Error inflating fragment view: " + e.getMessage(), e);
            // Fallback to a simple view if inflation fails
            View errorView = new View(requireContext());
            Toast.makeText(requireContext(), "Error loading attendance view", Toast.LENGTH_SHORT).show();
            return errorView;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            // Configure Firestore for offline capability
            configureFirestoreOfflineSupport();
            
            // Initialize ViewModels
            initializeViewModels();

            // Set up RecyclerView
            setupRecyclerView();

            // Set up observers
            setupObservers();

            // Set initial UI state
            binding.emptyText.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.attendanceRecyclerView.setVisibility(View.GONE);

            // Load data
            loadData();
        } catch (Exception e) {
            Log.e(TAG, "Error during fragment initialization: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error initializing attendance view", Toast.LENGTH_SHORT).show();
            showErrorState();
        }
    }
    
    private void initializeViewModels() {
        try {
            attendanceViewModel = new ViewModelProvider(requireActivity()).get(AttendanceViewModel.class);
            officeViewModel = new ViewModelProvider(requireActivity()).get(OfficeViewModel.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize ViewModels: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading attendance data", Toast.LENGTH_SHORT).show();
            throw e; // Re-throw to be caught by the outer try-catch
        }
    }
    
    private void configureFirestoreOfflineSupport() {
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (Exception e) {
            Log.w(TAG, "Error configuring Firestore offline support: " + e.getMessage());
            // Continue without offline support
        }
    }
    
    private void showErrorState() {
        if (binding != null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.emptyText.setText("Error loading attendance data");
            binding.emptyText.setVisibility(View.VISIBLE);
            binding.attendanceRecyclerView.setVisibility(View.GONE);
            
            // Set counters to zero
            binding.presentCount.setText("0");
            binding.lateCount.setText("0");
            binding.absentCount.setText("0");
            binding.inOfficeCount.setText("0");
            binding.inOfficePercentage.setText("0%");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // Only refresh data if the fragment is now visible
        if (getUserVisibleHint()) {
            refreshAttendanceData();
        }
        
        // Start periodic updates when fragment is visible
        startPeriodicUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        
        // Stop periodic updates when fragment is not visible
        stopPeriodicUpdates();
    }

    private void setupRecyclerView() {
        try {
            adapter = new LiveAttendanceAdapter();
            binding.attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.attendanceRecyclerView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerView: " + e.getMessage());
        }
    }

    private void setupObservers() {
        try {
            // Observe offices for filter chips
            officeViewModel.getOfficesLiveData().observe(getViewLifecycleOwner(), this::setupOfficeFilters);

            // Observe live attendance data
            attendanceViewModel.getLiveAttendanceData().observe(getViewLifecycleOwner(), attendanceList -> {
                isDataLoaded = true;
                handleAttendanceData(attendanceList);
            });

            // Observe daily summary
            attendanceViewModel.getDailySummaryLiveData().observe(getViewLifecycleOwner(), summary -> {
                if (summary != null) {
                    binding.presentCount.setText(String.valueOf(summary.getOnTime()));
                    binding.lateCount.setText(String.valueOf(summary.getLate()));
                    binding.absentCount.setText(String.valueOf(summary.getMissed()));
                }
            });

            // Observe loading state
            attendanceViewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
                if (binding != null) {
                    binding.progressBar.setVisibility(isLoading && !isDataLoaded ? View.VISIBLE : View.GONE);
                }
            });

            // Observe errors
            attendanceViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
                if (error != null) {
                    handleError(error);
                    attendanceViewModel.resetError();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up observers: " + e.getMessage());
            showErrorState();
        }
    }
    
    private void handleError(String error) {
        try {
            // Check if it's a Firestore indexing error
            if (error.contains("FAILED_PRECONDITION") && error.contains("requires") && error.contains("index")) {
                // Show a more helpful message for index errors
                String baseMessage = "Database setup required for 'All Offices' view.";
                String adminMessage = selectedOfficeId == null ? 
                    "Please use a specific office filter until the admin completes setup." : 
                    "Using fallback method to show data.";
                
                Toast.makeText(requireContext(), 
                    baseMessage + " " + adminMessage, 
                    Toast.LENGTH_LONG).show();
                
                Log.w(TAG, "Firestore index error: " + error);
            } else if (error.contains("network") || error.contains("UNAVAILABLE")) {
                // Network related errors
                Toast.makeText(requireContext(), 
                    "Network unavailable. Showing cached data if available.", 
                    Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Network error: " + error);
            } else {
                // Regular error handling
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading data: " + error);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling error message: " + e.getMessage());
        }
    }

    private void setupOfficeFilters(List<Office> offices) {
        binding.officeFilterChips.removeAllViews();

        // Add "All Offices" chip
        Chip allChip = new Chip(requireContext());
        allChip.setText("All Offices");
        allChip.setCheckable(true);
        allChip.setChecked(selectedOfficeId == null);
        allChip.setTag(null); // Explicitly set null tag for "All Offices"
        allChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedOfficeId = null;
                Log.d(TAG, "All Offices selected");
                loadAttendanceData();
            }
        });
        binding.officeFilterChips.addView(allChip);

        // Add office filter chips
        for (Office office : offices) {
            Chip chip = new Chip(requireContext());
            chip.setText(office.getName());
            chip.setCheckable(true);
            chip.setChecked(office.getId().equals(selectedOfficeId));
            chip.setTag(office.getId()); // Set office ID as tag
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedOfficeId = office.getId();
                    Log.d(TAG, "Office selected: " + office.getName() + " (ID: " + office.getId() + ")");
                    loadAttendanceData();
                }
            });
            binding.officeFilterChips.addView(chip);
        }
    }

    private void handleAttendanceData(List<Attendance> attendanceList) {
        // Hide loading progress
        binding.progressBar.setVisibility(View.GONE);
        
        // Handle empty state
        if (attendanceList == null || attendanceList.isEmpty()) {
            showEmptyState();
            return;
        }
        
        // Count active employees and in-office status
        int totalActiveEmployees = 0;
        int inOfficeCount = 0;
        List<Attendance> filteredList = new ArrayList<>();
        
        for (Attendance attendance : attendanceList) {
            // Process active (checked-in) employees
            if (attendance.getCheckOutTime() == null) {
                totalActiveEmployees++;
                if (attendance.isInOffice()) {
                    inOfficeCount++;
                    
                    // Add to filtered list if showing only in-office employees
                    if (showOnlyInOffice) {
                        filteredList.add(attendance);
                    }
                }
            }
            
            // Add to list if not filtering or filtering doesn't apply
            if (!showOnlyInOffice) {
                filteredList.add(attendance);
            }
        }
        
        // Update counters and percentage
        updateAttendanceCounters(inOfficeCount, totalActiveEmployees);
        
        // Update UI based on filtered results
        if (showOnlyInOffice && filteredList.isEmpty()) {
            binding.emptyText.setText("No employees currently in office");
            binding.emptyText.setVisibility(View.VISIBLE);
            binding.attendanceRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyText.setVisibility(View.GONE);
            binding.attendanceRecyclerView.setVisibility(View.VISIBLE);
            adapter.submitList(filteredList);
        }
    }

    private void showEmptyState() {
        binding.emptyText.setVisibility(View.VISIBLE);
        binding.attendanceRecyclerView.setVisibility(View.GONE);
        binding.inOfficeCount.setText("0");
        binding.inOfficePercentage.setText("0%");
        binding.inOfficePercentage.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.status_missed));
    }

    private void updateAttendanceCounters(int inOfficeCount, int totalActiveEmployees) {
        // Update the in-office counter
        binding.inOfficeCount.setText(String.valueOf(inOfficeCount));
        
        // Calculate and update percentage
        int percentage = (totalActiveEmployees > 0) ? 
                        (inOfficeCount * 100 / totalActiveEmployees) : 0;
        binding.inOfficePercentage.setText(percentage + "%");
        
        // Apply color based on percentage
        int colorRes;
        if (percentage >= 75) {
            colorRes = R.color.status_on_time; // Green
        } else if (percentage >= 50) {
            colorRes = android.R.color.holo_blue_dark; // Blue
        } else if (percentage >= 25) {
            colorRes = R.color.status_late; // Yellow/Orange
        } else {
            colorRes = R.color.status_missed; // Red
        }
        
        binding.inOfficePercentage.setTextColor(
                ContextCompat.getColor(requireContext(), colorRes));
    }

    private void loadData() {
        officeViewModel.loadOffices();
        loadAttendanceData();
    }

    private void loadAttendanceData() {
        // Log what we're loading for debugging
        Log.d(TAG, "Loading attendance data for " + 
              (selectedOfficeId == null ? "all offices" : "office: " + selectedOfficeId));
              
        // Only show loading indicator for initial load, not refreshes
        if (adapter.getCurrentList().isEmpty()) {
            binding.progressBar.setVisibility(View.VISIBLE);
        }
        
        // Load data from view models
        attendanceViewModel.loadLiveAttendanceData(selectedOfficeId);
        attendanceViewModel.loadDailySummary(selectedOfficeId);
    }

    /**
     * Start periodic updates of attendance data
     */
    private void startPeriodicUpdates() {
        // Clear any existing callbacks first to avoid duplication
        stopPeriodicUpdates();
        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }

    /**
     * Stop periodic updates of attendance data
     */
    private void stopPeriodicUpdates() {
        updateHandler.removeCallbacks(updateRunnable);
    }

    /**
     * Refresh attendance data and summary
     */
    public void refreshAttendanceData() {
        Log.d(TAG, "Refreshing attendance data");
        
        // Get selected office ID
        String officeId = null;
        boolean allOfficesSelected = true;
        
        for (int i = 0; i < binding.officeFilterChips.getChildCount(); i++) {
            Chip chip = (Chip) binding.officeFilterChips.getChildAt(i);
            if (chip.isChecked()) {
                if (i > 0) { // First chip (index 0) is "All Offices"
                    officeId = (String) chip.getTag();
                    allOfficesSelected = false;
                    Log.d(TAG, "Selected office: " + chip.getText() + " (ID: " + officeId + ")");
                } else {
                    Log.d(TAG, "All Offices selected");
                    selectedOfficeId = null; // Explicitly set to null for "All Offices"
                }
                break;
            }
        }
        
        // Load attendance data and summary for selected office (or all offices if null)
        attendanceViewModel.loadLiveAttendanceData(officeId);
        attendanceViewModel.loadDailySummary(officeId);
        
        // Schedule next update
        if (isAdded() && isResumed()) {
            startPeriodicUpdates();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPeriodicUpdates();
        binding = null;
    }
}