package com.example.attendify.ui.employee;

import android.graphics.Typeface;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.R;
import com.example.attendify.model.Office;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OfficeSelectionAdapter extends RecyclerView.Adapter<OfficeSelectionAdapter.OfficeViewHolder> {

    private static final String TAG = "OfficeSelectionAdapter";
    private List<Office> offices = new ArrayList<>();
    private Office selectedOffice;
    private Location currentLocation;
    private int selectedPosition = -1;
    private OnOfficeSelectedListener listener;

    public interface OnOfficeSelectedListener {
        void onOfficeSelected(Office office);
    }

    public OfficeSelectionAdapter(OnOfficeSelectedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfficeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "Creating view holder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_office_selection, parent, false);
        return new OfficeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfficeViewHolder holder, int position) {
        Office office = offices.get(position);
        Log.d(TAG, "Binding office at position: " + position + 
                ", name: " + office.getName() + 
                ", selected: " + (position == selectedPosition));
        
        holder.bind(office, position == selectedPosition);
        
        // Using final int variable to use in click listeners
        final int currentPosition = position;
        
        // Make entire view clickable
        holder.itemView.setOnClickListener(v -> {
            selectOffice(currentPosition);
        });
    }

    private void selectOffice(int position) {
        if (position == selectedPosition) {
            return; // Already selected
        }
        
        int previousSelected = selectedPosition;
        selectedPosition = position;
        selectedOffice = offices.get(position);
        
        // Update UI for the affected items
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
        notifyItemChanged(selectedPosition);
        
        // Notify listener
        if (listener != null) {
            listener.onOfficeSelected(selectedOffice);
            
            // Auto-confirm after a short delay (300ms)
            new Handler().postDelayed(() -> {
                if (listener instanceof OfficeSelectionDialogFragment) {
                    ((OfficeSelectionDialogFragment) listener).confirmSelection();
                }
            }, 300);
        }
        
        Log.d(TAG, "Selected office: " + selectedOffice.getName() + " at position " + selectedPosition);
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "getItemCount: " + offices.size());
        return offices.size();
    }

    public void updateOffices(List<Office> offices) {
        Log.d(TAG, "Updating offices list: " + (offices != null ? offices.size() : 0) + " items");
        if (offices == null) {
            this.offices = new ArrayList<>();
        } else {
            this.offices = new ArrayList<>(offices);
            for (Office office : offices) {
                Log.d(TAG, "Office in adapter: " + office.getName() + ", id: " + office.getId());
            }
        }
        selectedPosition = -1;
        selectedOffice = null;
        notifyDataSetChanged();
    }
    
    public void updateCurrentLocation(Location location) {
        this.currentLocation = location;
        notifyDataSetChanged();
    }
    
    public Office getSelectedOffice() {
        return selectedOffice;
    }
    
    public void setSelectedOffice(Office office) {
        if (office == null) {
            selectedPosition = -1;
            selectedOffice = null;
            notifyDataSetChanged();
            return;
        }
        
        for (int i = 0; i < offices.size(); i++) {
            if (offices.get(i).getId().equals(office.getId())) {
                selectedPosition = i;
                selectedOffice = office;
                notifyDataSetChanged();
                Log.d(TAG, "Selected office position: " + selectedPosition);
                return;
            }
        }
    }

    class OfficeViewHolder extends RecyclerView.ViewHolder {
        private final RadioButton radioButton;
        private final TextView nameText;
        private final TextView addressText;
        private final TextView distanceText;

        public OfficeViewHolder(@NonNull View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.officeRadioButton);
            nameText = itemView.findViewById(R.id.officeNameText);
            addressText = itemView.findViewById(R.id.officeAddressText);
            distanceText = itemView.findViewById(R.id.officeDistanceText);
        }

        public void bind(Office office, boolean isSelected) {
            Log.d(TAG, "Binding office: " + office.getName() + ", id: " + office.getId() + ", selected: " + isSelected);
            nameText.setText(office.getName());
            addressText.setText(office.getAddress());
            radioButton.setChecked(isSelected);
            
            // Apply selected style
            if (isSelected) {
                itemView.setBackgroundResource(R.color.selected_item_background);
                // Make the card elevate more when selected
                if (itemView.getParent() instanceof androidx.cardview.widget.CardView) {
                    ((androidx.cardview.widget.CardView) itemView.getParent()).setCardElevation(8f);
                    ((androidx.cardview.widget.CardView) itemView.getParent()).setCardBackgroundColor(
                            itemView.getContext().getResources().getColor(R.color.selected_item_background)
                    );
                }
            } else {
                // Reset to default
                if (itemView.getParent() instanceof androidx.cardview.widget.CardView) {
                    ((androidx.cardview.widget.CardView) itemView.getParent()).setCardElevation(4f);
                    ((androidx.cardview.widget.CardView) itemView.getParent()).setCardBackgroundColor(
                            itemView.getContext().getResources().getColor(android.R.color.white)
                    );
                }
            }
            
            // Calculate distance if current location is available
            if (currentLocation != null) {
                float[] results = new float[1];
                Location.distanceBetween(
                        currentLocation.getLatitude(), 
                        currentLocation.getLongitude(),
                        office.getLatitude(), 
                        office.getLongitude(), 
                        results);
                
                float distanceInMeters = results[0];
                
                if (distanceInMeters < 1000) {
                    distanceText.setText(String.format(Locale.getDefault(), "%.0f meters away", distanceInMeters));
                } else {
                    distanceText.setText(String.format(Locale.getDefault(), "%.1f km away", distanceInMeters / 1000));
                }
                distanceText.setVisibility(View.VISIBLE);
            } else {
                distanceText.setText("Distance unavailable");
                distanceText.setVisibility(View.VISIBLE);
            }
        }
    }
} 