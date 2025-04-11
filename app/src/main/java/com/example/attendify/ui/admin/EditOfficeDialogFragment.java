package com.example.attendify.ui.admin;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AlertDialog;

import com.example.attendify.databinding.DialogEditOfficeBinding;
import com.example.attendify.model.Office;
import com.example.attendify.ui.common.LocationPickerDialog;
import com.example.attendify.viewmodel.OfficeViewModel;
import com.google.android.material.R;

import java.util.Calendar;

public class EditOfficeDialogFragment extends DialogFragment {

    private static final String ARG_OFFICE = "office";
    private DialogEditOfficeBinding binding;
    private OfficeViewModel officeViewModel;
    private Office currentOffice;
    private double selectedLatitude;
    private double selectedLongitude;
    private double selectedRadius = 100;

    public static EditOfficeDialogFragment newInstance(@Nullable Office office) {
        EditOfficeDialogFragment fragment = new EditOfficeDialogFragment();
        if (office != null) {
            Bundle args = new Bundle();
            args.putParcelable(ARG_OFFICE, office);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogEditOfficeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        officeViewModel = new ViewModelProvider(requireActivity()).get(OfficeViewModel.class);

        // Set up the dialog
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // Get current office if editing
        if (getArguments() != null) {
            currentOffice = getArguments().getParcelable(ARG_OFFICE);
            if (currentOffice != null) {
                populateFields(currentOffice);
                selectedLatitude = currentOffice.getLatitude();
                selectedLongitude = currentOffice.getLongitude();
                selectedRadius = currentOffice.getRadius();
            }
        }
        
        // Show or hide delete button based on whether we're editing or creating
        binding.deleteButton.setVisibility(currentOffice != null ? View.VISIBLE : View.GONE);

        // Set up click listeners
        setupClickListeners();

        // Set up observers
        setupObservers();
    }

    private void setupClickListeners() {
        binding.entryTimeEditText.setOnClickListener(v -> showTimePickerDialog());
        binding.selectLocationButton.setOnClickListener(v -> showLocationPicker());
        binding.saveButton.setOnClickListener(v -> handleSave());
        binding.cancelButton.setOnClickListener(v -> dismiss());
        binding.deleteButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void setupObservers() {
        officeViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                officeViewModel.resetError();
            }
        });

        officeViewModel.getLoadingLiveData().observe(getViewLifecycleOwner(), isLoading -> {
            binding.saveButton.setEnabled(!isLoading);
            binding.cancelButton.setEnabled(!isLoading);
        });
    }

    private void populateFields(Office office) {
        binding.nameEditText.setText(office.getName());
        binding.latitudeEditText.setText(String.valueOf(office.getLatitude()));
        binding.longitudeEditText.setText(String.valueOf(office.getLongitude()));
        binding.radiusEditText.setText(String.valueOf((int) office.getRadius()));
        binding.entryTimeEditText.setText(office.getEntryTime());
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute1) -> {
                    String time = String.format("%02d:%02d", hourOfDay, minute1);
                    binding.entryTimeEditText.setText(time);
                },
                hour,
                minute,
                true
        );

        timePickerDialog.show();
    }

    private void showLocationPicker() {
        LocationPickerDialog locationPicker = LocationPickerDialog.newInstance();
        locationPicker.setLocationPickerListener((latitude, longitude, radius) -> {
            // Update the UI with selected location
            selectedLatitude = latitude;
            selectedLongitude = longitude;
            selectedRadius = radius;
            
            binding.latitudeEditText.setText(String.valueOf(latitude));
            binding.longitudeEditText.setText(String.valueOf(longitude));
            binding.radiusEditText.setText(String.valueOf((int) radius));
            
            // Show a confirmation message
            Toast.makeText(requireContext(), "Location selected successfully", Toast.LENGTH_SHORT).show();
        });
        locationPicker.show(getParentFragmentManager(), "location_picker");
    }

    private void handleSave() {
        String name = binding.nameEditText.getText().toString().trim();
        String latitudeStr = binding.latitudeEditText.getText().toString().trim();
        String longitudeStr = binding.longitudeEditText.getText().toString().trim();
        String radiusStr = binding.radiusEditText.getText().toString().trim();
        String entryTime = binding.entryTimeEditText.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            binding.nameLayout.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(latitudeStr)) {
            binding.latitudeLayout.setError("Latitude is required");
            return;
        }

        if (TextUtils.isEmpty(longitudeStr)) {
            binding.longitudeLayout.setError("Longitude is required");
            return;
        }

        if (TextUtils.isEmpty(radiusStr)) {
            binding.radiusLayout.setError("Radius is required");
            return;
        }

        if (TextUtils.isEmpty(entryTime)) {
            binding.entryTimeLayout.setError("Entry time is required");
            return;
        }

        try {
            double latitude = Double.parseDouble(latitudeStr);
            double longitude = Double.parseDouble(longitudeStr);
            double radius = Double.parseDouble(radiusStr);

            // Clear errors
            binding.nameLayout.setError(null);
            binding.latitudeLayout.setError(null);
            binding.longitudeLayout.setError(null);
            binding.radiusLayout.setError(null);
            binding.entryTimeLayout.setError(null);

            // Create or update office
            if (currentOffice != null) {
                // Update existing office
                Office updatedOffice = new Office(
                        currentOffice.getId(),
                        name,
                        latitude,
                        longitude,
                        radius,
                        entryTime
                );
                officeViewModel.updateOffice(updatedOffice);
            } else {
                // Create new office
                Office newOffice = new Office(
                        null,
                        name,
                        latitude,
                        longitude,
                        radius,
                        entryTime
                );
                officeViewModel.addOffice(newOffice);
            }

            dismiss();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Delete Office");
        builder.setMessage("Are you sure you want to delete this office? This action cannot be undone.");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            if (currentOffice != null && currentOffice.getId() != null) {
                officeViewModel.deleteOffice(currentOffice.getId());
                Toast.makeText(requireContext(), "Office deleted", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}