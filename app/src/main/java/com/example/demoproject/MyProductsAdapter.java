package com.example.demoproject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.example.demoproject.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class MyProductsAdapter extends BaseAdapter {
    private Context context;
    private List<Product> productList;
    private LayoutInflater inflater;


    public static Product SelectedProduct;








    public MyProductsAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.inflater = LayoutInflater.from(context);

    }

    @Override
    public int getCount() {
        return productList.size();
    }

    @Override
    public Object getItem(int position) {
        return productList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item_myproduct, parent, false);
            holder = new ViewHolder();
            holder.productName = convertView.findViewById(R.id.list_item_myprdouct_fragment_productName);
            holder.productAmount = convertView.findViewById(R.id.list_item_myprdouct_fragment_productAmount);
            holder.productImage = convertView.findViewById(R.id.list_item_myprdouct_fragment_productImage);
            holder.deleteButton = convertView.findViewById(R.id.list_item_myprdouct_fragment_deleteButton);
            holder.editButton = convertView.findViewById(R.id.list_item_myprdouct_fragment_editButton);
            holder.progressBar = convertView.findViewById(R.id.image_progress_bar);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product product = productList.get(position);
        holder.productName.setText(product.getName());
        holder.productAmount.setText(String.valueOf(product.getAmount()));
        if (product.getImage() != null && !product.getImage().isEmpty()) {
            holder.progressBar.setVisibility(View.VISIBLE);
            Picasso.get().load(product.getImage()).into(holder.productImage, new Callback() {
                @Override
                public void onSuccess() {
                    holder.progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onError(Exception e) {
                    holder.progressBar.setVisibility(View.GONE);
                    holder.productImage.setImageResource(R.drawable.ic_launcher_foreground);
                }
            });
        } else{
            holder.productImage.setImageResource(R.drawable.ic_launcher_foreground);
        }
        holder.deleteButton.setOnClickListener(v -> {
            String productId = productList.get(position).getProductId();
            deleteProductAndImage(productId);
        });
        holder.editButton.setOnClickListener(v ->{
            SelectedProduct = productList.get(position);
            navigateToEditProductFragment(SelectedProduct);
        });



        return convertView;
    }
    private void navigateToEditProductFragment(Product product) {
        // Assuming you are using Navigation Component for fragment navigation
        NavDirections action = UserProductsFragmentDirections.actionUserProductsFragmentToEditProductFragment(product);
        Navigation.findNavController(( (Activity) context).getCurrentFocus()).navigate(action);
    }





    private void deleteProductAndImage(String productId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Find the group document that contains the product
        db.collection("groups")
                .whereArrayContains("users", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            String groupId = documentSnapshot.getId();
                            List<Map<String, Object>> productsList = (List<Map<String, Object>>) documentSnapshot.get("products");

                            // Find the product in the products array
                            Map<String, Object> productToDelete = productsList.stream().filter(productData -> productId.equals(productData.get("productId"))).findFirst().orElse(null);

                            if (productToDelete != null) {
                                // Extract the image URL from the product data
                                String imageUrl = (String) productToDelete.get("image");
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    // Get the reference to the image in Firebase Storage
                                    storage.getReferenceFromUrl(imageUrl).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("MyProductsAdapter", "Image deleted from Firebase Storage");

                                                // Now delete the product from the Firestore document
                                                db.collection("groups").document(groupId)
                                                        .update("products", FieldValue.arrayRemove(productToDelete))
                                                        .addOnSuccessListener(aVoid2 -> {
                                                            Log.d("MyProductsAdapter", "Product deleted from Firestore");

                                                            // Remove the product from the list and update the UI
                                                            removeProductFromList(productId);

                                                            // Notify the user
                                                            Toast.makeText(context, "Product deleted successfully", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("MyProductsAdapter", "Error deleting product from Firestore: " + e.getMessage());
                                                            Toast.makeText(context, "Failed to delete product", Toast.LENGTH_SHORT).show();
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("MyProductsAdapter", "Error deleting image from Firebase Storage: " + e.getMessage());
                                            });
                                } else {
                                    // If there's no image URL, just delete the product from Firestore
                                    db.collection("groups").document(groupId)
                                            .update("products", FieldValue.arrayRemove(productToDelete))
                                            .addOnSuccessListener(aVoid2 -> {
                                                Log.d("MyProductsAdapter", "Product deleted from Firestore");

                                                // Remove the product from the list and update the UI
                                                removeProductFromList(productId);

                                                // Notify the user
                                                Toast.makeText(context, "Product deleted successfully", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("MyProductsAdapter", "Error deleting product from Firestore: " + e.getMessage());
                                                Toast.makeText(context, "Failed to delete product", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            } else {
                                Log.e("MyProductsAdapter", "Product not found in group");
                                Toast.makeText(context, "Product not found", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.e("MyProductsAdapter", "No groups found for user");
                        Toast.makeText(context, "No groups found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MyProductsAdapter", "Error finding groups: " + e.getMessage());
                    Toast.makeText(context, "Error finding groups", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeProductFromList(String productId) {
        // Find the index of the product with the given productId
        int indexToRemove = -1;
        for (int i = 0; i < productList.size(); i++) {
            if (productList.get(i).getProductId().equals(productId)) {
                indexToRemove = i;
                break;
            }
        }

        // Remove the product from the list if found
        if (indexToRemove != -1) {
            productList.remove(indexToRemove);
            notifyDataSetChanged(); // Notify adapter that data set has changed
        }
    }


    static class ViewHolder {
        TextView productName;
        TextView productAmount;
        ImageView productImage;
        Button deleteButton;
        Button editButton;

        ProgressBar progressBar;
    }
}