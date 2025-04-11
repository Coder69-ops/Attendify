package com.example.attendify.ui.common;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.example.attendify.R;
import com.example.attendify.databinding.DialogLocationPickerBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.slider.Slider;

public class LocationPickerDialog extends DialogFragment implements OnMapReadyCallback {

    public interface LocationPickerListener {
        void onLocationPicked(double latitude, double longitude, float radius);
    }

    private DialogLocationPickerBinding binding;
    private GoogleMap googleMap;
    private LatLng selectedLocation;
    private float selectedRadius = 100f;
    private Marker locationMarker;
    private LocationPickerListener listener;
    private FusedLocationProviderClient fusedLocationClient;
    private int primaryColor;
    private int primaryColorTransparent;

    public static LocationPickerDialog newInstance() {
        return new LocationPickerDialog();
    }

    public void setLocationPickerListener(LocationPickerListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_Attendify_FullScreenDialog);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        // Get theme colors using TypedValue
        TypedValue typedValue = new TypedValue();
        requireContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
        primaryColor = typedValue.data;
        
        // Create semi-transparent version of primary color
        primaryColorTransparent = (primaryColor & 0x00FFFFFF) | 0x40000000;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogLocationPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapView);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Apply theme styling
        binding.titleTextView.setTextAppearance(R.style.TextAppearance_Attendify_Headline2);
        binding.radiusTextView.setTextAppearance(R.style.TextAppearance_Attendify_Body1);
        binding.radiusInstructionTextView.setTextAppearance(R.style.TextAppearance_Attendify_Body2);
        
        // Update slider colors
        binding.radiusSlider.setTrackActiveTintList(requireContext().getColorStateList(R.color.primary));
        binding.radiusSlider.setTrackInactiveTintList(requireContext().getColorStateList(R.color.gray_300));
        binding.radiusSlider.setThumbTintList(requireContext().getColorStateList(R.color.primary));
        
        // Setup buttons with themed styles
        binding.confirmButton.setBackgroundColor(primaryColor);
        binding.confirmButton.setTextColor(getResources().getColor(R.color.on_primary, null));
        
        // Set up UI components
        setupUI();
    }

    private void setupUI() {
        // Set up radius slider
        binding.radiusSlider.addOnChangeListener((slider, value, fromUser) -> {
            selectedRadius = value;
            updateRadiusText();
            updateCircleOnMap();
        });

        // Current location button
        binding.currentLocationButton.setOnClickListener(v -> getCurrentLocation());

        // Cancel and confirm buttons
        binding.cancelButton.setOnClickListener(v -> dismiss());
        binding.confirmButton.setOnClickListener(v -> {
            if (selectedLocation != null && listener != null) {
                listener.onLocationPicked(
                        selectedLocation.latitude,
                        selectedLocation.longitude,
                        selectedRadius
                );
                dismiss();
            } else {
                Toast.makeText(requireContext(), "Please select a location first", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set initial radius text
        updateRadiusText();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        try {
            googleMap = map;
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setCompassEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(false);

            // Set default location to user's current location
            getCurrentLocation();

            // Set up map click listener
            googleMap.setOnMapClickListener(latLng -> {
                try {
                    selectedLocation = latLng;
                    updateMarkerOnMap();
                    updateCircleOnMap();
                } catch (Exception e) {
                    Log.e("LocationPickerDialog", "Error handling map click: " + e.getMessage());
                    Toast.makeText(requireContext(), "Error selecting location", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e("LocationPickerDialog", "Error initializing Google Map: " + e.getMessage());
            Toast.makeText(requireContext(), "Could not initialize map. Please try again.", Toast.LENGTH_SHORT).show();
            dismiss(); // Close the dialog if map can't be initialized
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                
                // Try to enable my location button safely
                try {
                    googleMap.setMyLocationEnabled(true);
                } catch (Exception e) {
                    Log.w("LocationPickerDialog", "Could not enable my location layer: " + e.getMessage());
                }
                
                fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        try {
                            if (location != null) {
                                selectedLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15f));
                                updateMarkerOnMap();
                                updateCircleOnMap();
                            } else {
                                // If no location available, use a default location (optional)
                                Log.w("LocationPickerDialog", "No last location available");
                                Toast.makeText(requireContext(), "Could not get current location", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("LocationPickerDialog", "Error processing location: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LocationPickerDialog", "Failed to get location: " + e.getMessage());
                        Toast.makeText(requireContext(), "Location services unavailable", Toast.LENGTH_SHORT).show();
                    });
            } else {
                Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("LocationPickerDialog", "Error getting current location: " + e.getMessage());
        }
    }

    private void updateMarkerOnMap() {
        try {
            if (googleMap != null && selectedLocation != null) {
                if (locationMarker != null) {
                    locationMarker.remove();
                }
                locationMarker = googleMap.addMarker(new MarkerOptions()
                        .position(selectedLocation)
                        .title("Selected Location"));
            }
        } catch (Exception e) {
            Log.e("LocationPickerDialog", "Error updating map marker: " + e.getMessage());
        }
    }

    private void updateCircleOnMap() {
        try {
            if (googleMap != null && selectedLocation != null) {
                googleMap.clear();
                updateMarkerOnMap();
                
                googleMap.addCircle(new CircleOptions()
                        .center(selectedLocation)
                        .radius(selectedRadius)
                        .strokeWidth(2)
                        .strokeColor(primaryColor)
                        .fillColor(primaryColorTransparent));
            }
        } catch (Exception e) {
            Log.e("LocationPickerDialog", "Error updating circle on map: " + e.getMessage());
        }
    }

    private void updateRadiusText() {
        binding.radiusTextView.setText(String.format("%.0f meters", selectedRadius));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 