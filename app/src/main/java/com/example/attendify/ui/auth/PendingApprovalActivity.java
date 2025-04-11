package com.example.attendify.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.attendify.databinding.ActivityPendingApprovalBinding;
import com.example.attendify.model.User;
import com.example.attendify.viewmodel.AuthViewModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

/**
 * Activity shown when a user is registered but waiting for admin approval
 */
public class PendingApprovalActivity extends AppCompatActivity {

    private ActivityPendingApprovalBinding binding;
    private AuthViewModel authViewModel;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingApprovalBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize view model
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Get current user
        authViewModel.getCurrentUser().observe(this, this::handleUser);

        // Set up logout button click listener
        binding.logoutButton.setOnClickListener(v -> {
            authViewModel.logout();
            startActivity(new Intent(this, AuthActivity.class));
            finish();
        });
    }

    private void handleUser(User user) {
        if (user == null) {
            // No user logged in, go back to login screen
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        // Save the user ID
        currentUserId = user.getUid();

        // Update the UI with user info
        binding.pendingTitle.setText("Account Pending Approval");
        
        // Different message based on user role
        String roleSpecificMessage = user.isAdmin() 
            ? "As an administrator, your account requires approval from an existing admin before you can access the dashboard."
            : "Your account is currently pending approval from an administrator.";
            
        binding.pendingMessage.setText(
                "Hello " + user.getName() + ",\n\n" +
                roleSpecificMessage + " " +
                "You will receive a notification once your account has been approved.");

        // Subscribe to FCM topic for approval notifications
        subscribeToPendingApprovalTopic();
    }

    private void subscribeToPendingApprovalTopic() {
        // Subscribe to topic with the current user's ID to receive approval notifications
        if (currentUserId != null && !currentUserId.isEmpty()) {
            FirebaseMessaging.getInstance().subscribeToTopic("user_approval_" + currentUserId)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Subscribed successfully
                        } else {
                            // Failed to subscribe
                        }
                    });
        }
    }
} 