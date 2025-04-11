package com.example.attendify.ui.employee;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.R;
import com.example.attendify.model.Office;

import java.util.List;

public class OfficeSelectionDialogFragment extends DialogFragment implements OfficeSelectionAdapter.OnOfficeSelectedListener {

    private static final String TAG = "OfficeSelectionDialog";
    private RecyclerView recyclerView;
    private Button cancelButton;
    private Button confirmButton;
    private TextView emptyStateText;
    private OfficeSelectionAdapter adapter;
    private OnOfficeSelectedListener listener;
    private List<Office> offices;
    private Office initialSelectedOffice;
    private Location currentLocation;

    public interface OnOfficeSelectedListener {
        void onOfficeSelected(Office office);
    }

    public static OfficeSelectionDialogFragment newInstance(List<Office> offices, @Nullable Office selectedOffice, @Nullable Location currentLocation) {
        OfficeSelectionDialogFragment fragment = new OfficeSelectionDialogFragment();
        fragment.offices = offices;
        fragment.initialSelectedOffice = selectedOffice;
        fragment.currentLocation = currentLocation;
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnOfficeSelectedListener) {
            listener = (OnOfficeSelectedListener) context;
        } else {
            throw new RuntimeException(context + " must implement OnOfficeSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_office_selection, container, false);
        view.setBackgroundColor(Color.WHITE); // Ensure background is white
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.officeRecyclerView);
        cancelButton = view.findViewById(R.id.cancelButton);
        confirmButton = view.findViewById(R.id.confirmButton);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        
        TextView subtitleView = view.findViewById(R.id.officeDialogSubtitle);

        // Log office count
        Log.d(TAG, "Office count: " + (offices != null ? offices.size() : 0));
        if (offices != null) {
            for (Office office : offices) {
                Log.d(TAG, "Office: " + office.getName() + " / " + office.getId());
            }
        }

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OfficeSelectionAdapter(this);
        recyclerView.setAdapter(adapter);

        // Show empty state if needed
        if (offices == null || offices.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
            subtitleView.setVisibility(View.GONE);
            Toast.makeText(getContext(), "No offices available to select", Toast.LENGTH_SHORT).show();
            confirmButton.setEnabled(false);
        } else {
            // If we have a small number of offices, show a helpful message
            if (offices.size() <= 2) {
                subtitleView.setText("Select one of the " + offices.size() + " available offices");
            }
            
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            
            // Update adapter with data
            adapter.updateOffices(offices);
            
            // Set initial selected office if available
            if (initialSelectedOffice != null) {
                adapter.setSelectedOffice(initialSelectedOffice);
            }
            
            // Set current location if available
            if (currentLocation != null) {
                adapter.updateCurrentLocation(currentLocation);
            }
        }

        // Set up buttons
        cancelButton.setOnClickListener(v -> dismiss());
        confirmButton.setOnClickListener(v -> {
            Office selectedOffice = adapter.getSelectedOffice();
            if (selectedOffice != null && listener != null) {
                Log.d(TAG, "Confirming selection of office: " + selectedOffice.getName());
                listener.onOfficeSelected(selectedOffice);
                dismiss();
            } else {
                Toast.makeText(getContext(), "Please select an office", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Initially disable confirm button if no office is selected
        updateConfirmButtonState();
    }
    
    private void updateConfirmButtonState() {
        if (adapter != null) {
            confirmButton.setEnabled(adapter.getSelectedOffice() != null);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        
        // Set the dialog size to almost full width
        if (getDialog() != null && getDialog().getWindow() != null) {
            Dialog dialog = getDialog();
            Window window = dialog.getWindow();
            
            window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            );
            
            // Set background to a rectangle with rounded corners
            window.setBackgroundDrawableResource(android.R.color.white);
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_Dialog;
            
            // Log that dialog is showing with proper size
            Log.d(TAG, "Dialog started with width: MATCH_PARENT, height: WRAP_CONTENT");
        } else {
            Log.e(TAG, "Dialog window is null");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Set this to false to make the dialog show properly
        setCancelable(true);
        
        return dialog;
    }

    @Override
    public void onOfficeSelected(Office office) {
        updateConfirmButtonState();
    }

    /**
     * Programmatically trigger confirmation of the selected office
     */
    public void confirmSelection() {
        Office selectedOffice = adapter.getSelectedOffice();
        if (selectedOffice != null && listener != null) {
            Log.d(TAG, "Auto-confirming selection of office: " + selectedOffice.getName());
            listener.onOfficeSelected(selectedOffice);
            dismiss();
        }
    }
} 