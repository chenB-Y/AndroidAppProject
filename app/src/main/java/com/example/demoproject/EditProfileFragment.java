package com.example.demoproject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EditProfileFragment extends Fragment {

    private EditText username;
    private Button saveUsername;
    private ImageView back;
    private Button changePassword;
    private Button saveImageButton;
    private FirebaseUser user;
    private TextView helloUserTextView;
    private ImageView currentProfileImageView;
    private FirebaseFirestore db;
    private Bitmap selectedProfileImage;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
    private ImageView changeProfileImageView;
    private FrameLayout progressOverlay;
    private boolean isImageChanged = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        username = view.findViewById(R.id.edit_profile_userNameEditText);
        saveUsername = view.findViewById(R.id.edit_profile_saveUserName);
        back = view.findViewById(R.id.edit_profile_backButton);
        changePassword = view.findViewById(R.id.edit_profile_changePassword);
        helloUserTextView = view.findViewById(R.id.Hello_user_textView2);
        currentProfileImageView = view.findViewById(R.id.Current_prophile_imageView2);
        saveImageButton = view.findViewById(R.id.save_Picture_buttton_edit_profile);
        changeProfileImageView = view.findViewById(R.id.edit_prophile_imageView);
        progressOverlay = view.findViewById(R.id.progress_overlay);

        user = FirebaseAuth.getInstance().getCurrentUser();

        showProgressOverlay();
        getUserInfo();
        saveImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isImageChanged) {
                    showProgressOverlay();
                    updateProfilePicture(selectedProfileImage);
                } else {
                    Toast.makeText(requireContext(), "You haven't changed the picture", Toast.LENGTH_SHORT).show();
                }
            }
        });

        changeProfileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSelectionDialog();
            }
        });
        saveUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPasswordInputDialog();
            }
        });
        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.action_editProfileFragment_to_changePasswordFragment);
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        return view;
    }

    private void showImageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Choose Image Source");
        builder.setItems(new CharSequence[]{"Camera", "Gallery"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        dispatchTakePictureIntent();
                        break;
                    case 1:
                        pickImageFromGallery();
                        break;
                }
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void pickImageFromGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Handle image captured from camera
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                if (imageBitmap != null) {
                    this.selectedProfileImage = imageBitmap;
                    isImageChanged = true;  // Mark image as changed
                    // Convert Bitmap to File
                    File imageFile = bitmapToFile(imageBitmap);
                    if (imageFile != null) {
                        // Load image using Picasso from File
                        Picasso.get().load(imageFile).into(changeProfileImageView);
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_PICK) {
                // Handle image picked from gallery
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    try {
                        Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImage);
                        this.selectedProfileImage = imageBitmap;
                        isImageChanged = true;  // Mark image as changed
                        // Convert Bitmap to File
                        File imageFile = bitmapToFile(imageBitmap);
                        if (imageFile != null) {
                            // Load image using Picasso from File
                            Picasso.get().load(imageFile).into(changeProfileImageView);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private File bitmapToFile(Bitmap bitmap) {
        try {
            // Create a file to store the bitmap
            File file = new File(requireActivity().getCacheDir(), "temp_image.jpg");
            file.createNewFile();

            // Convert bitmap to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            byte[] bitmapData = bos.toByteArray();

            // Write the bytes in file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bitmapData);
            fos.flush();
            fos.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void updateProfilePicture(Bitmap imageBitmap) {
        // Convert Bitmap to ByteArray
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageData = baos.toByteArray();

        // Get a reference to the old profile image in Firebase Storage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference oldProfileImageRef = storageRef.child("profile_photos/" + user.getUid() + ".jpg");

        // Delete the old profile image
        oldProfileImageRef.delete().addOnSuccessListener(aVoid -> {
            // Upload the new profile image to Firebase Storage
            StorageReference profileImageRef = storageRef.child("profile_photos/" + user.getUid() + ".jpg");
            profileImageRef.putBytes(imageData)
                    .addOnSuccessListener(taskSnapshot -> {
                        // Image uploaded successfully, get download URL
                        profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            // Update profile picture URL in Firestore
                            db.collection("users").document(user.getUid())
                                    .update("photo", downloadUrl)
                                    .addOnSuccessListener(aVoid1 -> {
                                        // Profile picture URL updated successfully
                                        hideProgressOverlay();
                                        Picasso.get().load(downloadUrl).into(currentProfileImageView);
                                        Toast.makeText(requireContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                                        isImageChanged = false; // Reset image changed flag
                                    })
                                    .addOnFailureListener(e -> {
                                        // Failed to update profile picture URL
                                        hideProgressOverlay();
                                        Log.e("EditProfileFragment", "Error updating profile picture URL", e);
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        // Image upload failed
                        hideProgressOverlay();
                        Log.e("EditProfileFragment", "Error uploading image to Firebase Storage", e);
                    });
        }).addOnFailureListener(e -> {
            // Failed to delete old profile image
            hideProgressOverlay();
            Log.e("EditProfileFragment", "Error deleting old profile image", e);
        });
    }

    private void getUserInfo() {
        if (user != null) {
            String email = user.getEmail();
            if (email != null) {
                // Query Firestore to get user info based on email
                db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful() && task.getResult() != null) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        String username = document.getString("username");
                                        String photoUrl = document.getString("photo");

                                        // Update UI with username and photo
                                        updateUserInfo(username, photoUrl);
                                        hideProgressOverlay();
                                    }
                                } else {
                                    Log.e("EditProfileFragment", "Error getting user info from Firestore", task.getException());
                                }
                            }
                        });
            }
        }
    }

    private void updateUserInfo(String username, String photoUrl) {
        if (!TextUtils.isEmpty(username)) {
            helloUserTextView.setText("Hello, " + username);
        }

        if (!TextUtils.isEmpty(photoUrl)) {
            Picasso.get().load(photoUrl).into(currentProfileImageView);
        }
    }

    private void showPasswordInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.password_dialog, null);
        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);

        builder.setView(dialogView)
                .setTitle("Enter Your Password")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String password = passwordInput.getText().toString();
                        showProgressOverlay();
                        if (!TextUtils.isEmpty(password)) {
                            reAuthenticateUser(password);
                        } else {
                            hideProgressOverlay();
                            Toast.makeText(requireContext(), "Please enter your password", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void reAuthenticateUser(String password) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Re-authentication successful, update email
                    String newUsername  = username.getText().toString();
                    db.collection("users").document(user.getUid())
                            .update("username", newUsername)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        // Username updated successfully
                                        hideProgressOverlay();
                                        Log.d("EditProfileFragment", "Username updated successfully!");
                                        Toast.makeText(requireContext(), "Username updated successfully!", Toast.LENGTH_SHORT).show();
                                        helloUserTextView.setText("Hello, " + newUsername);
                                    } else {
                                        // Failed to update username
                                        Exception error = task.getException();
                                        hideProgressOverlay();
                                        Log.e("EditProfileFragment", "Error updating username", error);
                                        Toast.makeText(requireContext(), "Error updating username: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                } else {
                    // Re-authentication failed
                    Exception error = task.getException();
                    hideProgressOverlay();
                    Log.e("EditProfileFragment", "Error re-authenticating user", error);
                    Toast.makeText(requireContext(), "Error re-authenticating user: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showProgressOverlay() {
        progressOverlay.setVisibility(View.VISIBLE);
    }

    private void hideProgressOverlay() {
        progressOverlay.setVisibility(View.GONE);
    }
}