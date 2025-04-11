package com.example.attendify.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.attendify.model.Office;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class OfficeViewModel extends ViewModel {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final MutableLiveData<List<Office>> officesLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);

    public LiveData<List<Office>> getOfficesLiveData() {
        return officesLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getLoadingLiveData() {
        return loadingLiveData;
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

    public void addOffice(Office office) {
        loadingLiveData.setValue(true);
        db.collection("offices")
                .add(office)
                .addOnSuccessListener(documentReference -> {
                    loadOffices();
                })
                .addOnFailureListener(e -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("Failed to add office: " + e.getMessage());
                });
    }

    public void updateOffice(Office office) {
        if (office.getId() == null) {
            errorLiveData.setValue("Cannot update office: missing ID");
            return;
        }

        loadingLiveData.setValue(true);
        db.collection("offices")
                .document(office.getId())
                .set(office)
                .addOnSuccessListener(aVoid -> {
                    List<Office> currentOffices = officesLiveData.getValue();
                    if (currentOffices != null) {
                        for (int i = 0; i < currentOffices.size(); i++) {
                            if (currentOffices.get(i).getId().equals(office.getId())) {
                                currentOffices.set(i, office);
                                break;
                            }
                        }
                        officesLiveData.setValue(currentOffices);
                    }
                    loadingLiveData.setValue(false);
                })
                .addOnFailureListener(e -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("Failed to update office: " + e.getMessage());
                });
    }

    public void deleteOffice(String officeId) {
        if (officeId == null) {
            errorLiveData.setValue("Cannot delete office: missing ID");
            return;
        }
        
        loadingLiveData.setValue(true);
        db.collection("offices")
                .document(officeId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    List<Office> currentOffices = officesLiveData.getValue();
                    if (currentOffices != null) {
                        currentOffices.removeIf(office -> 
                            officeId.equals(office.getId())
                        );
                        officesLiveData.setValue(currentOffices);
                    }
                    loadingLiveData.setValue(false);
                })
                .addOnFailureListener(e -> {
                    loadingLiveData.setValue(false);
                    errorLiveData.setValue("Failed to delete office: " + e.getMessage());
                });
    }

    public void resetError() {
        errorLiveData.setValue(null);
    }
}