package com.example.attendify.ui.admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.R;
import com.example.attendify.model.Attendance;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Locale;

import androidx.core.content.ContextCompat;

public class ReportAdapter extends ListAdapter<Attendance, ReportAdapter.ViewHolder> {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault());

    public ReportAdapter() {
        super(new AttendanceDiffCallback());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance attendance = getItem(position);
        
        // Set user and office info
        holder.userNameTextView.setText(attendance.getUserName());
        holder.officeNameTextView.setText(attendance.getOfficeName());
        
        // Set date - add null check
        if (attendance.getCheckInTime() != null) {
            holder.dateTextView.setText(DATE_FORMAT.format(attendance.getCheckInTime()));
            // Set check-in time
            holder.checkInTimeTextView.setText(TIME_FORMAT.format(attendance.getCheckInTime()));
        } else {
            holder.dateTextView.setText("N/A");
            holder.checkInTimeTextView.setText("N/A");
        }
        
        // Set check-out time (if available)
        if (attendance.getCheckOutTime() != null) {
            holder.checkOutTimeTextView.setText(TIME_FORMAT.format(attendance.getCheckOutTime()));
            holder.checkOutTimeTextView.setVisibility(View.VISIBLE);
            holder.checkOutLabelTextView.setVisibility(View.VISIBLE);
        } else {
            holder.checkOutTimeTextView.setVisibility(View.GONE);
            holder.checkOutLabelTextView.setVisibility(View.GONE);
        }
        
        // Set status chip
        holder.statusChip.setText(attendance.getStatus());
        
        int chipBackgroundColor;
        int chipTextColor = android.R.color.white;
        
        switch (attendance.getStatus()) {
            case "OnTime":
                chipBackgroundColor = R.color.status_on_time;
                break;
            case "Late":
                chipBackgroundColor = R.color.status_late;
                break;
            case "Missed":
                chipBackgroundColor = R.color.status_missed;
                break;
            default:
                chipBackgroundColor = R.color.status_unknown;
                break;
        }
        
        holder.statusChip.setChipBackgroundColorResource(chipBackgroundColor);
        holder.statusChip.setTextColor(holder.itemView.getContext().getResources().getColor(chipTextColor));
        
        // Set card color based on status
        int cardStrokeColor;
        
        switch (attendance.getStatus()) {
            case "OnTime":
                cardStrokeColor = R.color.status_on_time;
                break;
            case "Late":
                cardStrokeColor = R.color.status_late;
                break;
            case "Missed":
                cardStrokeColor = R.color.status_missed;
                break;
            default:
                cardStrokeColor = R.color.status_unknown;
                break;
        }
        
        holder.cardView.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), cardStrokeColor));
        
        // Set location status if available
        if (attendance.getLocationStatus() != null) {
            holder.locationStatusChip.setVisibility(View.VISIBLE);
            
            switch (attendance.getLocationStatus()) {
                case "InOffice":
                    holder.locationStatusChip.setText("In Office");
                    holder.locationStatusChip.setChipBackgroundColorResource(R.color.location_in_office);
                    holder.locationStatusChip.setChipIconResource(R.drawable.ic_location_in_office);
                    break;
                case "OutOfOffice":
                    holder.locationStatusChip.setText("Out of Office");
                    holder.locationStatusChip.setChipBackgroundColorResource(R.color.location_out_office);
                    holder.locationStatusChip.setChipIconResource(R.drawable.ic_location_out_office);
                    break;
                default:
                    holder.locationStatusChip.setText("Unknown");
                    holder.locationStatusChip.setChipBackgroundColorResource(R.color.location_unknown);
                    holder.locationStatusChip.setChipIconResource(R.drawable.ic_location_unknown);
                    break;
            }
        } else {
            holder.locationStatusChip.setVisibility(View.GONE);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView userNameTextView;
        private final TextView officeNameTextView;
        private final TextView dateTextView;
        private final TextView checkInTimeTextView;
        private final TextView checkOutTimeTextView;
        private final TextView checkOutLabelTextView;
        private final Chip statusChip;
        private final Chip locationStatusChip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            userNameTextView = itemView.findViewById(R.id.userNameTextView);
            officeNameTextView = itemView.findViewById(R.id.officeNameTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            checkInTimeTextView = itemView.findViewById(R.id.checkInTimeTextView);
            checkOutTimeTextView = itemView.findViewById(R.id.checkOutTimeTextView);
            checkOutLabelTextView = itemView.findViewById(R.id.checkOutLabelTextView);
            statusChip = itemView.findViewById(R.id.statusChip);
            locationStatusChip = itemView.findViewById(R.id.locationStatusChip);
        }
    }

    private static class AttendanceDiffCallback extends DiffUtil.ItemCallback<Attendance> {
        @Override
        public boolean areItemsTheSame(@NonNull Attendance oldItem, @NonNull Attendance newItem) {
            // Add null safety for userId and date comparison
            if (oldItem.getUserId() == null || newItem.getUserId() == null || 
                oldItem.getDate() == null || newItem.getDate() == null) {
                return false;
            }
            
            // Only compare checkInTime if both are non-null
            if (oldItem.getCheckInTime() == null || newItem.getCheckInTime() == null) {
                return oldItem.getUserId().equals(newItem.getUserId()) && 
                       oldItem.getDate().equals(newItem.getDate());
            }
            
            return oldItem.getUserId().equals(newItem.getUserId()) && 
                   oldItem.getDate().equals(newItem.getDate()) && 
                   oldItem.getCheckInTime().equals(newItem.getCheckInTime());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Attendance oldItem, @NonNull Attendance newItem) {
            return oldItem.equals(newItem);
        }
    }
} 