package com.example.attendify.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.databinding.ItemOfficeBinding;
import com.example.attendify.model.Office;

import java.util.function.Consumer;

public class OfficeAdapter extends ListAdapter<Office, OfficeAdapter.ViewHolder> {

    private final Consumer<Office> onEditClick;

    public OfficeAdapter(Consumer<Office> onEditClick) {
        super(new DiffCallback());
        this.onEditClick = onEditClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemOfficeBinding binding = ItemOfficeBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Office office = getItem(position);
        holder.bind(office);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemOfficeBinding binding;

        ViewHolder(ItemOfficeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Office office) {
            binding.nameText.setText(office.getName());
            binding.addressText.setText(office.getAddress());
            binding.radiusText.setText(String.format("%dm radius", office.getCheckInRadius()));
            binding.workHoursText.setText(String.format("%02d:%02d - %02d:%02d",
                    office.getStartHour(), office.getStartMinute(),
                    office.getEndHour(), office.getEndMinute()));

            binding.editButton.setOnClickListener(v -> onEditClick.accept(office));
        }
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<Office> {
        @Override
        public boolean areItemsTheSame(@NonNull Office oldItem, @NonNull Office newItem) {
            return (oldItem.getId() == null && newItem.getId() == null) ||
                   (oldItem.getId() != null && oldItem.getId().equals(newItem.getId()));
        }

        @Override
        public boolean areContentsTheSame(@NonNull Office oldItem, @NonNull Office newItem) {
            return oldItem.equals(newItem);
        }
    }
}