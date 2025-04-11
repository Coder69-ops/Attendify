package com.example.attendify.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.R;
import com.example.attendify.databinding.ItemRecentAttendanceBinding;
import com.example.attendify.model.Attendance;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class RecentAttendanceAdapter extends ListAdapter<Attendance, RecentAttendanceAdapter.ViewHolder> {

    private final SimpleDateFormat dateFormat;
    private final SimpleDateFormat timeFormat;

    public RecentAttendanceAdapter() {
        super(new DiffUtil.ItemCallback<Attendance>() {
            @Override
            public boolean areItemsTheSame(@NonNull Attendance oldItem, @NonNull Attendance newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Attendance oldItem, @NonNull Attendance newItem) {
                return oldItem.equals(newItem);
            }
        });
        dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRecentAttendanceBinding binding = ItemRecentAttendanceBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Attendance attendance = getItem(position);
        holder.bind(attendance);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecentAttendanceBinding binding;

        ViewHolder(ItemRecentAttendanceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Attendance attendance) {
            // Set date
            binding.dateText.setText(dateFormat.format(attendance.getDate()));
            
            // Set check-in time
            if (attendance.getCheckInTime() != null) {
                binding.checkInTimeText.setText(timeFormat.format(attendance.getCheckInTime()));
                binding.checkInTimeText.setVisibility(View.VISIBLE);
            } else {
                binding.checkInTimeText.setVisibility(View.GONE);
            }
            
            // Set check-out time
            if (attendance.getCheckOutTime() != null) {
                binding.checkOutTimeText.setText(timeFormat.format(attendance.getCheckOutTime()));
                binding.checkOutTimeText.setVisibility(View.VISIBLE);
            } else {
                binding.checkOutTimeText.setVisibility(View.GONE);
            }
            
            // Set status
            String status = attendance.getStatus();
            if (status != null) {
                switch (status.toLowerCase()) {
                    case "ontime":
                        binding.statusIcon.setImageResource(R.drawable.ic_check_circle);
                        binding.statusIcon.setColorFilter(
                            itemView.getContext().getColor(R.color.success));
                        break;
                    case "late":
                        binding.statusIcon.setImageResource(R.drawable.ic_warning);
                        binding.statusIcon.setColorFilter(
                            itemView.getContext().getColor(R.color.warning));
                        break;
                    case "missed":
                        binding.statusIcon.setImageResource(R.drawable.ic_error);
                        binding.statusIcon.setColorFilter(
                            itemView.getContext().getColor(R.color.error));
                        break;
                    default:
                        binding.statusIcon.setImageResource(R.drawable.ic_help);
                        binding.statusIcon.setColorFilter(
                            itemView.getContext().getColor(R.color.info));
                }
            }
        }
    }
} 