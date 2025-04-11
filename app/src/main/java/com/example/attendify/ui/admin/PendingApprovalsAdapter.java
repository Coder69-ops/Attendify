package com.example.attendify.ui.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.databinding.ItemPendingApprovalBinding;
import com.example.attendify.model.User;

public class PendingApprovalsAdapter extends ListAdapter<User, PendingApprovalsAdapter.ViewHolder> {
    
    private final OnPendingApprovalListener listener;

    public PendingApprovalsAdapter(OnPendingApprovalListener listener) {
        super(new DiffCallback());
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPendingApprovalBinding binding = ItemPendingApprovalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPendingApprovalBinding binding;

        ViewHolder(ItemPendingApprovalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            binding.nameText.setText(user.getName());
            binding.emailText.setText(user.getEmail());
            binding.officeText.setText("Office: " + user.getOfficeId());

            binding.approveButton.setOnClickListener(v -> listener.onApprove(user));
            binding.rejectButton.setOnClickListener(v -> listener.onReject(user));
        }
    }

    private static class DiffCallback extends DiffUtil.ItemCallback<User> {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getUid().equals(newItem.getUid());
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.equals(newItem);
        }
    }

    public interface OnPendingApprovalListener {
        void onApprove(User user);
        void onReject(User user);
    }
}