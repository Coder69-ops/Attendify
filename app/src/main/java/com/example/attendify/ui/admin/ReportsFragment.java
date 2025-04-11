package com.example.attendify.ui.admin;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.attendify.R;
import com.example.attendify.databinding.FragmentReportsBinding;
import com.example.attendify.model.Attendance;
import com.example.attendify.model.User;
import com.example.attendify.ui.admin.adapter.ReportAdapter;
import com.example.attendify.viewmodel.AttendanceViewModel;
import com.example.attendify.viewmodel.OfficeViewModel;
import com.example.attendify.viewmodel.UserViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private static final String TAG = "ReportsFragment";
    private FragmentReportsBinding binding;
    private AttendanceViewModel attendanceViewModel;
    private UserViewModel userViewModel;
    private OfficeViewModel officeViewModel;
    private ReportAdapter adapter;
    private List<Attendance> allAttendanceRecords = new ArrayList<>();
    private List<User> allUsers = new ArrayList<>();
    private String selectedOfficeId = null;
    private String selectedUserId = null;
    private String selectedMonth = null;
    private Date startDate;
    private Date endDate;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        attendanceViewModel = new ViewModelProvider(requireActivity()).get(AttendanceViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        officeViewModel = new ViewModelProvider(requireActivity()).get(OfficeViewModel.class);
        
        setupRecyclerView();
        setupFilterSpinners();
        setupDateRangePickers();
        setupExportButtons();
        
        // Initial data load - all attendance records for current month
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat yearMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        selectedMonth = yearMonthFormat.format(calendar.getTime());
        
        loadReportData();
    }
    
    private void setupRecyclerView() {
        adapter = new ReportAdapter();
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }
    
    private void setupFilterSpinners() {
        // Setup office spinner
        officeViewModel.getOfficesLiveData().observe(getViewLifecycleOwner(), offices -> {
            List<String> officeNames = new ArrayList<>();
            officeNames.add("All Offices");
            
            // Office ID to position mapping for selection
            for (int i = 0; i < offices.size(); i++) {
                // Add null check to prevent NullPointerException
                String officeName = offices.get(i).getName();
                if (officeName != null) {
                    officeNames.add(officeName);
                } else {
                    officeNames.add("Unknown Office");
                }
            }
            
            ArrayAdapter<String> officeAdapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, officeNames);
            officeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.officeSpinner.setAdapter(officeAdapter);
            
            binding.officeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        selectedOfficeId = null;
                    } else if (position <= offices.size()) {
                        selectedOfficeId = offices.get(position - 1).getId();
                    }
                    loadReportData();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedOfficeId = null;
                }
            });
        });
        
        // Setup user spinner (employees)
        userViewModel.getEmployees().observe(getViewLifecycleOwner(), users -> {
            allUsers = users;
            List<String> userNames = new ArrayList<>();
            userNames.add("All Employees");
            
            for (User user : users) {
                // Add null check to prevent NullPointerException
                String displayName = user.getDisplayName();
                if (displayName != null) {
                    userNames.add(displayName);
                } else if (user.getName() != null) {
                    userNames.add(user.getName());
                } else {
                    userNames.add("Unknown Employee");
                }
            }
            
            ArrayAdapter<String> userAdapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item, userNames);
            userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.employeeSpinner.setAdapter(userAdapter);
            
            binding.employeeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == 0) {
                        // "All Employees" option selected
                        Log.d(TAG, "All Employees selected");
                        selectedUserId = null;
                    } else if (position <= users.size()) {
                        // Specific employee selected
                        selectedUserId = users.get(position - 1).getUid();
                        Log.d(TAG, "Selected employee: " + users.get(position - 1).getDisplayName() + " (ID: " + selectedUserId + ")");
                    }
                    loadReportData();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedUserId = null;
                }
            });
        });
        
        // Setup month spinner
        Calendar calendar = Calendar.getInstance();
        List<String> months = new ArrayList<>();
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        SimpleDateFormat valueFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        
        // Add last 12 months to the spinner
        for (int i = 0; i < 12; i++) {
            String displayMonth = displayFormat.format(calendar.getTime());
            String valueMonth = valueFormat.format(calendar.getTime());
            months.add(displayMonth);
            
            if (i == 0) {
                selectedMonth = valueMonth; // Default to current month
            }
            
            calendar.add(Calendar.MONTH, -1); // Go back one month
        }
        
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.monthSpinner.setAdapter(monthAdapter);
        
        binding.monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.MONTH, -position);
                selectedMonth = valueFormat.format(cal.getTime());
                loadReportData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Use current month by default
                selectedMonth = valueFormat.format(Calendar.getInstance().getTime());
            }
        });
    }
    
    private void setupDateRangePickers() {
        // Start Date picker
        binding.startDateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar startCal = Calendar.getInstance();
                        startCal.set(year, month, dayOfMonth, 0, 0, 0);
                        startDate = startCal.getTime();
                        binding.startDateButton.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(startDate));
                        
                        if (endDate != null && startDate.after(endDate)) {
                            endDate = startDate;
                            binding.endDateButton.setText(binding.startDateButton.getText());
                        }
                        
                        loadReportData();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // End Date picker
        binding.endDateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar endCal = Calendar.getInstance();
                        endCal.set(year, month, dayOfMonth, 23, 59, 59);
                        endDate = endCal.getTime();
                        binding.endDateButton.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate));
                        
                        if (startDate != null && endDate.before(startDate)) {
                            startDate = endDate;
                            binding.startDateButton.setText(binding.endDateButton.getText());
                        }
                        
                        loadReportData();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        // Clear date filter button
        binding.clearDateFilterButton.setOnClickListener(v -> {
            startDate = null;
            endDate = null;
            binding.startDateButton.setText(R.string.start_date);
            binding.endDateButton.setText(R.string.end_date);
            loadReportData();
        });
    }
    
    private void setupExportButtons() {
        // Export to PDF
        binding.exportPdfButton.setOnClickListener(v -> {
            if (allAttendanceRecords.isEmpty()) {
                Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show();
                return;
            }
            exportToPdf();
        });
        
        // Export to CSV
        binding.exportCsvButton.setOnClickListener(v -> {
            if (allAttendanceRecords.isEmpty()) {
                Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show();
                return;
            }
            exportToCsv();
        });
        
        // Seed test data button
        binding.seedDataButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Seed Test Data")
                .setMessage("This will generate 30 days of random attendance data for all employees. Continue?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Get currently selected office
                    String officeId = selectedOfficeId;
                    String officeName = "Default Office";
                    
                    // If an office is selected, get its name
                    if (officeId != null && binding.officeSpinner.getSelectedItemPosition() > 0) {
                        officeName = binding.officeSpinner.getSelectedItem().toString();
                    }
                    
                    // Create an instance of DataSeedingUtil and seed attendance data
                    com.example.attendify.util.DataSeedingUtil dataSeedingUtil = 
                        new com.example.attendify.util.DataSeedingUtil(requireContext());
                    dataSeedingUtil.seedAttendanceData(officeId, officeName);
                })
                .setNegativeButton("Cancel", null)
                .show();
        });
    }
    
    private void loadReportData() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.noDataTextView.setVisibility(View.GONE);
        
        Log.d(TAG, "Loading report data: selectedUserId=" + selectedUserId + ", selectedOfficeId=" + selectedOfficeId + ", selectedMonth=" + selectedMonth);
        
        if (selectedUserId != null) {
            // Load data for specific user
            attendanceViewModel.getAttendanceHistoryForMonth(selectedUserId, selectedMonth)
                    .observe(getViewLifecycleOwner(), attendanceList -> {
                        Log.d(TAG, "Loaded specific user data: " + (attendanceList != null ? attendanceList.size() : 0) + " records");
                        processAttendanceData(attendanceList);
                    });
        } else {
            // Load data for all users (might be filtered by office)
            Log.d(TAG, "Loading data for ALL employees" + (selectedOfficeId != null ? " in office " + selectedOfficeId : ""));
            attendanceViewModel.getAllAttendanceForMonth(selectedMonth, selectedOfficeId)
                    .observe(getViewLifecycleOwner(), attendanceList -> {
                        Log.d(TAG, "Loaded all users data: " + (attendanceList != null ? attendanceList.size() : 0) + " records");
                        processAttendanceData(attendanceList);
                    });
        }
    }
    
    private void processAttendanceData(List<Attendance> attendanceList) {
        // Add null check for attendanceList
        if (attendanceList == null) {
            attendanceList = new ArrayList<>();
        }
        
        allAttendanceRecords = new ArrayList<>(attendanceList);
        
        // Apply date filters if set
        if (startDate != null || endDate != null) {
            List<Attendance> filteredList = new ArrayList<>();
            
            for (Attendance attendance : allAttendanceRecords) {
                Date checkInDate = attendance.getCheckInTime();
                
                // Skip records with null check-in dates
                if (checkInDate == null) {
                    continue;
                }
                
                boolean include = true;
                if (startDate != null && checkInDate.before(startDate)) {
                    include = false;
                }
                if (endDate != null && checkInDate.after(endDate)) {
                    include = false;
                }
                
                if (include) {
                    filteredList.add(attendance);
                }
            }
            
            allAttendanceRecords = filteredList;
        }
        
        adapter.submitList(allAttendanceRecords);
        
        binding.progressBar.setVisibility(View.GONE);
        if (allAttendanceRecords.isEmpty()) {
            binding.noDataTextView.setVisibility(View.VISIBLE);
            binding.statsLayout.setVisibility(View.GONE);
        } else {
            binding.noDataTextView.setVisibility(View.GONE);
            updateStatistics();
            binding.statsLayout.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateStatistics() {
        int totalRecords = allAttendanceRecords.size();
        int onTimeCount = 0;
        int lateCount = 0;
        int missedCount = 0;
        
        for (Attendance attendance : allAttendanceRecords) {
            // Add null check for status
            String status = attendance.getStatus();
            if (status == null) {
                continue;
            }
            
            switch (status) {
                case "OnTime":
                    onTimeCount++;
                    break;
                case "Late":
                    lateCount++;
                    break;
                case "Missed":
                    missedCount++;
                    break;
            }
        }
        
        // Update statistics cards
        binding.totalAttendanceValue.setText(String.valueOf(totalRecords));
        binding.onTimeValue.setText(String.valueOf(onTimeCount));
        binding.lateValue.setText(String.valueOf(lateCount));
        binding.missedValue.setText(String.valueOf(missedCount));
        
        // Calculate percentages
        if (totalRecords > 0) {
            float onTimePercent = (float) onTimeCount / totalRecords * 100;
            float latePercent = (float) lateCount / totalRecords * 100;
            float missedPercent = (float) missedCount / totalRecords * 100;
            
            binding.onTimePercent.setText(String.format("%.1f%%", onTimePercent));
            binding.latePercent.setText(String.format("%.1f%%", latePercent));
            binding.missedPercent.setText(String.format("%.1f%%", missedPercent));
        } else {
            binding.onTimePercent.setText("0%");
            binding.latePercent.setText("0%");
            binding.missedPercent.setText("0%");
        }
    }
    
    private void exportToPdf() {
        try {
            String fileName = "attendance_report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
            File pdfFile = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
            
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.open();
            
            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Attendance Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // Add spacing
            
            // Add report details
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);
            document.add(new Paragraph("Generated on: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()), normalFont));
            
            if (selectedOfficeId != null) {
                String officeName = "Unknown";
                for (int i = 0; i < binding.officeSpinner.getCount(); i++) {
                    if (binding.officeSpinner.getSelectedItemPosition() > 0) {
                        officeName = binding.officeSpinner.getSelectedItem().toString();
                    }
                }
                document.add(new Paragraph("Office: " + officeName, normalFont));
            } else {
                document.add(new Paragraph("Office: All Offices", normalFont));
            }
            
            if (selectedUserId != null) {
                String userName = "Unknown";
                for (User user : allUsers) {
                    if (user.getUid().equals(selectedUserId)) {
                        userName = user.getDisplayName();
                        break;
                    }
                }
                document.add(new Paragraph("Employee: " + userName, normalFont));
            } else {
                document.add(new Paragraph("Employee: All Employees", normalFont));
            }
            
            document.add(new Paragraph("Period: " + binding.monthSpinner.getSelectedItem().toString(), normalFont));
            document.add(new Paragraph(" ")); // Add spacing
            
            // Add statistics
            document.add(new Paragraph("Statistics:", normalFont));
            document.add(new Paragraph("Total Records: " + binding.totalAttendanceValue.getText(), normalFont));
            document.add(new Paragraph("On Time: " + binding.onTimeValue.getText() + " (" + binding.onTimePercent.getText() + ")", normalFont));
            document.add(new Paragraph("Late: " + binding.lateValue.getText() + " (" + binding.latePercent.getText() + ")", normalFont));
            document.add(new Paragraph("Missed: " + binding.missedValue.getText() + " (" + binding.missedPercent.getText() + ")", normalFont));
            document.add(new Paragraph(" ")); // Add spacing
            
            // Add table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            
            // Add table headers
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            PdfPCell cell = new PdfPCell(new Paragraph("Employee", headerFont));
            table.addCell(cell);
            cell = new PdfPCell(new Paragraph("Office", headerFont));
            table.addCell(cell);
            cell = new PdfPCell(new Paragraph("Date", headerFont));
            table.addCell(cell);
            cell = new PdfPCell(new Paragraph("Check In", headerFont));
            table.addCell(cell);
            cell = new PdfPCell(new Paragraph("Check Out", headerFont));
            table.addCell(cell);
            cell = new PdfPCell(new Paragraph("Status", headerFont));
            table.addCell(cell);
            
            // Add data rows
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            
            for (Attendance attendance : allAttendanceRecords) {
                // Add null checks for all fields that might be null
                String userName = attendance.getUserName() != null ? attendance.getUserName() : "Unknown";
                String officeName = attendance.getOfficeName() != null ? attendance.getOfficeName() : "Unknown";
                String date = attendance.getDate() != null ? attendance.getDate() : "N/A";
                
                table.addCell(new Paragraph(userName, normalFont));
                table.addCell(new Paragraph(officeName, normalFont));
                table.addCell(new Paragraph(date, normalFont));
                table.addCell(new Paragraph(attendance.getCheckInTime() != null ? timeFormat.format(attendance.getCheckInTime()) : "N/A", normalFont));
                table.addCell(new Paragraph(attendance.getCheckOutTime() != null ? timeFormat.format(attendance.getCheckOutTime()) : "N/A", normalFont));
                table.addCell(new Paragraph(attendance.getStatus() != null ? attendance.getStatus() : "Unknown", normalFont));
            }
            
            document.add(table);
            document.close();
            
            // Share the PDF file
            shareFile(pdfFile, "application/pdf");
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting to PDF: ", e);
            Toast.makeText(requireContext(), "Error exporting to PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void exportToCsv() {
        try {
            String fileName = "attendance_report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".csv";
            File csvFile = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
            
            FileWriter writer = new FileWriter(csvFile);
            
            // Write header
            writer.append("Employee,Office,Date,Check In,Check Out,Status,Location Status\n");
            
            // Write data rows
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            
            for (Attendance attendance : allAttendanceRecords) {
                // Add null checks for all fields that might be null
                String userName = attendance.getUserName() != null ? attendance.getUserName() : "Unknown";
                String officeName = attendance.getOfficeName() != null ? attendance.getOfficeName() : "Unknown";
                String date = attendance.getDate() != null ? attendance.getDate() : "N/A";
                String status = attendance.getStatus() != null ? attendance.getStatus() : "Unknown";
                
                writer.append(escapeCsv(userName)).append(",");
                writer.append(escapeCsv(officeName)).append(",");
                writer.append(escapeCsv(date)).append(",");
                writer.append(escapeCsv(attendance.getCheckInTime() != null ? timeFormat.format(attendance.getCheckInTime()) : "N/A")).append(",");
                writer.append(escapeCsv(attendance.getCheckOutTime() != null ? timeFormat.format(attendance.getCheckOutTime()) : "N/A")).append(",");
                writer.append(escapeCsv(status)).append("\n");
            }
            
            writer.flush();
            writer.close();
            
            // Share the CSV file
            shareFile(csvFile, "text/csv");
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting to CSV: ", e);
            Toast.makeText(requireContext(), "Error exporting to CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        
        // If the value contains a comma, quote, or newline, wrap it in quotes and escape any quotes
        if (value.contains("\"") || value.contains(",") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
    
    private void shareFile(File file, String mimeType) {
        Uri fileUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file
        );
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        startActivity(Intent.createChooser(shareIntent, "Share Attendance Report"));
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 