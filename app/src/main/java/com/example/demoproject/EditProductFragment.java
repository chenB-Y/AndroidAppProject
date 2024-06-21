package com.example.demoproject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.demoproject.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EditProductFragment extends Fragment {

    private EditText editProductName, editProductAmount;
    private Button buttonSaveChanges, buttonSelectImage ,buttonSelectImageCamera;
    private ImageView imageViewProduct;

    private Product selectedProduct;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Uri imageUri;

    private String currentPhotoPath;

    private static final int GALLERY_REQUEST_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;

    private FrameLayout progressOverlay;

    public EditProductFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Initialize views
        editProductName = view.findViewById(R.id.edit_text_product_name);
        editProductAmount = view.findViewById(R.id.edit_text_product_amount);
        buttonSaveChanges = view.findViewById(R.id.button_save_changes);
        buttonSelectImage = view.findViewById(R.id.button_select_image);
        buttonSelectImageCamera = view.findViewById(R.id.button_select_image_camera);
        imageViewProduct = view.findViewById(R.id.imageViewEditProduct);
        progressOverlay = view.findViewById(R.id.progress_overlay);

        // Get the selected product from arguments
        if (getArguments() != null) {
            selectedProduct = EditProductFragmentArgs.fromBundle(getArguments()).getSelectedProduct();
        }

        // Set existing values to the EditText fields
        if (selectedProduct != null) {
            editProductName.setText(selectedProduct.getName());
            editProductAmount.setText(String.valueOf(selectedProduct.getAmount()));
        }

        // Set click listener for select image button
        buttonSelectImage.setOnClickListener(v -> {
            // Open image picker dialog
            openImagePicker();
        });

        // Set click listener for select image from camera button
        buttonSelectImageCamera.setOnClickListener(v -> openCamera());

        // Set click listener for save changes button
        buttonSaveChanges.setOnClickListener(v ->{
            showProgressOverlay();
            saveChanges();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_REQUEST_CODE);
    }
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(), "com.example.demoproject.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(null);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                imageViewProduct.setImageURI(imageUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            imageUri = Uri.fromFile(new File(currentPhotoPath));
            try {
                imageViewProduct.setImageURI(imageUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveChanges() {
        // Get updated values from EditText fields
        String newName = editProductName.getText().toString().trim();
        String newAmountStr = editProductAmount.getText().toString().trim();

        // Validate input
        if (newName.isEmpty() || newAmountStr.isEmpty() || imageUri == null) {
            // Show error message or toast for empty fields
            return;
        }

        double newAmount = Double.parseDouble(newAmountStr);

        // Update the selected product object
        if (selectedProduct != null) {
            selectedProduct.setName(newName);
            selectedProduct.setAmount(String.valueOf(newAmount));

            // Get the current user's ID
            String email = auth.getCurrentUser().getEmail();

            // Query Firestore to get the group ID for the current user
            db.collection("groups")
                    .whereArrayContains("users", email)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {


                            String groupId = documentSnapshot.getId();

                            // Find the old product in the products array
                            List<Map<String, Object>> productsList = (List<Map<String, Object>>) documentSnapshot.get("products");
                            Map<String, Object> oldProduct = productsList.stream().filter(productData -> productData.get("productId").equals(selectedProduct.getProductId())).findFirst().orElse(null);

                            if (oldProduct == null) {
                                // Old product not found, show error message
                                Toast.makeText(requireContext(), "Old product not found", Toast.LENGTH_SHORT).show();
                                return;
                            }


                            // Upload image to Firebase Storage
                            StorageReference imageRef = storageRef.child("images/" + UUID.randomUUID().toString());
                            imageRef.putFile(imageUri)
                                    .addOnSuccessListener(taskSnapshot -> {
                                        // Image uploaded successfully, get the download URL
                                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                            // Update product details with the image URL
                                            selectedProduct.setImage(uri.toString());

                                            Map<String, Object> newProduct = new HashMap<>();
                                            newProduct.put("productId",selectedProduct.getProductId() ); // Use the generated ID
                                            newProduct.put("name", selectedProduct.getName());
                                            newProduct.put("amount", selectedProduct.getAmount());// Set the image URL directly
                                            newProduct.put("image", selectedProduct.getImage());
                                            newProduct.put("uid", selectedProduct.getUid());
                                            newProduct.put("userId", selectedProduct.getUid());


                                            // Update product details in Firestore

                                            // Remove the old product and add the updated product in a single batch operation
                                            WriteBatch batch = db.batch();
                                            DocumentReference groupRef = db.collection("groups").document(groupId);
                                            batch.update(groupRef, "products", FieldValue.arrayRemove(oldProduct));
                                            batch.update(groupRef, "products", FieldValue.arrayUnion(newProduct));

                                            batch.commit()
                                                    .addOnSuccessListener(aVoid -> {
                                                        // Product updated successfully
                                                        Toast.makeText(requireContext(), "Product updated successfully", Toast.LENGTH_SHORT).show();
                                                        // Navigate to userProduct fragment
                                                        hideProgressOverlay();
                                                        Navigation.findNavController(requireView()).navigate(R.id.action_editProductFragment_to_userProductsFragment);

                                                        // Get the old image URL
                                                        String oldImageUrl = (String) oldProduct.get("image");

                                                        // Delete the old image from Firebase Storage
                                                        deleteImageWithUrl(oldImageUrl);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        // Error updating product
                                                        hideProgressOverlay();
                                                        Toast.makeText(requireContext(), "Error updating product", Toast.LENGTH_SHORT).show();
                                                    });
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        // Error uploading image
                                        hideProgressOverlay();
                                        Toast.makeText(requireContext(), "Error uploading image", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Error getting group ID for user
                        hideProgressOverlay();
                        Toast.makeText(requireContext(), "Error getting group ID", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void deleteImageWithUrl(String imageUrl) {
        // Check if the image URL is not empty
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Get the image file name from the URL
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

            // Create a reference to the image in Firebase Storage
            StorageReference imageRef = storageRef.child("images/" + fileName);

            // Delete the image
            imageRef.delete()
                    .addOnSuccessListener(voidValue -> {
                        // Old image deleted successfully
                        Log.d("EditProductFragment", "Old image deleted successfully");
                    })
                    .addOnFailureListener(e -> {
                        // Error deleting old image
                        Log.e("EditProductFragment", "Error deleting old image: " + e.getMessage());
                    });
        }
    }

    private void showProgressOverlay() {
        progressOverlay.setVisibility(View.VISIBLE);
    }

    private void hideProgressOverlay() {
        progressOverlay.setVisibility(View.GONE);
    }
}

