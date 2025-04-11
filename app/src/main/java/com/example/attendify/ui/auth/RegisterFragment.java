package com.example.attendify.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendify.databinding.FragmentRegisterBinding;
import com.example.attendify.model.Office;
import com.example.attendify.model.User;
import com.example.attendify.viewmodel.AuthViewModel;
import com.example.attendify.viewmodel.OfficeViewModel;

import java.util.ArrayList;
import java.util.List;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;
    private OfficeViewModel officeViewModel;
    private List<Office> offices = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModels
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);
        officeViewModel = new ViewModelProvider(this).get(OfficeViewModel.class);

        // Set up office dropdown
        setupOfficeDropdown();

        // Set up register button click listener
        binding.registerButton.setOnClickListener(v -> registerUser());

        // Observe user data changes
        authViewModel.getUserLiveData().observe(getViewLifecycleOwner(), this::handleUserState);

        // Observe errors
        authViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), this::handleError);

        // Observe loading state
        authViewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), this::handleLoading);

        // Observe offices
        officeViewModel.getOfficesLiveData().observe(getViewLifecycleOwner(), this::handleOffices);
    }

    private void setupOfficeDropdown() {
        // Load offices for reference
        officeViewModel.loadOffices();
    }

    private void handleOffices(List<Office> officeList) {
        if (officeList != null) {
            offices = officeList;
        }
    }

    private void registerUser() {
        String name = binding.nameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();
        String role = binding.adminRadioButton.isChecked() ? "admin" : "employee";
        String officeLocation = binding.officeAutoComplete.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError("Password is required");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.confirmPasswordLayout.setError("Confirm password is required");
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError("Passwords do not match");
            return;
        }

        if (TextUtils.isEmpty(officeLocation)) {
            binding.officeLayout.setError("Office location is required");
            return;
        }

        // Clear errors
        binding.nameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.confirmPasswordLayout.setError(null);
        binding.officeLayout.setError(null);

        // Show loading
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.registerButton.setEnabled(false);

        // Register user directly with the office location string
        authViewModel.registerUser(name, email, password, role, officeLocation);
    }
    
    private void handleUserState(User user) {
        if (user != null) {
            // User registered - Activity will handle navigation
            ((AuthActivity) requireActivity()).navigateBasedOnUserStatus(user);
        }
    }

    private void handleError(String error) {
        if (error != null) {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            authViewModel.resetError();
        }
    }

    private void handleLoading(Boolean isLoading) {
        if (isLoading != null && isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.registerButton.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.registerButton.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}