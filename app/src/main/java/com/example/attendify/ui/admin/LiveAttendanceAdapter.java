package com.example.attendify.ui.admin;

import android.content.res.ColorStateList;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.R;
import com.example.attendify.databinding.ItemLiveAttendanceBinding;
import com.example.attendify.model.Attendance;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class LiveAttendanceAdapter extends ListAdapter<Attendance, LiveAttendanceAdapter.ViewHolder> {

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public LiveAttendanceAdapter() {
        super(new DiffCallback());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLiveAttendanceBinding binding = ItemLiveAttendanceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance attendance = getItem(position);
        holder.bind(attendance);
    }

    static class DiffCallback extends DiffUtil.ItemCallback<Attendance> {
        @Override
        public boolean areItemsTheSame(@NonNull Attendance oldItem, @NonNull Attendance newItem) {
            return oldItem.getUserId().equals(newItem.getUserId()) && 
                   oldItem.getDate().equals(newItem.getDate());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Attendance oldItem, @NonNull Attendance newItem) {
            // Check if any important fields have changed
            return oldItem.getCheckInTime().equals(newItem.getCheckInTime()) &&
                   (oldItem.getCheckOutTime() == null ? newItem.getCheckOutTime() == null :
                   oldItem.getCheckOutTime().equals(newItem.getCheckOutTime())) &&
                   oldItem.getStatus().equals(newItem.getStatus()) &&
                   oldItem.getLocationStatus().equals(newItem.getLocationStatus());
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLiveAttendanceBinding binding;

        ViewHolder(ItemLiveAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Attendance attendance) {
            // Set user info
            binding.nameText.setText(attendance.getUserName());
            binding.officeText.setText(attendance.getOfficeName());
            binding.checkInText.setText("Checked in: " + timeFormat.format(attendance.getCheckInTime()));

            // Set check-out time if available
            if (attendance.getCheckOutTime() != null) {
                binding.checkOutText.setText("Checked out: " + timeFormat.format(attendance.getCheckOutTime()));
                binding.checkOutText.setVisibility(View.VISIBLE);
                
                // Hide location status for checked-out employees
                binding.locationStatusText.setVisibility(View.GONE);
            } else {
                binding.checkOutText.setVisibility(View.GONE);
                
                // Show location status for employees who are still checked in
                binding.locationStatusText.setVisibility(View.VISIBLE);
                
                // Set location status text and color
                setLocationStatus(attendance);
            }

            // Set status chip
            binding.statusChip.setText(attendance.getStatus());
            int statusColor = getStatusColor(attendance.getStatus());
            binding.statusChip.setChipBackgroundColor(ColorStateList.valueOf(
                    ContextCompat.getColor(binding.getRoot().getContext(), statusColor)));
            binding.statusChip.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(), android.R.color.white));
        }
        
        private void setLocationStatus(Attendance attendance) {
            String locationStatus = attendance.getLocationStatus();
            if (locationStatus == null || locationStatus.equals("Unknown")) {
                binding.locationStatusText.setText("Location: Unknown");
                binding.locationStatusText.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_missed));
                binding.locationStatusIcon.setImageResource(R.drawable.ic_location_unknown);
                binding.locationStatusIcon.setColorFilter(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_missed));
            } else if (locationStatus.equals("InOffice")) {
                binding.locationStatusText.setText("In Office");
                binding.locationStatusText.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_on_time));
                binding.locationStatusIcon.setImageResource(R.drawable.ic_location_in_office);
                binding.locationStatusIcon.setColorFilter(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_on_time));
            } else {
                binding.locationStatusText.setText("Out of Office");
                binding.locationStatusText.setTextColor(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_late));
                binding.locationStatusIcon.setImageResource(R.drawable.ic_location_out_office);
                binding.locationStatusIcon.setColorFilter(
                        ContextCompat.getColor(binding.getRoot().getContext(), R.color.status_late));
            }
        }

        private int getStatusColor(String status) {
            switch (status) {
                case "OnTime":
                    return R.color.status_on_time;
                case "Late":
                    return R.color.status_late;
                default:
                    return R.color.status_missed;
            }
        }
    }
}