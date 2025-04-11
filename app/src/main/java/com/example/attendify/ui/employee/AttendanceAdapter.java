package com.example.attendify.ui.employee;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.R;
import com.example.attendify.model.Attendance;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder> {
    
    private List<Attendance> attendanceList = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    
    @NonNull
    @Override
    public AttendanceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new AttendanceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AttendanceViewHolder holder, int position) {
        Attendance attendance = attendanceList.get(position);
        
        // Set date
        if (attendance.getDate() != null) {
            holder.dateText.setText(attendance.getDate());
        }
        
        // Set office name
        String officeName = attendance.getOfficeName();
        if (officeName != null) {
            holder.officeNameText.setText(officeName);
        } else {
            holder.officeNameText.setText("Unknown Office");
        }
        
        // Set check-in time
        if (attendance.getCheckInTime() != null) {
            holder.checkInTimeText.setText("Check-in: " + timeFormat.format(attendance.getCheckInTime()));
        } else {
            holder.checkInTimeText.setText("Check-in: N/A");
        }
        
        // Set check-out time
        if (attendance.getCheckOutTime() != null) {
            holder.checkOutTimeText.setText("Check-out: " + timeFormat.format(attendance.getCheckOutTime()));
            holder.checkOutTimeText.setVisibility(View.VISIBLE);
        } else {
            holder.checkOutTimeText.setVisibility(View.GONE);
        }
        
        // Set status text
        String status = attendance.getStatus();
        if (status != null) {
            holder.statusText.setText(status);
            
            // Set status background color
            int colorId;
            switch (status) {
                case "OnTime":
                    colorId = R.color.success;
                    break;
                case "Late":
                    colorId = R.color.warning;
                    break;
                default:
                    colorId = R.color.error;
                    break;
            }
            holder.statusText.setBackgroundResource(colorId);
        } else {
            holder.statusText.setText("Unknown");
        }
    }
    
    @Override
    public int getItemCount() {
        return attendanceList.size();
    }
    
    public void updateAttendanceList(List<Attendance> newList) {
        this.attendanceList = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    static class AttendanceViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView attendanceCard;
        final TextView dateText;
        final TextView statusText;
        final TextView officeNameText;
        final TextView checkInTimeText;
        final TextView checkOutTimeText;
        
        AttendanceViewHolder(View itemView) {
            super(itemView);
            attendanceCard = itemView.findViewById(R.id.attendanceCard);
            dateText = itemView.findViewById(R.id.dateText);
            statusText = itemView.findViewById(R.id.statusText);
            officeNameText = itemView.findViewById(R.id.officeNameText);
            checkInTimeText = itemView.findViewById(R.id.checkInTimeText);
            checkOutTimeText = itemView.findViewById(R.id.checkOutTimeText);
        }
    }
}