package com.example.attendify.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.Observer;

import com.example.attendify.model.User;
import com.example.attendify.repository.AttendanceRepository;
import com.example.attendify.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for seeding test data into the application
 */
public class DataSeedingUtil {
    private static final String TAG = "DataSeedingUtil";
    
    private final Context context;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    
    public DataSeedingUtil(Context context) {
        this.context = context;
        this.userRepository = UserRepository.getInstance();
        this.attendanceRepository = AttendanceRepository.getInstance();
    }
    
    /**
     * Seeds test attendance data for all employees in the given office
     * @param officeId ID of the office to seed data for
     * @param officeName Name of the office
     */
    public void seedAttendanceData(String officeId, String officeName) {
        Toast.makeText(context, "Starting to seed attendance data...", Toast.LENGTH_SHORT).show();
        
        // Get all employees
        userRepository.getAllEmployees().observeForever(new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> employees) {
                if (employees == null || employees.isEmpty()) {
                    Toast.makeText(context, "No employees found to seed data for", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Filter employees by office if an office ID is provided
                List<User> filteredEmployees = new ArrayList<>();
                if (officeId != null && !officeId.isEmpty()) {
                    for (User employee : employees) {
                        if (officeId.equals(employee.getOfficeId())) {
                            filteredEmployees.add(employee);
                        }
                    }
                } else {
                    filteredEmployees.addAll(employees);
                }
                
                if (filteredEmployees.isEmpty()) {
                    Toast.makeText(context, "No employees found in the selected office", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Extract user IDs and names
                List<String> userIds = new ArrayList<>();
                List<String> userNames = new ArrayList<>();
                for (User employee : filteredEmployees) {
                    userIds.add(employee.getUid());
                    userNames.add(employee.getDisplayName());
                }
                
                // Seed test attendance data
                attendanceRepository.seedTestAttendanceData(userIds, userNames, officeId, officeName)
                        .observeForever(new Observer<Boolean>() {
                            @Override
                            public void onChanged(Boolean success) {
                                if (success) {
                                    Toast.makeText(context, 
                                            "Successfully seeded attendance data for " + 
                                            filteredEmployees.size() + " employees", 
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(context, 
                                            "Failed to seed attendance data", 
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
} 