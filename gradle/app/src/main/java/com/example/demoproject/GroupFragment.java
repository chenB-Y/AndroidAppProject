package com.example.demoproject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.room.Room;

import com.example.demoproject.UserModel.User;
import com.example.demoproject.UserModel.UserDAO;
import com.example.demoproject.UserModel.UserDatabase;
import com.example.demoproject.model.ProductDAO;
import com.example.demoproject.model.ProductDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupFragment extends Fragment {

    private EditText groupNameEditText;
    private Button createGroupButton;
    private Button joinGroupButton;
    private FirebaseFirestore firestore;
    private ProductDAO productDAO;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group, container, false);

        // Initialize views
        groupNameEditText = view.findViewById(R.id.group_name_edit_text);
        createGroupButton = view.findViewById(R.id.create_group_button);
        joinGroupButton = view.findViewById(R.id.join_group_button);


        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize Room database
        ProductDatabase productDB = Room.databaseBuilder(requireContext(), ProductDatabase.class, "productDB").build();
        productDAO = productDB.getproductDao();

        // Set click listeners
        createGroupButton.setOnClickListener(v -> createGroup());
        joinGroupButton.setOnClickListener(v -> joinGroup());

        return view;
    }

    private void addUserToGroup(String groupId) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Log.e("GroupFragment", "Error: Current user is null");
            Toast.makeText(requireContext(), "User is not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userEmail = currentUser.getEmail();

        if (userEmail == null || userEmail.isEmpty()) {
            Log.e("GroupFragment", "Error: User email is null or empty");
            Toast.makeText(requireContext(), "User email is null or empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a user object with email, username, and profile photo
        String email= currentUser.getEmail();

        // Get reference to the group document
        DocumentReference groupRef = firestore.collection("groups").document(groupId);
        // Update the "users" field in Firestore with the new user
        groupRef.update("users", FieldValue.arrayUnion(email))
                .addOnSuccessListener(aVoid -> {
                    // User added successfully
                    Log.d("GroupFragment", "User added to group");

                    // Save the user to the Room database
                    saveUserToDatabase(email);
                })
                .addOnFailureListener(e -> {
                    // Error adding user
                    Log.e("GroupFragment", "Error adding user to group", e);
                });
    }







    private void saveUserToDatabase( String email) {
        User user = new User(email);
        UserDatabase userDatabase = UserDatabase.getInstance(getActivity());

        new InsertUserAsyncTask(userDatabase.userDao()).execute(user);
    }

    private static class InsertUserAsyncTask extends AsyncTask<User, Void, Void> {
        private UserDAO userDAO;

        InsertUserAsyncTask(UserDAO userDAO) {
            this.userDAO = userDAO;
        }

        @Override
        protected Void doInBackground(User... users) {
            userDAO.insert(users[0]);
            return null;
        }
    }


    private void createGroup() {
        String groupName = groupNameEditText.getText().toString().trim();
        if (!groupName.isEmpty()) {
            // Create a unique group ID
            String groupId = UUID.randomUUID().toString();

            // Create the group data
            Map<String, Object> groupData = new HashMap<>();
            groupData.put("name", groupName);

            // Add the users array and add the current user
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            String userEmail = firebaseAuth.getCurrentUser().getEmail();
            ArrayList<String> users = new ArrayList<>();
            users.add(userEmail);
            groupData.put("users", users);

            // Add the group to Firestore
            firestore.collection("groups").document(groupId)
                    .set(groupData)
                    .addOnSuccessListener(aVoid -> {
                        // Group created successfully
                        Toast.makeText(requireContext(), "Group created successfully", Toast.LENGTH_SHORT).show();
                        Navigation.findNavController(requireView()).navigate(R.id.action_groupFragment_to_productListFragment);
                    })
                    .addOnFailureListener(e -> {
                        // Error creating group
                        Log.e("GroupFragment", "Error creating group", e);
                        Toast.makeText(requireContext(), "Failed to create group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(requireContext(), "Please enter a group name", Toast.LENGTH_SHORT).show();
        }
    }

    private void joinGroup() {
        String groupName = groupNameEditText.getText().toString().trim();
        if (!groupName.isEmpty()) {
            // Search for the group by name in Firestore
            firestore.collection("groups")
                    .whereEqualTo("name", groupName)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Group found
                            DocumentSnapshot groupDoc = queryDocumentSnapshots.getDocuments().get(0);
                            String groupId = groupDoc.getId();
                            // Join the group by adding the current user
                            addUserToGroup(groupId);
                            Navigation.findNavController(requireView()).navigate(R.id.action_groupFragment_to_productListFragment);
                        } else {
                            // Group not found
                            Toast.makeText(requireContext(), "Group not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Error searching for group
                        Log.e("GroupFragment", "Error joining group", e);
                        Toast.makeText(requireContext(), "Failed to join group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(requireContext(), "Please enter a group name", Toast.LENGTH_SHORT).show();
        }
    }
}
