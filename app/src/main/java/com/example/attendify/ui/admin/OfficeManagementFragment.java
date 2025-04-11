package com.example.attendify.ui.admin;

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

import com.example.attendify.databinding.FragmentOfficeManagementBinding;
import com.example.attendify.model.Office;
import com.example.attendify.viewmodel.AdminDashboardViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

public class OfficeManagementFragment extends Fragment {

    private FragmentOfficeManagementBinding binding;
    private AdminDashboardViewModel viewModel;
    private OfficeAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOfficeManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(AdminDashboardViewModel.class);

        // Set up RecyclerView
        setupRecyclerView();

        // Set up observers
        setupObservers();

        // Set up FAB
        binding.addOfficeFab.setOnClickListener(v -> handleAddOffice());

        // Load data
        viewModel.loadOffices();
    }

    private void setupRecyclerView() {
        adapter = new OfficeAdapter(this::handleEditOffice);
        binding.officeRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.officeRecyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        // Observe offices
        viewModel.getOfficesLiveData().observe(getViewLifecycleOwner(), offices -> {
            if (offices.isEmpty()) {
                binding.emptyText.setVisibility(View.VISIBLE);
                binding.officeRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyText.setVisibility(View.GONE);
                binding.officeRecyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(offices);
            }
        });

        // Observe loading state
        viewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), isLoading ->
                binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE));

        // Observe errors
        viewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.resetError();
            }
        });
    }

    private void handleEditOffice(Office office) {
        EditOfficeDialogFragment.newInstance(office)
                .show(getChildFragmentManager(), "edit_office");
    }

    public void handleAddOffice() {
        AddOfficeDialogFragment.newInstance()
                .show(getChildFragmentManager(), "add_office");
    }

    /**
     * Public method to refresh the office list
     * Called by the parent activity when a new office is added
     */
    public void refreshOffices() {
        viewModel.loadOffices();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}