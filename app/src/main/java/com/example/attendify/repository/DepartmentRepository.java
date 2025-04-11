package com.example.attendify.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.attendify.model.Department;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository class for Department operations with Firestore.
 */
public class DepartmentRepository {
    private static final String DEPARTMENTS_COLLECTION = "departments";
    
    private final FirebaseFirestore db;
    private final CollectionReference departmentsCollection;

    public DepartmentRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.departmentsCollection = db.collection(DEPARTMENTS_COLLECTION);
    }

    /**
     * Create a new department.
     * @param department Department to create
     * @return LiveData with operation result
     */
    public LiveData<Boolean> createDepartment(Department department) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        DocumentReference newDepartmentRef = departmentsCollection.document();
        department.setId(newDepartmentRef.getId());
        
        newDepartmentRef.set(department.toMap())
                .addOnSuccessListener(aVoid -> result.setValue(true))
                .addOnFailureListener(e -> result.setValue(false));
        
        return result;
    }

    /**
     * Update an existing department.
     * @param department Department to update
     * @return LiveData with operation result
     */
    public LiveData<Boolean> updateDepartment(Department department) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        departmentsCollection.document(department.getId())
                .update(department.toMap())
                .addOnSuccessListener(aVoid -> result.setValue(true))
                .addOnFailureListener(e -> result.setValue(false));
        
        return result;
    }

    /**
     * Delete a department (soft delete by setting active to false).
     * @param departmentId ID of the department to delete
     * @return LiveData with operation result
     */
    public LiveData<Boolean> deleteDepartment(String departmentId) {
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        departmentsCollection.document(departmentId)
                .update("active", false)
                .addOnSuccessListener(aVoid -> result.setValue(true))
                .addOnFailureListener(e -> result.setValue(false));
        
        return result;
    }

    /**
     * Get a department by ID.
     * @param departmentId ID of the department to retrieve
     * @return LiveData with Department object
     */
    public LiveData<Department> getDepartmentById(String departmentId) {
        MutableLiveData<Department> departmentLiveData = new MutableLiveData<>();
        
        departmentsCollection.document(departmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Department department = documentSnapshot.toObject(Department.class);
                        department.setId(documentSnapshot.getId());
                        departmentLiveData.setValue(department);
                    } else {
                        departmentLiveData.setValue(null);
                    }
                })
                .addOnFailureListener(e -> departmentLiveData.setValue(null));
        
        return departmentLiveData;
    }

    /**
     * Get all active departments.
     * @return LiveData with list of departments
     */
    public LiveData<List<Department>> getAllDepartments() {
        MutableLiveData<List<Department>> departmentsLiveData = new MutableLiveData<>();
        
        departmentsCollection
                .whereEqualTo("active", true)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Department> departments = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Department department = document.toObject(Department.class);
                        department.setId(document.getId());
                        departments.add(department);
                    }
                    departmentsLiveData.setValue(departments);
                })
                .addOnFailureListener(e -> departmentsLiveData.setValue(new ArrayList<>()));
        
        return departmentsLiveData;
    }
    
    /**
     * Get departments by manager ID.
     * @param managerId ID of the manager
     * @return LiveData with list of departments
     */
    public LiveData<List<Department>> getDepartmentsByManager(String managerId) {
        MutableLiveData<List<Department>> departmentsLiveData = new MutableLiveData<>();
        
        departmentsCollection
                .whereEqualTo("active", true)
                .whereEqualTo("managerId", managerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Department> departments = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Department department = document.toObject(Department.class);
                        department.setId(document.getId());
                        departments.add(department);
                    }
                    departmentsLiveData.setValue(departments);
                })
                .addOnFailureListener(e -> departmentsLiveData.setValue(new ArrayList<>()));
        
        return departmentsLiveData;
    }
} 