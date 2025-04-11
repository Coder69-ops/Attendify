package com.example.attendify.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.attendify.R;
import com.example.attendify.databinding.FragmentHomeBinding;
import com.example.attendify.model.Attendance;
import com.example.attendify.model.User;
import com.example.attendify.viewmodel.AttendanceViewModel;
import com.example.attendify.viewmodel.UserViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private UserViewModel userViewModel;
    private AttendanceViewModel attendanceViewModel;
    private RecentAttendanceAdapter adapter;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat timeFormat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        attendanceViewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);
        dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupSwipeRefresh();
        setupCheckInOutButton();
        setupHelpButton();
        observeUserData();
        observeAttendanceData();
    }

    private void setupRecyclerView() {
        adapter = new RecentAttendanceAdapter();
        binding.recentAttendanceList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recentAttendanceList.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            refreshData();
        });
    }

    private void setupCheckInOutButton() {
        binding.checkInOutButton.setOnClickListener(v -> {
            // TODO: Implement check-in/out logic
            Snackbar.make(binding.getRoot(), "Check-in/out functionality coming soon!", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void setupHelpButton() {
        binding.helpFab.setOnClickListener(v -> {
            // TODO: Implement help functionality
            Snackbar.make(binding.getRoot(), "Help functionality coming soon!", Snackbar.LENGTH_SHORT).show();
        });
    }

    private void observeUserData() {
        userViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                updateUserUI(user);
            }
        });
    }

    private void observeAttendanceData() {
        attendanceViewModel.getRecentAttendance().observe(getViewLifecycleOwner(), attendances -> {
            if (attendances != null) {
                adapter.submitList(attendances);
            }
            binding.swipeRefresh.setRefreshing(false);
        });
    }

    private void updateUserUI(User user) {
        // Update welcome message
        binding.welcomeText.setText(getString(R.string.welcome_message, user.getDisplayName()));
        
        // Update date
        binding.dateText.setText(dateFormat.format(new Date()));
        
        // Load profile image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                .load(user.getProfileImageUrl())
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(binding.profileImage);
        }
    }

    private void refreshData() {
        userViewModel.refreshUserData();
        attendanceViewModel.refreshAttendanceData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 