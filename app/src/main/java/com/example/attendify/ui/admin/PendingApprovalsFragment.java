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

import com.example.attendify.databinding.FragmentPendingApprovalsBinding;
import com.example.attendify.model.User;
import com.example.attendify.viewmodel.AdminDashboardViewModel;

public class PendingApprovalsFragment extends Fragment implements PendingApprovalsAdapter.OnPendingApprovalListener {

    private FragmentPendingApprovalsBinding binding;
    private AdminDashboardViewModel viewModel;
    private PendingApprovalsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPendingApprovalsBinding.inflate(inflater, container, false);
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

        // Load data
        viewModel.loadPendingUsers();
    }

    private void setupRecyclerView() {
        adapter = new PendingApprovalsAdapter(this);
        binding.pendingRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.pendingRecyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        // Observe pending users
        viewModel.getPendingUsersLiveData().observe(getViewLifecycleOwner(), users -> {
            if (users.isEmpty()) {
                binding.emptyText.setVisibility(View.VISIBLE);
                binding.pendingRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyText.setVisibility(View.GONE);
                binding.pendingRecyclerView.setVisibility(View.VISIBLE);
                adapter.submitList(users);
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

    @Override
    public void onApprove(User user) {
        viewModel.approveUser(user.getUid());
    }

    @Override
    public void onReject(User user) {
        viewModel.rejectUser(user.getUid());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}