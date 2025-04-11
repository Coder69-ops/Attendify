package com.example.attendify.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.attendify.model.User;
import com.example.attendify.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EmployeeViewModel extends ViewModel {
    
    private final UserRepository userRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MediatorLiveData<List<User>> allEmployees = new MediatorLiveData<>();
    private final MediatorLiveData<List<User>> managers = new MediatorLiveData<>();
    
    public EmployeeViewModel() {
        userRepository = UserRepository.getInstance();
        loadData();
    }
    
    private void loadData() {
        isLoading.setValue(true);
        
        // Load all employees
        LiveData<List<User>> employeesSource = userRepository.getAllUsers();
        allEmployees.addSource(employeesSource, users -> {
            if (users != null) {
                allEmployees.setValue(users);
            } else {
                allEmployees.setValue(new ArrayList<>());
            }
            isLoading.setValue(false);
        });
        
        // Load managers (users with manager role or isManager flag)
        LiveData<List<User>> managersSource = userRepository.getAllUsers();
        managers.addSource(managersSource, users -> {
            if (users != null) {
                List<User> managersList = users.stream()
                        .filter(user -> "manager".equalsIgnoreCase(user.getRole()) || user.isManager())
                        .collect(Collectors.toList());
                managers.setValue(managersList);
            } else {
                managers.setValue(new ArrayList<>());
            }
        });
    }
    
    public LiveData<List<User>> getAllEmployees() {
        return allEmployees;
    }
    
    public LiveData<List<User>> getManagers() {
        return managers;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public void saveUser(User user) {
        isLoading.setValue(true);
        
        userRepository.saveUser(user, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                // Refresh the employee list
                loadData();
            } else {
                errorMessage.setValue("Failed to save user: " + 
                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }
    
    public void deleteUser(String userId) {
        isLoading.setValue(true);
        
        userRepository.deleteUser(userId, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                // Refresh the employee list
                loadData();
            } else {
                errorMessage.setValue("Failed to delete user: " + 
                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }
    
    public void filterEmployeesByDepartment(String departmentId) {
        if (departmentId == null || departmentId.isEmpty()) {
            // Reset to show all employees
            loadData();
            return;
        }
        
        isLoading.setValue(true);
        userRepository.getUsersByDepartment(departmentId, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                allEmployees.setValue(task.getResult());
            } else {
                errorMessage.setValue("Failed to filter employees: " + 
                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }
    
    public void filterEmployeesByTeam(String teamId) {
        if (teamId == null || teamId.isEmpty()) {
            // Reset to show all employees
            loadData();
            return;
        }
        
        isLoading.setValue(true);
        userRepository.getUsersByTeam(teamId, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful()) {
                allEmployees.setValue(task.getResult());
            } else {
                errorMessage.setValue("Failed to filter employees: " + 
                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
            }
        });
    }

    // Used to cancel any ongoing operations when ViewModel is cleared
    @Override
    protected void onCleared() {
        super.onCleared();
        // Unregister listeners if needed
    }
} 