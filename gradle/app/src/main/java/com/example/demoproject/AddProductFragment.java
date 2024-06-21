package com.example.demoproject;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.demoproject.MyProductsModel.MyProduct;
import com.example.demoproject.MyProductsModel.MyProductDatabase;
import com.example.demoproject.model.Product;
import com.example.demoproject.model.ProductDatabase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.content.Intent;
import android.provider.MediaStore;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import android.Manifest;




public class AddProductFragment extends Fragment {

    private static final int REQUEST_GALLERY = 1;
    private static final int REQUEST_CAMERA = 2;

    private EditText nameEditText;
    private EditText amountEditText;
    private Button saveButton;
    private ProductDatabase productDB;
    private FirebaseFirestore firestore;
    private MyProductDatabase myProductDB;

    private ImageView productImageView;
    private FirebaseAuth mAuth;

    private Uri selectedImageUri;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_add, container, false);
        nameEditText = view.findViewById(R.id.product_name_EditText);
        amountEditText = view.findViewById(R.id.amount_edit_text);
        saveButton = view.findViewById(R.id.saveButton);
        productImageView = view.findViewById(R.id.productImageView);

       // Add this in your Application class or main activity
        FirebaseApp.initializeApp(getContext());

        firestore = FirebaseFirestore.getInstance();
        try {
            productDB = Room.databaseBuilder(requireActivity().getApplicationContext(), ProductDatabase.class, "ProductDB")
                    .build();
        } catch (Exception e) {
            Log.d("Tag", e.toString());
        }

        mAuth = FirebaseAuth.getInstance();

        saveButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString();
            String amount = amountEditText.getText().toString();

            if (selectedImageUri == null) {
                Log.e("AddProduct", "No image selected");
                return;
            }

            Product product = new Product(selectedImageUri.toString(), name, amount,
                    mAuth.getCurrentUser().getUid(),UUID.randomUUID().toString());

            insertProductToRoom(product);
            addProductInBackground(product, selectedImageUri);
        });
        Button addFromGalleryButton = view.findViewById(R.id.addFromGalleryButton);
        addFromGalleryButton.setOnClickListener(v -> {
            openGallery();
        });
        Button capturePhotoButton = view.findViewById(R.id.capturePhotoButton);
        capturePhotoButton.setOnClickListener(v -> {
            openCamera();
        });

        return view;
    }

    private void openGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_GALLERY);

        } else {
            // Permission is already granted, proceed with opening the gallery
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, REQUEST_GALLERY);
        }
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        } else {
            // Permission already granted, proceed with opening the camera
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, proceed with opening the camera
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, REQUEST_CAMERA);
            } else {
                // Camera permission denied, show a message or handle accordingly
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_GALLERY) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with opening the gallery
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, REQUEST_GALLERY);
            } else {
                // Permission denied, show a message or handle accordingly
                Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_GALLERY) {
                // Handle image selected from gallery
                selectedImageUri = data.getData();
                // Load the selected image into an ImageView using Picasso
                Picasso.get().load(selectedImageUri).into(productImageView);
                // Set the tag to the image URL
                productImageView.setTag(selectedImageUri.toString());
            } else if (requestCode == REQUEST_CAMERA) {
                // Handle image captured from camera
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                // Convert bitmap to URI
                selectedImageUri = getImageUri(requireActivity(), imageBitmap);
                // Load the captured image into an ImageView using Picasso
                Picasso.get().load(selectedImageUri).into(productImageView);
                // Set the tag to the image URL
                productImageView.setTag(selectedImageUri.toString());
            }
        }
    }

    private Uri getImageUri(Context context, Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Image", null);
        return Uri.parse(path);
    }

    public void addProductInBackground(Product product, Uri imageUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("Auth", "User is not authenticated");
            return;
        }

        // Check if the image URI is valid
        if (imageUri == null) {
            Log.e("AddProduct", "Invalid image URI");
            return;
        }

        String userId = user.getUid();
        product.setUid(userId);



        // Validate the product fields before adding to Firestore
        if (!isValidProduct(product)) {
            Log.e("TAG", "Invalid product fields, not adding to Firestore");
            // Handle invalid product fields here (e.g., display an error message)
            return;
        }

        // Get the group ID for the user
        getGroupIdForUser(user.getEmail(), new GroupIdCallback() {
            @Override
            public void onGroupIdRetrieved(String groupId) {
                if (groupId != null) {
                    // Create a new product map
                    Map<String, Object> newProduct = new HashMap<>();
                    newProduct.put("productId",product.getProductId() ); // Use the generated ID
                    newProduct.put("name", product.getName());
                    newProduct.put("amount", product.getAmount());// Set the image URL directly
                    newProduct.put("image", imageUri.toString());
                    newProduct.put("uid", product.getUid());
                    newProduct.put("userId", product.getUid());

                    // Reference to the group document
                    DocumentReference groupRef = FirebaseFirestore.getInstance().collection("groups").document(groupId);

                    // Add the new product to the products array in Firestore
                    groupRef.update("products", FieldValue.arrayUnion(newProduct))
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Firestore", "New product added to Firestore");
                                updateProductImage(groupId, product.getProductId(), imageUri.toString());
                                Navigation.findNavController(requireView()).popBackStack();
                            })
                            .addOnFailureListener(e -> Log.e("Firestore", "Error adding new product to Firestore", e));
                } else {
                    Log.e("Firestore", "Failed to retrieve group ID for user: " + user.getEmail());
                }
            }
        });

    }
    private String getImagePath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            String imagePath = cursor.getString(columnIndex);
            cursor.close();
            return imagePath;
        }
        return null;
    }

    public void updateProductImage(String groupId, String productId, String newImageUrl) {
        // Convert newImageUrl (content URI) to a file path
        String imagePath = getImagePath(Uri.parse(newImageUrl));
        if (imagePath != null) {
            // Upload the image file to Firebase Storage
            uploadImageToPath(groupId, productId, imagePath);
        } else {
            Log.e("Image Upload", "Image path is null");
        }
    }

    private void uploadImageToPath(String groupId, String productId, String imagePath) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child("images/" + UUID.randomUUID().toString());

        Uri fileUri = Uri.fromFile(new File(imagePath));

        // Upload the image file directly to Firebase Storage
        imagesRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Image uploaded successfully, get download URL
                    imagesRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Use the download URL to update the product image in Firestore
                        String downloadUrl = uri.toString();
                        Log.d("Firebase Storage", "Download URL: " + downloadUrl); // Log the download URL

                        // Update Firestore document with imageUrl
                        updateFirestoreProductImage(groupId, productId, downloadUrl);
                    }).addOnFailureListener(e -> {
                        Log.e("Firebase Storage", "Error getting download URL: " + e.getMessage());
                    });
                })
                .addOnFailureListener(e -> {
                    // Handle failed upload
                    Log.e("Firebase Storage", "Error uploading image: " + e.getMessage());
                    // Log the stack trace for more details
                    e.printStackTrace();
                });
    }

    private void updateFirestoreProductImage(String groupId, String productId, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference groupRef = db.collection("groups").document(groupId);

        groupRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<Map<String, Object>> productsList = (List<Map<String, Object>>) documentSnapshot.get("products");

                if (productsList != null) {
                    for (Map<String, Object> product : productsList) {
                        String currentProductId = (String) product.get("productId");
                        if (currentProductId != null && currentProductId.equals(productId)) {
                            // Update the image URL for the matching product
                            product.put("image", imageUrl);
                            break; // Exit the loop after updating the product
                        }
                    }

                    // Update the products array in Firestore
                    groupRef.update("products", productsList)
                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "Product image updated"))
                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating product image", e));
                } else {
                    Log.e("Firestore", "Products list is null or empty");
                }
            } else {
                Log.e("Firestore", "Group document does not exist");
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "Error getting group document", e));
    }


    private void uploadImageToPath(String groupId, String productId, File imageFile) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("images/" + UUID.randomUUID().toString());

        // Upload the image file to Firebase Storage
        imageRef.putFile(Uri.fromFile(imageFile))
                .addOnSuccessListener(taskSnapshot -> {
                    // Image uploaded successfully, get download URL
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Use the download URL to update the product image in Firestore
                        String downloadUrl = uri.toString();
                        Log.d("Firebase Storage", "Download URL: " + downloadUrl); // Log the download URL

                        // Update Firestore document with imageUrl
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        DocumentReference groupRef = db.collection("groups").document(groupId);

                        groupRef.get().addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                List<Map<String, Object>> productsList = (List<Map<String, Object>>) documentSnapshot.get("products");

                                if (productsList != null) {
                                    for (Map<String, Object> product : productsList) {
                                        String currentProductId = (String) product.get("productId");
                                        if (currentProductId != null && currentProductId.equals(productId)) {
                                            // Update the image URL for the matching product
                                            product.put("image", downloadUrl);
                                            break; // Exit the loop after updating the product
                                        }
                                    }

                                    // Update the products array in Firestore
                                    groupRef.update("products", productsList)
                                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "Product image updated"))
                                            .addOnFailureListener(e -> Log.e("Firestore", "Error updating product image", e));
                                } else {
                                    Log.e("Firestore", "Products list is null or empty");
                                }
                            } else {
                                Log.e("Firestore", "Group document does not exist");
                            }
                        }).addOnFailureListener(e -> Log.e("Firestore", "Error getting group document", e));
                    }).addOnFailureListener(e -> {
                        Log.e("Firebase Storage", "Error getting download URL: " + e.getMessage());
                    });
                })
                .addOnFailureListener(e -> {
                    // Handle failed upload
                    Log.e("Firebase Storage", "Error uploading image: " + e.getMessage());
                    // Log the stack trace for more details
                    e.printStackTrace();
                });
    }

    private boolean isValidProduct(Product product) {
        return product.getName() != null && !product.getName().isEmpty() &&
                product.getAmount() != null && !product.getAmount().isEmpty();
    }

    private void getGroupIdForUser(String email, GroupIdCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("groups").whereArrayContains("users", email).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    String id = document.getId();
                    callback.onGroupIdRetrieved(id);
                    return;
                }
                callback.onGroupIdRetrieved(null); // No group found
            } else {
                Log.e("Firestore", "Error getting group ID", task.getException());
                callback.onGroupIdRetrieved(null); // Error occurred
            }
        });
    }

    private void insertProductToRoom(Product product) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            productDB.getproductDao().addProduct(product);
            Log.d("TAG", "Product inserted into Room database: " + product.getName());
        });
    }

    public interface GroupIdCallback {
        void onGroupIdRetrieved(String groupId);
    }
}
