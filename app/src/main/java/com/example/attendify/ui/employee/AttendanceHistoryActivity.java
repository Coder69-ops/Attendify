package com.example.attendify.ui.employee;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.attendify.R;
import com.example.attendify.databinding.ActivityAttendanceHistoryBinding;
import com.example.attendify.model.Attendance;
import com.example.attendify.viewmodel.AttendanceViewModel;
import com.example.attendify.viewmodel.AuthViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryActivity extends AppCompatActivity {
    private static final String TAG = "AttendanceHistory";
    private static final int STORAGE_PERMISSION_CODE = 100;
    
    private ActivityAttendanceHistoryBinding binding;
    private AttendanceViewModel attendanceViewModel;
    private AuthViewModel authViewModel;
    private AttendanceAdapter attendanceAdapter;
    private List<Attendance> attendanceList = new ArrayList<>();
    private String userId;
    
    // Date formatting
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    
    // Month selection
    private String[] monthsArray;
    private int currentMonthPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set NoActionBar theme before inflating layout
        setTheme(R.style.Theme_Attendify_NoActionBar);
        binding = ActivityAttendanceHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Attendance History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize ViewModels
        attendanceViewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Set up month selector
        setupMonthSelector();
        
        // Set up observers
        setupObservers();
        
        // Set up export buttons
        binding.exportPdfButton.setOnClickListener(v -> checkPermissionAndExportPdf());
        binding.exportCsvButton.setOnClickListener(v -> checkPermissionAndExportCsv());
    }
    
    private void setupRecyclerView() {
        attendanceAdapter = new AttendanceAdapter();
        binding.attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.attendanceRecyclerView.setAdapter(attendanceAdapter);
    }
    
    private void setupMonthSelector() {
        // Generate last 12 months
        Calendar calendar = Calendar.getInstance();
        monthsArray = new String[12];
        for (int i = 0; i < 12; i++) {
            monthsArray[i] = monthFormat.format(calendar.getTime());
            calendar.add(Calendar.MONTH, -1);
        }
        
        // Current month is position 0
        currentMonthPosition = 0;
        
        // Set up spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, monthsArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.monthSpinner.setAdapter(adapter);
        
        binding.monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMonthPosition = position;
                loadAttendanceForMonth(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void setupObservers() {
        // Observe current user
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user == null) {
                // User logged out, go back to login
                finish();
                return;
            }
            userId = user.getUid();
            loadAttendanceForMonth(currentMonthPosition);
        });
        
        // Observe attendance data
        attendanceViewModel.getAttendanceHistoryLiveData().observe(this, attendanceList -> {
            this.attendanceList = attendanceList;
            attendanceAdapter.updateAttendanceList(attendanceList);
            
            // Show empty state or list
            if (attendanceList.isEmpty()) {
                binding.emptyStateLayout.setVisibility(View.VISIBLE);
                binding.attendanceRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyStateLayout.setVisibility(View.GONE);
                binding.attendanceRecyclerView.setVisibility(View.VISIBLE);
            }
            
            // Hide loading
            binding.progressBar.setVisibility(View.GONE);
        });
        
        // Observe loading state
        attendanceViewModel.getLoadingLiveData().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // Observe error state
        attendanceViewModel.getErrorLiveData().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                attendanceViewModel.resetError();
            }
        });
    }
    
    private void loadAttendanceForMonth(int monthOffset) {
        if (userId == null) return;
        
        binding.progressBar.setVisibility(View.VISIBLE);
        
        // Get month string for the selected position
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -monthOffset);
        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime());
        
        // Load attendance for the selected month
        attendanceViewModel.loadAttendanceHistoryForMonth(userId, yearMonth);
    }
    
    private void checkPermissionAndExportPdf() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                    STORAGE_PERMISSION_CODE);
        } else {
            exportAttendanceToPdf();
        }
    }
    
    private void checkPermissionAndExportCsv() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                    STORAGE_PERMISSION_CODE);
        } else {
            exportAttendanceToCsv();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, check which button was clicked
                if (binding.exportPdfButton.isPressed()) {
                    exportAttendanceToPdf();
                } else if (binding.exportCsvButton.isPressed()) {
                    exportAttendanceToCsv();
                }
            } else {
                Toast.makeText(this, "Storage permission is required to export reports", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void exportAttendanceToPdf() {
        if (attendanceList.isEmpty()) {
            Toast.makeText(this, "No attendance records to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Create document
        Document document = new Document();
        
        try {
            // Create directory if it doesn't exist
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Attendify");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Create file
            String fileName = "attendance_" + monthsArray[currentMonthPosition].replace(" ", "_") + ".pdf";
            File file = new File(dir, fileName);
            
            // Write to file
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();
            
            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("Attendance Report: " + monthsArray[currentMonthPosition], titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // Add space
            
            // Create table
            PdfPTable table = new PdfPTable(4); // 4 columns
            table.setWidthPercentage(100);
            
            // Set column widths
            float[] columnWidths = {2f, 1.5f, 1.5f, 1f}; // Date, Check-in, Check-out, Status
            table.setWidths(columnWidths);
            
            // Add header row
            addTableHeader(table);
            
            // Add data rows
            for (Attendance attendance : attendanceList) {
                addTableRow(table, attendance);
            }
            
            // Add table to document
            document.add(table);
            
            // Close document
            document.close();
            
            // Show success message with option to open file
            showExportSuccessDialog(file, "PDF");
            
        } catch (DocumentException | IOException e) {
            Log.e(TAG, "Error exporting PDF: " + e.getMessage());
            Toast.makeText(this, "Error creating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addTableHeader(PdfPTable table) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
        
        PdfPCell dateHeader = new PdfPCell(new Phrase("Date", headerFont));
        dateHeader.setBackgroundColor(new BaseColor(33, 150, 243)); // Primary color
        dateHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        dateHeader.setPadding(8);
        
        PdfPCell checkInHeader = new PdfPCell(new Phrase("Check-in", headerFont));
        checkInHeader.setBackgroundColor(new BaseColor(33, 150, 243));
        checkInHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        checkInHeader.setPadding(8);
        
        PdfPCell checkOutHeader = new PdfPCell(new Phrase("Check-out", headerFont));
        checkOutHeader.setBackgroundColor(new BaseColor(33, 150, 243));
        checkOutHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        checkOutHeader.setPadding(8);
        
        PdfPCell statusHeader = new PdfPCell(new Phrase("Status", headerFont));
        statusHeader.setBackgroundColor(new BaseColor(33, 150, 243));
        statusHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusHeader.setPadding(8);
        
        table.addCell(dateHeader);
        table.addCell(checkInHeader);
        table.addCell(checkOutHeader);
        table.addCell(statusHeader);
    }
    
    private void addTableRow(PdfPTable table, Attendance attendance) {
        // Date
        SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        PdfPCell dateCell = new PdfPCell(new Phrase(displayDateFormat.format(attendance.getCheckInTime())));
        dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        dateCell.setPadding(5);
        
        // Check-in time
        PdfPCell checkInCell = new PdfPCell(new Phrase(timeFormat.format(attendance.getCheckInTime())));
        checkInCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        checkInCell.setPadding(5);
        
        // Check-out time
        String checkOutTime = attendance.getCheckOutTime() != null ? 
                timeFormat.format(attendance.getCheckOutTime()) : "Not Checked Out";
        PdfPCell checkOutCell = new PdfPCell(new Phrase(checkOutTime));
        checkOutCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        checkOutCell.setPadding(5);
        
        // Status
        PdfPCell statusCell = new PdfPCell(new Phrase(attendance.getStatus()));
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusCell.setPadding(5);
        
        table.addCell(dateCell);
        table.addCell(checkInCell);
        table.addCell(checkOutCell);
        table.addCell(statusCell);
    }
    
    private void exportAttendanceToCsv() {
        if (attendanceList.isEmpty()) {
            Toast.makeText(this, "No attendance records to export", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Create directory if it doesn't exist
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Attendify");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // Create file
            String fileName = "attendance_" + monthsArray[currentMonthPosition].replace(" ", "_") + ".csv";
            File file = new File(dir, fileName);
            
            // Write to file
            FileWriter writer = new FileWriter(file);
            
            // Write header
            writer.append("Date,Check-in Time,Check-out Time,Status,Office\n");
            
            // Write data
            SimpleDateFormat displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            for (Attendance attendance : attendanceList) {
                // Date
                writer.append(displayDateFormat.format(attendance.getCheckInTime()));
                writer.append(",");
                
                // Check-in time
                writer.append(timeFormat.format(attendance.getCheckInTime()));
                writer.append(",");
                
                // Check-out time
                if (attendance.getCheckOutTime() != null) {
                    writer.append(timeFormat.format(attendance.getCheckOutTime()));
                } else {
                    writer.append("Not Checked Out");
                }
                writer.append(",");
                
                // Status
                writer.append(attendance.getStatus());
                writer.append(",");
                
                // Office
                writer.append(attendance.getOfficeName() != null ? attendance.getOfficeName() : "Unknown");
                writer.append("\n");
            }
            
            // Close writer
            writer.flush();
            writer.close();
            
            // Show success message with option to open file
            showExportSuccessDialog(file, "CSV");
            
        } catch (IOException e) {
            Log.e(TAG, "Error exporting CSV: " + e.getMessage());
            Toast.makeText(this, "Error creating CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showExportSuccessDialog(File file, String fileType) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Export Successful")
                .setMessage("The " + fileType + " file has been saved to Downloads/Attendify folder. Would you like to open it?")
                .setPositiveButton("Open", (dialog, which) -> openFile(file))
                .setNegativeButton("Close", null)
                .show();
    }
    
    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, 
                    "com.example.attendify.fileprovider", file);
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            
            if (file.getName().endsWith(".pdf")) {
                intent.setDataAndType(uri, "application/pdf");
            } else if (file.getName().endsWith(".csv")) {
                intent.setDataAndType(uri, "text/csv");
            }
            
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening file: " + e.getMessage());
            Toast.makeText(this, "No app found to open this file type", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
} 