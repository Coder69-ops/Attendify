package com.example.attendify.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.attendify.model.User;
import com.example.attendify.repository.UserRepository;
import com.example.attendify.util.SingleLiveEvent;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {
    
    private final UserRepository userRepository;
    private final MutableLiveData<User> currentUserLiveData;
    private final SingleLiveEvent<String> errorLiveData;
    private final MutableLiveData<Boolean> loadingLiveData;
    private final SingleLiveEvent<NavigationEvent> navigationLiveData;

    public AuthViewModel() {
        userRepository = UserRepository.getInstance();
        currentUserLiveData = new MutableLiveData<>();
        errorLiveData = new SingleLiveEvent<>();
        loadingLiveData = new MutableLiveData<>(false);
        navigationLiveData = new SingleLiveEvent<>();
        
        // Start listening to auth state changes
        userRepository.getAuthStateListener(this::handleAuthStateChange);
    }

    private void handleAuthStateChange(FirebaseUser firebaseUser) {
        if (firebaseUser != null) {
            loadingLiveData.setValue(true);
            userRepository.getUserProfile(firebaseUser.getUid())
                    .observeForever(result -> {
                        loadingLiveData.setValue(false);
                        currentUserLiveData.setValue(result);
                        if (result != null) {
                            handleUserNavigation(result);
                        }
                    });
        } else {
            currentUserLiveData.setValue(null);
            navigationLiveData.setValue(new NavigationEvent(NavigationTarget.AUTH));
        }
    }

    private void handleUserNavigation(User user) {
        // First check if user is approved or not
        if (!user.isApproved()) {
            // Any user (admin or employee) who is not approved goes to pending approval
            navigationLiveData.setValue(new NavigationEvent(NavigationTarget.PENDING_APPROVAL));
        } else {
            // User is approved, now check role
            if (user.isAdmin()) {
                // Approved admin goes to admin dashboard
                navigationLiveData.setValue(new NavigationEvent(NavigationTarget.ADMIN_DASHBOARD));
            } else if (user.isEmployee()) {
                // Approved employee goes to employee dashboard
                navigationLiveData.setValue(new NavigationEvent(NavigationTarget.EMPLOYEE_DASHBOARD));
            }
        }
    }

    public void signIn(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorLiveData.setValue("Email and password are required");
            return;
        }

        loadingLiveData.setValue(true);
        userRepository.loginUser(email, password)
                .observeForever(user -> {
                    loadingLiveData.setValue(false);
                    if (user == null) {
                        errorLiveData.setValue("Invalid email or password");
                    } else if (!user.isApproved()) {
                        errorLiveData.setValue("Your account is pending approval");
                    } else {
                        currentUserLiveData.setValue(user);
                    }
                });
    }

    // Alias for loginUser to maintain compatibility
    public void loginUser(String email, String password) {
        signIn(email, password);
    }

    public void registerUser(String name, String email, String password, String officeId) {
        if (name == null || name.trim().isEmpty() || 
            email == null || email.trim().isEmpty() || 
            password == null || password.trim().isEmpty() || 
            officeId == null || officeId.trim().isEmpty()) {
            errorLiveData.setValue("All fields are required");
            return;
        }

        loadingLiveData.setValue(true);
        userRepository.registerUser(email, password, name, "employee", officeId)
                .observeForever(user -> {
                    loadingLiveData.setValue(false);
                    if (user == null) {
                        errorLiveData.setValue("Registration failed");
                    } else {
                        currentUserLiveData.setValue(user);
                        navigationLiveData.setValue(new NavigationEvent(NavigationTarget.PENDING_APPROVAL));
                    }
                });
    }
    
    // New method for registering with a direct office location string
    public void registerUser(String name, String email, String password, String role, String officeLocation) {
        if (name == null || name.trim().isEmpty() || 
            email == null || email.trim().isEmpty() || 
            password == null || password.trim().isEmpty() || 
            officeLocation == null || officeLocation.trim().isEmpty()) {
            errorLiveData.setValue("All fields are required");
            return;
        }

        loadingLiveData.setValue(true);
        userRepository.registerUser(email, password, name, role, officeLocation)
                .observeForever(user -> {
                    loadingLiveData.setValue(false);
                    if (user == null) {
                        errorLiveData.setValue("Registration failed");
                    } else {
                        currentUserLiveData.setValue(user);
                        navigationLiveData.setValue(new NavigationEvent(NavigationTarget.PENDING_APPROVAL));
                    }
                });
    }

    public void logout() {
        userRepository.logout();
        currentUserLiveData.setValue(null);
        navigationLiveData.setValue(new NavigationEvent(NavigationTarget.AUTH));
    }

    public LiveData<User> getCurrentUser() {
        return currentUserLiveData;
    }
    
    // Alias for getCurrentUser to maintain compatibility
    public LiveData<User> getUserLiveData() {
        return currentUserLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public LiveData<NavigationEvent> getNavigationLiveData() {
        return navigationLiveData;
    }

    public void resetError() {
        errorLiveData.setValue(null);
    }

    public enum NavigationTarget {
        AUTH,
        ADMIN_DASHBOARD,
        EMPLOYEE_DASHBOARD,
        PENDING_APPROVAL
    }

    public static class NavigationEvent {
        private final NavigationTarget target;

        public NavigationEvent(NavigationTarget target) {
            this.target = target;
        }

        public NavigationTarget getTarget() {
            return target;
        }
    }
}