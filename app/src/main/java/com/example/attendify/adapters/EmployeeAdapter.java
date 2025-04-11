package com.example.attendify.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendify.R;
import com.example.attendify.model.User;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private Context context;
    private List<User> employeeList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onEditClick(User user, int position);
        void onPermissionsClick(User user, int position);
        void onMetricsClick(User user, int position);
    }

    public EmployeeAdapter(Context context, List<User> employeeList) {
        this.context = context;
        this.employeeList = employeeList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        User user = employeeList.get(position);
        
        holder.checkboxSelect.setChecked(false); // Reset selection state
        holder.textName.setText(user.getName());
        holder.textEmail.setText(user.getEmail());
        
        // Set department, role, and status if available
        String department = user.getDepartmentId() != null ? user.getDepartmentId() : "Not assigned";
        String role = user.getRole() != null ? user.getRole() : "Employee";
        String status = user.isActive() ? "Active" : "Inactive";
        
        holder.textDepartment.setText(department);
        holder.textRole.setText(role);
        holder.textStatus.setText(status);
        
        // Set click listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(user, holder.getAdapterPosition());
            }
        });
        
        holder.btnPermissions.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPermissionsClick(user, holder.getAdapterPosition());
            }
        });
        
        holder.btnMetrics.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMetricsClick(user, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return employeeList != null ? employeeList.size() : 0;
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkboxSelect;
        TextView textName, textEmail, textDepartment, textRole, textStatus;
        MaterialButton btnEdit, btnPermissions, btnMetrics;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            checkboxSelect = itemView.findViewById(R.id.checkboxSelect);
            textName = itemView.findViewById(R.id.textName);
            textEmail = itemView.findViewById(R.id.textEmail);
            textDepartment = itemView.findViewById(R.id.textDepartment);
            textRole = itemView.findViewById(R.id.textRole);
            textStatus = itemView.findViewById(R.id.textStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnPermissions = itemView.findViewById(R.id.btnPermissions);
            btnMetrics = itemView.findViewById(R.id.btnMetrics);
        }
    }
} 