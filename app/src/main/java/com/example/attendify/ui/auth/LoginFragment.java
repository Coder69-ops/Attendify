package com.example.attendify.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendify.databinding.FragmentLoginBinding;
import com.example.attendify.model.User;
import com.example.attendify.viewmodel.AuthViewModel;

/**
 * Fragment for user login
 */
public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthViewModel authViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        // Set up click listeners
        binding.loginButton.setOnClickListener(v -> handleLogin());
        binding.forgotPasswordText.setOnClickListener(v -> handleForgotPassword());

        // Set up observers
        setupObservers();
    }

    private void setupObservers() {
        // Observe loading state
        authViewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), isLoading ->
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));

        // Observe errors
        authViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                authViewModel.resetError();
            }
        });

        // Observe user state
        authViewModel.getUserLiveData().observe(getViewLifecycleOwner(), this::handleUserState);
    }

    private void handleLogin() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError("Password is required");
            return;
        }

        // Clear errors
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        // Attempt login
        authViewModel.signIn(email, password);
    }
    
    private void handleForgotPassword() {
        String email = binding.emailEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError("Enter your email to reset password");
            return;
        }
        
        // TODO: Implement password reset functionality
        Toast.makeText(requireContext(), "Password reset email sent to " + email, Toast.LENGTH_SHORT).show();
    }

    private void handleUserState(User user) {
        if (user != null) {
            ((AuthActivity) requireActivity()).navigateBasedOnUserStatus(user);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 