package com.example.attendify.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.attendify.model.User;
import com.example.attendify.repository.UserRepository;

import java.util.List;

/**
 * ViewModel for handling user-related data
 */
public class UserViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<List<User>> employeesLiveData = new MutableLiveData<>();
    private final MutableLiveData<User> currentUserLiveData = new MutableLiveData<>();
    
    public UserViewModel() {
        userRepository = UserRepository.getInstance();
        fetchCurrentUser();
    }
    
    /**
     * Get all employees (users with role "employee")
     * @return LiveData containing list of employee users
     */
    public LiveData<List<User>> getEmployees() {
        // Fetch employees from repository
        userRepository.getAllEmployees().observeForever(employees -> {
            employeesLiveData.setValue(employees);
        });
        
        return employeesLiveData;
    }
    
    /**
     * Gets the currently logged-in user
     * @return LiveData containing the current user
     */
    public LiveData<User> getCurrentUser() {
        return currentUserLiveData;
    }
    
    /**
     * Refreshes the current user data from the repository
     */
    public void refreshUserData() {
        fetchCurrentUser();
    }
    
    /**
     * Helper method to fetch current user from repository
     */
    private void fetchCurrentUser() {
        userRepository.getCurrentUser().observeForever(user -> {
            currentUserLiveData.setValue(user);
        });
    }
} 