package com.example.attendify.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.attendify.R;
import com.example.attendify.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserEditDialog extends Dialog {

    private final User user;
    private OnUserSavedListener onUserSavedListener;
    
    // UI components
    private TextInputEditText editFullName, editEmail, editEmployeeId, editPassword, editConfirmPassword;
    private TextInputLayout layoutFullName, layoutEmail, layoutEmployeeId, layoutPassword, layoutConfirmPassword;
    private TextInputLayout layoutRole, layoutOffice, layoutDepartment, layoutTeam, layoutManager;
    private AutoCompleteTextView dropdownRole, dropdownOffice, dropdownDepartment, dropdownTeam, dropdownManager;
    private SwitchMaterial switchActive, switchIsManager;
    private MaterialButton btnCancel, btnSave;

    public interface OnUserSavedListener {
        void onUserSaved(User user);
    }

    public UserEditDialog(@NonNull Context context, User user) {
        super(context);
        this.user = user;
    }

    public void setOnUserSavedListener(OnUserSavedListener listener) {
        this.onUserSavedListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        setContentView(R.layout.dialog_user_edit);
        
        // Set dialog width to 90% of screen width
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(getWindow().getAttributes());
        layoutParams.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.9);
        getWindow().setAttributes(layoutParams);
        
        initViews();
        setupDropdowns();
        
        if (user != null) {
            populateUserData();
        }
        
        setupListeners();
    }

    private void initViews() {
        // TextInputEditText fields
        editFullName = findViewById(R.id.editFullName);
        editEmail = findViewById(R.id.editEmail);
        editEmployeeId = findViewById(R.id.editEmployeeId);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        
        // TextInputLayout fields
        layoutFullName = findViewById(R.id.layoutFullName);
        layoutEmail = findViewById(R.id.layoutEmail);
        layoutEmployeeId = findViewById(R.id.layoutEmployeeId);
        layoutPassword = findViewById(R.id.layoutPassword);
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword);
        layoutRole = findViewById(R.id.layoutRole);
        layoutOffice = findViewById(R.id.layoutOffice);
        layoutDepartment = findViewById(R.id.layoutDepartment);
        layoutTeam = findViewById(R.id.layoutTeam);
        layoutManager = findViewById(R.id.layoutManager);
        
        // Dropdown fields
        dropdownRole = findViewById(R.id.dropdownRole);
        dropdownOffice = findViewById(R.id.dropdownOffice);
        dropdownDepartment = findViewById(R.id.dropdownDepartment);
        dropdownTeam = findViewById(R.id.dropdownTeam);
        dropdownManager = findViewById(R.id.dropdownManager);
        
        // Switches
        switchActive = findViewById(R.id.switchActive);
        switchIsManager = findViewById(R.id.switchIsManager);
        
        // Buttons
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);
        
        // If editing existing user, hide password fields
        if (user != null) {
            layoutPassword.setVisibility(View.GONE);
            layoutConfirmPassword.setVisibility(View.GONE);
        }
    }

    private void setupDropdowns() {
        // TODO: Load these from repositories
        
        // Sample roles
        List<String> roles = new ArrayList<>();
        roles.add("admin");
        roles.add("manager");
        roles.add("employee");
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(getContext(), 
                R.layout.item_dropdown, roles);
        dropdownRole.setAdapter(roleAdapter);
        
        // Sample offices
        List<String> offices = new ArrayList<>();
        offices.add("Headquarters");
        offices.add("Branch A");
        offices.add("Branch B");
        ArrayAdapter<String> officeAdapter = new ArrayAdapter<>(getContext(), 
                R.layout.item_dropdown, offices);
        dropdownOffice.setAdapter(officeAdapter);
        
        // Sample departments
        List<String> departments = new ArrayList<>();
        departments.add("Engineering");
        departments.add("Marketing");
        departments.add("Sales");
        departments.add("Human Resources");
        ArrayAdapter<String> departmentAdapter = new ArrayAdapter<>(getContext(), 
                R.layout.item_dropdown, departments);
        dropdownDepartment.setAdapter(departmentAdapter);
        
        // Sample teams
        List<String> teams = new ArrayList<>();
        teams.add("Team A");
        teams.add("Team B");
        teams.add("Team C");
        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(getContext(), 
                R.layout.item_dropdown, teams);
        dropdownTeam.setAdapter(teamAdapter);
        
        // Sample managers
        List<String> managers = new ArrayList<>();
        managers.add("John Doe");
        managers.add("Jane Smith");
        ArrayAdapter<String> managerAdapter = new ArrayAdapter<>(getContext(), 
                R.layout.item_dropdown, managers);
        dropdownManager.setAdapter(managerAdapter);
    }

    private void populateUserData() {
        if (user == null) return;
        
        editFullName.setText(user.getName());
        editEmail.setText(user.getEmail());
        editEmployeeId.setText(user.getEmployeeId());
        
        // Set dropdown values
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            dropdownRole.setText(user.getRole(), false);
        }
        
        String officeId = user.getOfficeId();
        if (officeId != null && !officeId.isEmpty()) {
            dropdownOffice.setText(officeId, false);
        }
        
        String departmentId = user.getDepartmentId();
        if (departmentId != null && !departmentId.isEmpty()) {
            dropdownDepartment.setText(departmentId, false);
        }
        
        String teamId = user.getTeamId();
        if (teamId != null && !teamId.isEmpty()) {
            dropdownTeam.setText(teamId, false);
        }
        
        String managerId = user.getManagerId();
        if (managerId != null && !managerId.isEmpty()) {
            dropdownManager.setText(managerId, false);
        }
        
        switchActive.setChecked(user.isActive());
        switchIsManager.setChecked(user.isManager());
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());
        
        btnSave.setOnClickListener(v -> {
            if (validateInput()) {
                saveUser();
            }
        });
        
        // Setup dropdown click listeners to show the dropdown when the whole layout is clicked
        setupDropdownClickListener(layoutRole, dropdownRole);
        setupDropdownClickListener(layoutOffice, dropdownOffice);
        setupDropdownClickListener(layoutDepartment, dropdownDepartment);
        setupDropdownClickListener(layoutTeam, dropdownTeam);
        setupDropdownClickListener(layoutManager, dropdownManager);
    }
    
    private void setupDropdownClickListener(TextInputLayout layout, AutoCompleteTextView dropdown) {
        layout.setOnClickListener(v -> dropdown.showDropDown());
        dropdown.setOnClickListener(v -> dropdown.showDropDown());
    }

    private boolean validateInput() {
        boolean isValid = true;
        
        // Reset errors
        layoutFullName.setError(null);
        layoutEmail.setError(null);
        layoutEmployeeId.setError(null);
        layoutPassword.setError(null);
        layoutConfirmPassword.setError(null);
        layoutRole.setError(null);
        
        // Validate name
        String fullName = Objects.requireNonNull(editFullName.getText()).toString().trim();
        if (fullName.isEmpty()) {
            layoutFullName.setError("Full name is required");
            isValid = false;
        }
        
        // Validate email
        String email = Objects.requireNonNull(editEmail.getText()).toString().trim();
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError("Valid email is required");
            isValid = false;
        }
        
        // Validate employee ID
        String employeeId = Objects.requireNonNull(editEmployeeId.getText()).toString().trim();
        if (employeeId.isEmpty()) {
            layoutEmployeeId.setError("Employee ID is required");
            isValid = false;
        }
        
        // Validate role
        String role = dropdownRole.getText().toString().trim();
        if (role.isEmpty()) {
            layoutRole.setError("Role is required");
            isValid = false;
        }
        
        // Validate password (only for new users)
        if (user == null) {
            String password = Objects.requireNonNull(editPassword.getText()).toString();
            String confirmPassword = Objects.requireNonNull(editConfirmPassword.getText()).toString();
            
            if (password.isEmpty()) {
                layoutPassword.setError("Password is required");
                isValid = false;
            } else if (password.length() < 6) {
                layoutPassword.setError("Password must be at least 6 characters");
                isValid = false;
            } else if (!password.equals(confirmPassword)) {
                layoutConfirmPassword.setError("Passwords do not match");
                isValid = false;
            }
        }
        
        return isValid;
    }

    private void saveUser() {
        User updatedUser = user != null ? new User(user.getUid(), 
                                                 Objects.requireNonNull(editFullName.getText()).toString().trim(),
                                                 Objects.requireNonNull(editEmail.getText()).toString().trim(),
                                                 dropdownRole.getText().toString().trim(),
                                                 true, // approved
                                                 dropdownOffice.getText().toString().trim()) 
                              : new User(
                                  Objects.requireNonNull(editEmail.getText()).toString().trim(),
                                  Objects.requireNonNull(editFullName.getText()).toString().trim(),
                                  dropdownRole.getText().toString().trim(),
                                  dropdownOffice.getText().toString().trim()
                              );
        
        // Set employee ID
        updatedUser.setEmployeeId(Objects.requireNonNull(editEmployeeId.getText()).toString().trim());
        
        // Set department and team
        updatedUser.setDepartmentId(dropdownDepartment.getText().toString().trim());
        updatedUser.setTeamId(dropdownTeam.getText().toString().trim());
        
        // Set manager and isManager flag
        updatedUser.setManagerId(dropdownManager.getText().toString().trim());
        updatedUser.setManager(switchIsManager.isChecked());
        
        // Set active status
        updatedUser.setActive(switchActive.isChecked());
        
        // Handle password for new users
        if (user == null) {
            // In a real implementation, we would handle Firebase Auth registration here
            // For now, just store password in the User object (not secure, just for demo)
            // TODO: Implement proper registration with Firebase Auth
            Toast.makeText(getContext(), "New user created. In a real app, this would register the user with Firebase Auth.", Toast.LENGTH_SHORT).show();
        }
        
        // Notify the listener
        if (onUserSavedListener != null) {
            onUserSavedListener.onUserSaved(updatedUser);
            dismiss();
        }
    }
} 