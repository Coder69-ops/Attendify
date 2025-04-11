package com.example.attendify.fragments.admin;

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
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.R;
import com.example.attendify.adapters.EmployeeAdapter;
import com.example.attendify.dialogs.CsvImportDialog;
import com.example.attendify.dialogs.UserEditDialog;
import com.example.attendify.model.User;
import com.example.attendify.utils.ThemeUtils;
import com.example.attendify.viewmodel.EmployeeViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class EmployeeManagementFragment extends Fragment {

    private RecyclerView recyclerViewEmployees;
    private EmployeeAdapter employeeAdapter;
    private List<User> employeeList = new ArrayList<>();
    private EmployeeViewModel viewModel;
    private View layoutLoading, layoutEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_employee_management, container, false);
        
        viewModel = new ViewModelProvider(requireActivity()).get(EmployeeViewModel.class);
        
        initViews(view);
        setupRecyclerView();
        setupObservers();
        setupListeners(view);
        
        return view;
    }

    private void initViews(View view) {
        recyclerViewEmployees = view.findViewById(R.id.recyclerViewEmployees);
        layoutLoading = view.findViewById(R.id.layoutLoading);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        
        // Initially show loading
        showLoading();
    }

    private void setupRecyclerView() {
        employeeAdapter = new EmployeeAdapter(requireContext(), employeeList);
        employeeAdapter.setOnItemClickListener(new EmployeeAdapter.OnItemClickListener() {
            @Override
            public void onEditClick(User user, int position) {
                showEditUserDialog(user);
            }

            @Override
            public void onPermissionsClick(User user, int position) {
                // Show permissions dialog with theme-friendly UI
                showPermissionsDialog(user);
            }

            @Override
            public void onMetricsClick(User user, int position) {
                // Navigate to metrics screen with theme transition
                navigateToMetricsScreen(user);
            }
        });
        
        recyclerViewEmployees.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewEmployees.setAdapter(employeeAdapter);
        
        // Apply theme-appropriate item decoration
        recyclerViewEmployees.addItemDecoration(ThemeUtils.getListDividerDecoration(requireContext()));
    }
    
    private void setupObservers() {
        // Observe all employees
        viewModel.getAllEmployees().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                employeeList.clear();
                employeeList.addAll(users);
                employeeAdapter.notifyDataSetChanged();
                
                if (employeeList.isEmpty()) {
                    showEmptyState();
                } else {
                    showContent();
                }
            }
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoading();
            }
        });
        
        // Observe error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListeners(View view) {
        // Import button
        MaterialButton btnImport = view.findViewById(R.id.btnImport);
        btnImport.setOnClickListener(v -> showImportDialog());
        
        // Export button
        MaterialButton btnExport = view.findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> exportUsers());
        
        // FAB for department management
        ExtendedFloatingActionButton fabManageDepartments = view.findViewById(R.id.fabManageDepartments);
        fabManageDepartments.setOnClickListener(v -> manageDepartments());
        
        // Add new user button
        MaterialButton btnAddUser = view.findViewById(R.id.btnAddUser);
        btnAddUser.setOnClickListener(v -> showAddUserDialog());
    }

    private void showLoading() {
        layoutLoading.setVisibility(View.VISIBLE);
        recyclerViewEmployees.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    private void showContent() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewEmployees.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        layoutLoading.setVisibility(View.GONE);
        recyclerViewEmployees.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
    }

    private void showAddUserDialog() {
        UserEditDialog dialog = new UserEditDialog(requireContext(), null);
        dialog.setOnUserSavedListener(user -> {
            // Save user to Firestore
            viewModel.saveUser(user);
        });
        dialog.show();
    }

    private void showEditUserDialog(User user) {
        UserEditDialog dialog = new UserEditDialog(requireContext(), user);
        dialog.setOnUserSavedListener(updatedUser -> {
            // Update user in Firestore
            viewModel.saveUser(updatedUser);
        });
        dialog.show();
    }

    private void showImportDialog() {
        CsvImportDialog dialog = new CsvImportDialog(requireContext());
        dialog.setOnImportCompletedListener((successCount, failCount) -> {
            // Refresh employees after import
            Toast.makeText(requireContext(), 
                    "Import completed: " + successCount + " successful, " + failCount + " failed", 
                    Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void exportUsers() {
        // TODO: Implement CSV export functionality
        Toast.makeText(requireContext(), "Export functionality will be implemented soon", Toast.LENGTH_SHORT).show();
    }

    private void manageDepartments() {
        // TODO: Show department management dialog or navigate to department screen
        Toast.makeText(requireContext(), "Department management will be implemented soon", Toast.LENGTH_SHORT).show();
    }

    // Add theme-oriented methods
    private void showPermissionsDialog(User user) {
        // TODO: Show permissions dialog that adapts to theme
        Toast.makeText(requireContext(), "Permissions management will be implemented soon", Toast.LENGTH_SHORT).show();
    }

    private void navigateToMetricsScreen(User user) {
        // TODO: Navigate with theme-aware transition
        Toast.makeText(requireContext(), "User metrics view will be implemented soon", Toast.LENGTH_SHORT).show();
    }
} 