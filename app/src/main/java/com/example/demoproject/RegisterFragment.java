package com.example.demoproject;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_PERMISSIONS = 3;

    private EditText editTextEmail, editTextPassword;
    private Button buttonRegister;
    private ImageView buttonPlusSignUploadPhoto;
    private ImageView profilePhoto;
    private EditText editTextUserName;
    private Uri photoUri;
    private FirebaseAuth firebaseAuth;

    private FrameLayout progressOverlayRegister;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        editTextEmail = view.findViewById(R.id.register_email);
        editTextPassword = view.findViewById(R.id.register_password);
        editTextUserName = view.findViewById(R.id.register_fragment_username_editext);
        buttonRegister = view.findViewById(R.id.register_button);
        profilePhoto = view.findViewById(R.id.register_fragment_profile_photo);
        buttonPlusSignUploadPhoto = view.findViewById(R.id.register_fragment_plus_sign);
        progressOverlayRegister = view.findViewById(R.id.progress_overlay_register);
        firebaseAuth = FirebaseAuth.getInstance();

        buttonPlusSignUploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkAndRequestPermissions()) {
                    showPhotoOptions();
                }
            }
        });

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressOverlay();
                registerUser(view);
            }
        });

        profilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkAndRequestPermissions()) {
                    showPhotoOptions();
                }
            }
        });

        return view;
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        boolean allPermissionsGranted = true;

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(getActivity(), permissions, REQUEST_PERMISSIONS);
        }

        return allPermissionsGranted;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showPhotoOptions();
            } else {
                Toast.makeText(getActivity(), "Permissions are required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showPhotoOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        new AlertDialog.Builder(getActivity())
                .setTitle("Select Photo")
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Take Photo
                                Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                if (takePhotoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                    try {
                                        File photoFile = createImageFile();
                                        if (photoFile != null) {
                                            photoUri = FileProvider.getUriForFile(getActivity(), "com.example.demoproject.fileprovider", photoFile);
                                            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                                            startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE);
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(getActivity(), "Error creating file for photo", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                break;
                            case 1: // Choose from Gallery
                                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                pickIntent.setType("image/*");
                                startActivityForResult(pickIntent, REQUEST_IMAGE_PICK);
                                break;
                        }
                    }
                })
                .show();
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK) {
                if (data != null && data.getData() != null) {
                    photoUri = data.getData();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // The photoUri is already set in onCreateImageFile
            }
            profilePhoto.setImageURI(photoUri);
        }
    }

    private void registerUser(View view) {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String username = editTextUserName.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            return;
        }

        if (TextUtils.isEmpty(username)) {
            editTextUserName.setError("Username is required");
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null && photoUri != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setPhotoUri(photoUri)
                                        .build();
                                user.updateProfile(profileUpdates);
                            }

                            // Save additional user data to Firestore
                            saveUserDataToFirestore(user.getUid(), username, photoUri, email);

                            Toast.makeText(getActivity(), "Registration successful", Toast.LENGTH_SHORT).show();
                            hideProgressOverlay();
                            Navigation.findNavController(view).navigate(R.id.action_registerFragment_to_loginFragment);
                        } else {
                            hideProgressOverlay();
                            Toast.makeText(getActivity(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserDataToFirestore(String userId, String username, Uri photoUri, String email) {
        // Validate input
        if (photoUri == null) {
            Log.e("TAG", "Photo URI is null");
            return;
        }

        // Save the user data to Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("photo", ""); // Placeholder for the photo URL, to be updated later
        userData.put("email", email);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Add the user data to the "users" collection in Firestore
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TAG", "User data saved to Firestore");

                    // Create a reference to Firebase Storage
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReference();

                    // Create a reference to the image file in Firebase Storage
                    StorageReference photoRef = storageRef.child("profile_photos/" + userId + ".jpg");

                    // Upload the image to Firebase Storage
                    photoRef.putFile(photoUri)
                            .addOnSuccessListener(taskSnapshot -> {
                                // Image uploaded successfully, get the download URL
                                photoRef.getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            // Update the user data with the download URL in Firestore
                                            db.collection("users").document(userId)
                                                    .update("photo", uri.toString())
                                                    .addOnSuccessListener(aVoid1 -> {
                                                        Log.d("TAG", "User photo URL updated in Firestore");
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e("TAG", "Error updating user photo URL in Firestore", e);
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("TAG", "Error getting download URL for photo", e);
                                            // Handle error getting download URL
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("TAG", "Error uploading photo to Firebase Storage", e);
                                // Handle error uploading photo
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG", "Error saving user data to Firestore", e);
                    // Handle error saving user data
                });
    }


    private void showProgressOverlay() {
        progressOverlayRegister.setVisibility(View.VISIBLE);
    }

    private void hideProgressOverlay() {
        progressOverlayRegister.setVisibility(View.GONE);
    }


}
