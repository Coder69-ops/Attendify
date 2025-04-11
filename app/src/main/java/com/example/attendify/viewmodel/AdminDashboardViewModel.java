package com.example.attendify.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.attendify.model.Office;
import com.example.attendify.model.PendingApproval;
import com.example.attendify.model.User;
import com.example.attendify.repository.UserRepository;
import com.example.attendify.util.SingleLiveEvent;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final UserRepository userRepository;
    
    private final MutableLiveData<List<Office>> officesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<User>> pendingUsersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final SingleLiveEvent<String> errorLiveData = new SingleLiveEvent<>();

    public AdminDashboardViewModel() {
        userRepository = UserRepository.getInstance();
    }

    public LiveData<List<Office>> getOfficesLiveData() {
        return officesLiveData;
    }

    public LiveData<List<User>> getPendingUsersLiveData() {
        return pendingUsersLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public void loadOffices() {
        loadingLiveData.setValue(true);
        db.collection("offices")
                .get()
                .addOnSuccessListener(this::handleOfficesSnapshot)
                .addOnFailureListener(e -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("Failed to load offices: " + e.getMessage());
                });
    }

    private void handleOfficesSnapshot(QuerySnapshot snapshot) {
        List<Office> offices = new ArrayList<>();
        snapshot.forEach(doc -> {
            Office office = doc.toObject(Office.class);
            if (office != null) {
                office.setId(doc.getId());
                offices.add(office);
            }
        });
        officesLiveData.setValue(offices);
        loadingLiveData.setValue(false);
    }

    public void loadPendingUsers() {
        loadingLiveData.setValue(true);
        userRepository.getPendingApprovalUsers()
                .observeForever(result -> {
                    loadingLiveData.setValue(false);
                    if (result != null) {
                        pendingUsersLiveData.setValue(result);
                    } else {
                        errorLiveData.setValue("Failed to load pending approvals");
                    }
                });
    }

    public void updateOffice(Office office) {
        loadingLiveData.setValue(true);
        db.collection("offices")
                .document(office.getId())
                .set(office)
                .addOnSuccessListener(aVoid -> {
                    loadOffices(); // Refresh the list
                    errorLiveData.setValue("Office updated successfully");
                })
                .addOnFailureListener(e -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("Failed to update office: " + e.getMessage());
                });
    }

    public void approveUser(String userId) {
        loadingLiveData.setValue(true);
        userRepository.approveUser(userId)
                .observeForever(result -> {
                    loadingLiveData.setValue(false);
                    if (Boolean.TRUE.equals(result)) {
                        loadPendingUsers(); // Refresh the list
                        errorLiveData.setValue("User approved successfully");
                    } else {
                        errorLiveData.setValue("Failed to approve user");
                    }
                });
    }

    public void rejectUser(String userId) {
        loadingLiveData.setValue(true);
        userRepository.rejectUser(userId)
                .observeForever(result -> {
                    loadingLiveData.setValue(false);
                    if (Boolean.TRUE.equals(result)) {
                        loadPendingUsers(); // Refresh the list
                        errorLiveData.setValue("User rejected successfully");
                    } else {
                        errorLiveData.setValue("Failed to reject user");
                    }
                });
    }

    public void resetError() {
        errorLiveData.setValue(null);
    }
}