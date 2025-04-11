package com.example.attendify.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.example.attendify.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

/**
 * Dialog for importing employee data from CSV files
 */
public class CsvImportDialog extends Dialog {

    private LinearProgressIndicator progressBar;
    private MaterialTextView statusText, resultText;
    private MaterialButton btnSelectFile, btnUpload, btnClose;
    private Uri selectedFileUri;
    private OnImportCompletedListener listener;

    public interface OnImportCompletedListener {
        void onImportCompleted(int successCount, int failCount);
    }

    public CsvImportDialog(@NonNull Context context) {
        super(context);
    }

    public void setOnImportCompletedListener(OnImportCompletedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_csv_import, null);
        setContentView(view);
        
        // Initialize views
        progressBar = view.findViewById(R.id.progressBar);
        statusText = view.findViewById(R.id.statusText);
        resultText = view.findViewById(R.id.resultText);
        btnSelectFile = view.findViewById(R.id.btnSelectFile);
        btnUpload = view.findViewById(R.id.btnUpload);
        btnClose = view.findViewById(R.id.btnClose);
        
        // Initial state
        updateState(ImportState.SELECT_FILE);
        
        // Setup click listeners
        setupListeners();
    }
    
    private enum ImportState {
        SELECT_FILE,
        READY_TO_UPLOAD,
        UPLOADING,
        COMPLETED,
        ERROR
    }
    
    private void updateState(ImportState state) {
        switch (state) {
            case SELECT_FILE:
                statusText.setText("Select a CSV file to import");
                btnSelectFile.setEnabled(true);
                btnUpload.setEnabled(false);
                progressBar.setVisibility(View.GONE);
                resultText.setVisibility(View.GONE);
                break;
                
            case READY_TO_UPLOAD:
                statusText.setText("File selected. Ready to upload.");
                btnSelectFile.setEnabled(true);
                btnUpload.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                resultText.setVisibility(View.GONE);
                break;
                
            case UPLOADING:
                statusText.setText("Uploading and processing...");
                btnSelectFile.setEnabled(false);
                btnUpload.setEnabled(false);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setIndeterminate(true);
                resultText.setVisibility(View.GONE);
                break;
                
            case COMPLETED:
                statusText.setText("Import completed");
                btnSelectFile.setEnabled(true);
                btnUpload.setEnabled(false);
                progressBar.setVisibility(View.GONE);
                resultText.setVisibility(View.VISIBLE);
                break;
                
            case ERROR:
                statusText.setText("Error occurred during import");
                btnSelectFile.setEnabled(true);
                btnUpload.setEnabled(true);
                progressBar.setVisibility(View.GONE);
                resultText.setVisibility(View.VISIBLE);
                resultText.setText("Failed to process the file. Please check the format and try again.");
                break;
        }
    }
    
    private void setupListeners() {
        btnSelectFile.setOnClickListener(v -> {
            if (getContext() instanceof FragmentActivity) {
                openFilePicker();
            } else {
                Toast.makeText(getContext(), "Cannot open file picker", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnUpload.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                uploadFile();
            }
        });
        
        btnClose.setOnClickListener(v -> dismiss());
    }
    
    private void openFilePicker() {
        FragmentActivity activity = (FragmentActivity) getContext();
        ActivityResultLauncher<String> filePicker = activity.getActivityResultRegistry().register(
                "file_picker",
                activity,
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedFileUri = uri;
                        updateState(ImportState.READY_TO_UPLOAD);
                    }
                }
        );
        
        filePicker.launch("text/csv");
    }
    
    private void uploadFile() {
        updateState(ImportState.UPLOADING);
        
        // Simulate processing delay
        btnUpload.postDelayed(() -> {
            // For demo purposes, simulate a successful import
            updateState(ImportState.COMPLETED);
            int successCount = 5;
            int failCount = 0;
            resultText.setText("Successfully imported " + successCount + " employees.");
            
            if (listener != null) {
                listener.onImportCompleted(successCount, failCount);
            }
        }, 2000);
    }
} 