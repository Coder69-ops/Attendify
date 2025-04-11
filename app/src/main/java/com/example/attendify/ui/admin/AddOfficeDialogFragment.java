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

import com.example.attendify.databinding.DialogEditOfficeBinding;
import com.example.attendify.model.Office;
import com.example.attendify.ui.common.LocationPickerDialog;
import com.example.attendify.viewmodel.OfficeViewModel;

import java.util.Calendar;

public class AddOfficeDialogFragment extends DialogFragment {

    private DialogEditOfficeBinding binding;
    private OfficeViewModel officeViewModel;
    private double selectedLatitude;
    private double selectedLongitude;
    private double selectedRadius = 100;

    public static AddOfficeDialogFragment newInstance() {
        return new AddOfficeDialogFragment();
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
            dialog.setTitle("Add New Office");
        }

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

            dismiss();
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 