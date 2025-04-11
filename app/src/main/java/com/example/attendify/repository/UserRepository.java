package com.example.attendify.repository;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.example.attendify.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Repository for user-related Firebase operations
 */
public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String USERS_COLLECTION = "users";
    
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final CollectionReference usersCollection;
    
    // Singleton instance
    private static UserRepository instance;
    
    public static UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }
    
    private UserRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        usersCollection = firestore.collection(USERS_COLLECTION);
    }
    
    // Add auth state listener
    public void getAuthStateListener(Consumer<FirebaseUser> callback) {
        firebaseAuth.addAuthStateListener(auth -> callback.accept(auth.getCurrentUser()));
    }

    // Register a new user with email and password
    public MutableLiveData<User> registerUser(String email, String password, String name, String role, String officeId) {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Send email verification
                        firebaseUser.sendEmailVerification();
                        
                        // Create user in Firestore
                        User user = new User(
                                firebaseUser.getUid(),
                                name,
                                email,
                                role,
                                false, // Not approved by default
                                officeId
                        );
                        
                        usersCollection.document(firebaseUser.getUid())
                                .set(user.toMap())
                                .addOnSuccessListener(aVoid -> userLiveData.setValue(user))
                                .addOnFailureListener(e -> userLiveData.setValue(null));
                    } else {
                        userLiveData.setValue(null);
                    }
                })
                .addOnFailureListener(e -> userLiveData.setValue(null));
        
        return userLiveData;
    }
    
    // Login with email and password
    public MutableLiveData<User> loginUser(String email, String password) {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser != null) {
                        // Get user data from Firestore
                        getUserData(firebaseUser.getUid(), userLiveData);
                    } else {
                        userLiveData.setValue(null);
                    }
                })
                .addOnFailureListener(e -> userLiveData.setValue(null));
        
        return userLiveData;
    }
    
    // Get user data from Firestore
    public void getUserData(String uid, MutableLiveData<User> userLiveData) {
        usersCollection.document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Try to handle both User model formats
                        User user = new User();
                        user.setUid(documentSnapshot.getId());
                        
                        // Get email (both models have this)
                        user.setEmail(documentSnapshot.getString("email"));
                        
                        // Get name (might be 'name' or 'fullName')
                        if (documentSnapshot.contains("name")) {
                            user.setName(documentSnapshot.getString("name"));
                        } else if (documentSnapshot.contains("fullName")) {
                            user.setName(documentSnapshot.getString("fullName"));
                        }
                        
                        // Get role (both models have this)
                        user.setRole(documentSnapshot.getString("role"));
                        
                        // Get officeId (might be 'officeId' or 'office')
                        if (documentSnapshot.contains("officeId")) {
                            user.setOfficeId(documentSnapshot.getString("officeId"));
                        } else if (documentSnapshot.contains("office")) {
                            user.setOfficeId(documentSnapshot.getString("office"));
                        }
                        
                        // Get departmentId (might be 'departmentId' or 'department')
                        if (documentSnapshot.contains("departmentId")) {
                            user.setDepartmentId(documentSnapshot.getString("departmentId"));
                        } else if (documentSnapshot.contains("department")) {
                            user.setDepartmentId(documentSnapshot.getString("department"));
                        }
                        
                        // Get teamId (might be 'teamId' or 'team')
                        if (documentSnapshot.contains("teamId")) {
                            user.setTeamId(documentSnapshot.getString("teamId"));
                        } else if (documentSnapshot.contains("team")) {
                            user.setTeamId(documentSnapshot.getString("team"));
                        }
                        
                        // Get manager info (might be 'managerId' or 'manager')
                        if (documentSnapshot.contains("managerId")) {
                            user.setManagerId(documentSnapshot.getString("managerId"));
                        } else if (documentSnapshot.contains("manager")) {
                            user.setManagerId(documentSnapshot.getString("manager"));
                        }
                        
                        // Handle approval status (might be 'isApproved' or 'status')
                        if (documentSnapshot.contains("isApproved")) {
                            user.setApproved(documentSnapshot.getBoolean("isApproved"));
                        } else if (documentSnapshot.contains("status")) {
                            user.setStatus(documentSnapshot.getString("status"));
                        }
                        
                        // Handle manager status
                        if (documentSnapshot.contains("isManager")) {
                            user.setManager(documentSnapshot.getBoolean("isManager"));
                        }
                        
                        // Handle profile image
                        user.setProfileImageUrl(documentSnapshot.getString("profileImageUrl"));
                        
                        userLiveData.setValue(user);
                    } else {
                        userLiveData.setValue(null);
                    }
                })
                .addOnFailureListener(e -> userLiveData.setValue(null));
    }
    
    // Get current user
    public MutableLiveData<User> getCurrentUser() {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            getUserData(firebaseUser.getUid(), userLiveData);
        } else {
            userLiveData.setValue(null);
        }
        
        return userLiveData;
    }
    
    // Get user profile
    public MutableLiveData<User> getUserProfile(String uid) {
        MutableLiveData<User> userLiveData = new MutableLiveData<>();
        getUserData(uid, userLiveData);
        return userLiveData;
    }
    
    // Logout current user
    public void logout() {
        firebaseAuth.signOut();
    }
    
    // Approve a user
    public MutableLiveData<Boolean> approveUser(String uid) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        // Update both isApproved and status fields to ensure consistency
        Map<String, Object> updates = new HashMap<>();
        updates.put("isApproved", true);
        updates.put("status", "approved");
        
        usersCollection.document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> resultLiveData.setValue(true))
                .addOnFailureListener(e -> resultLiveData.setValue(false));
        
        return resultLiveData;
    }
    
    // Reject a user (mark as rejected instead of deleting)
    public MutableLiveData<Boolean> rejectUser(String uid) {
        MutableLiveData<Boolean> resultLiveData = new MutableLiveData<>();
        
        // Update both isApproved and status fields to ensure consistency
        Map<String, Object> updates = new HashMap<>();
        updates.put("isApproved", false);
        updates.put("status", "rejected");
        
        usersCollection.document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> resultLiveData.setValue(true))
                .addOnFailureListener(e -> resultLiveData.setValue(false));
        
        return resultLiveData;
    }
    
    // Get pending approval users
    public MutableLiveData<List<User>> getPendingApprovalUsers() {
        MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>();
        
        // Query for users with isApproved=false AND status="pending" 
        // (excluding rejected users)
        usersCollection.whereEqualTo("isApproved", false)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUid(document.getId());
                            users.add(user);
                        }
                    }
                    usersLiveData.setValue(users);
                })
                .addOnFailureListener(e -> usersLiveData.setValue(new ArrayList<>()));
        
        return usersLiveData;
    }
    
    // Get users by role (admin or employee)
    public MutableLiveData<List<User>> getUsersByRole(String role) {
        MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>();
        
        usersCollection.whereEqualTo("role", role)
                .whereEqualTo("isApproved", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUid(document.getId());
                            users.add(user);
                        }
                    }
                    usersLiveData.setValue(users);
                })
                .addOnFailureListener(e -> usersLiveData.setValue(new ArrayList<>()));
        
        return usersLiveData;
    }
    
    // Get users by office
    public MutableLiveData<List<User>> getUsersByOffice(String officeId) {
        MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>();
        
        usersCollection.whereEqualTo("officeId", officeId)
                .whereEqualTo("isApproved", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUid(document.getId());
                            users.add(user);
                        }
                    }
                    usersLiveData.setValue(users);
                })
                .addOnFailureListener(e -> usersLiveData.setValue(new ArrayList<>()));
        
        return usersLiveData;
    }
    
    // Update user's location
    public void updateUserLocation(String uid, double latitude, double longitude) {
        DocumentReference userRef = usersCollection.document(uid);
        
        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            user.updateLocation(latitude, longitude);
                            userRef.update("location", user.getLocation());
                        }
                    }
                });
    }

    /**
     * Get all employees (users with role "employee")
     * @return LiveData containing list of employee users
     */
    public MutableLiveData<List<User>> getAllEmployees() {
        MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>();
        
        firestore.collection(USERS_COLLECTION)
                .whereEqualTo("role", "employee")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> employees = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUid(document.getId());
                        employees.add(user);
                    }
                    usersLiveData.setValue(employees);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting employees: ", e);
                    usersLiveData.setValue(new ArrayList<>());
                });
        
        return usersLiveData;
    }
    
    /**
     * Get all users
     * @return LiveData containing list of all users
     */
    public MutableLiveData<List<User>> getAllUsers() {
        MutableLiveData<List<User>> usersLiveData = new MutableLiveData<>();
        
        usersCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUid(document.getId());
                        users.add(user);
                    }
                    usersLiveData.setValue(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting all users: ", e);
                    usersLiveData.setValue(new ArrayList<>());
                });
        
        return usersLiveData;
    }
    
    /**
     * Save or update a user
     * @param user User to save
     * @param callback Callback to handle result
     */
    public void saveUser(User user, OnCompleteListener<Void> callback) {
        if (user.getUid() == null || user.getUid().isEmpty()) {
            // This is a new user, generate an ID
            String userId = usersCollection.document().getId();
            user.setUid(userId);
        }
        
        usersCollection.document(user.getUid())
                .set(user.toMap())
                .addOnCompleteListener(callback);
    }
    
    /**
     * Delete a user
     * @param userId ID of user to delete
     * @param callback Callback to handle result
     */
    public void deleteUser(String userId, OnCompleteListener<Void> callback) {
        usersCollection.document(userId)
                .delete()
                .addOnCompleteListener(callback);
    }
    
    /**
     * Get users by department
     * @param departmentId ID of department
     * @param callback Callback with list of users
     */
    public void getUsersByDepartment(String departmentId, OnCompleteListener<List<User>> callback) {
        usersCollection.whereEqualTo("departmentId", departmentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUid(document.getId());
                        users.add(user);
                    }
                    Task<List<User>> task = Tasks.forResult(users);
                    callback.onComplete(task);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting users by department: ", e);
                    Task<List<User>> task = Tasks.forException(e);
                    callback.onComplete(task);
                });
    }
    
    /**
     * Get users by team
     * @param teamId ID of team
     * @param callback Callback with list of users
     */
    public void getUsersByTeam(String teamId, OnCompleteListener<List<User>> callback) {
        usersCollection.whereEqualTo("teamId", teamId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        user.setUid(document.getId());
                        users.add(user);
                    }
                    Task<List<User>> task = Tasks.forResult(users);
                    callback.onComplete(task);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting users by team: ", e);
                    Task<List<User>> task = Tasks.forException(e);
                    callback.onComplete(task);
                });
    }
}