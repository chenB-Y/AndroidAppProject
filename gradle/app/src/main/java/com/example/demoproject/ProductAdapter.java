package com.example.demoproject;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.room.Room;

import com.bumptech.glide.Glide;
import com.example.demoproject.MyProductsModel.MyProduct;
import com.example.demoproject.MyProductsModel.MyProductDatabase;
import com.example.demoproject.R;
import com.example.demoproject.model.Product;
import com.example.demoproject.model.ProductDAO;
import com.example.demoproject.model.ProductDatabase;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.Manifest;


public class ProductAdapter extends BaseAdapter {
    private static final String TAG = "ProductAdapter";
    private Context context;
    private List<Product> productList;
    private LayoutInflater inflater;
    private ProductDatabase productDatabase;
    private MyProductDatabase myProductDatabase;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private ProductDAO productDAO;
    private FirebaseUser currentUser;

    // Define the permission request code
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1001;

    private OnPermissionGrantedListener permissionListener; // Callback interface

    public interface OnPermissionGrantedListener {
        void onPermissionGranted(Uri photoUri);
    }


    public ProductAdapter(Context context, List<Product> productList,OnPermissionGrantedListener listener) {
        this.context = context;
        this.productList = productList;
        this.inflater = LayoutInflater.from(context);
        this.productDatabase = Room.databaseBuilder(context.getApplicationContext(),
                ProductDatabase.class, "productDB").build();
        productDAO = productDatabase.getproductDao();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.currentUser = firebaseAuth.getCurrentUser();
        this.permissionListener = listener; // Assign the listener
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
            convertView = inflater.inflate(R.layout.list_item_product, parent, false);
            holder = new ViewHolder();
            holder.productImage = convertView.findViewById(R.id.list_item_myprdouct_fragment_productImage);
            holder.productName = convertView.findViewById(R.id.list_item_myprdouct_fragment_productName);
            holder.productAmount = convertView.findViewById(R.id.list_item_myprdouct_fragment_productAmount);
            holder.checkBox = convertView.findViewById(R.id.list_item_product_checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Product product = (Product) getItem(position);
        holder.productName.setText(product.getName());
        holder.productAmount.setText(product.getAmount());
        holder.checkBox.setChecked(product.isBought());
        holder.checkBox.setTag(position);


        //Glide.with(context).load(productList.get(position).getImage()).into(holder.productImage);
        Picasso.get().load(productList.get(position).getImage()).into(holder.productImage);


        // Check user authentication
        currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated");
            return convertView;
        }
        String uid = currentUser.getUid();

        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Retrieve the product ID from Firestore before deletion
                firestore.collection("groups").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot groupDoc : task.getResult()) {
                            List<String> users = (List<String>) groupDoc.get("users");
                            if (users != null && users.contains(firebaseAuth.getCurrentUser().getEmail())) {
                                ArrayList<Map<String, Object>> productsList = (ArrayList<Map<String, Object>>) groupDoc.get("products");
                                if (productsList != null) {
                                    Log.d("Firestore", "Before retrieval: " + productsList.toString()); // Log the products before retrieval

                                    // Iterate through the products list to find and remove the product by ID
                                    for (int i = 0; i < productsList.size(); i++) {
                                        Map<String, Object> productMap = productsList.get(i);
                                        String productId = String.valueOf(productMap.get("productId"));

                                        // Match the product ID to delete the product
                                        if (productId.equals(product.getProductId())) {
                                            productsList.remove(i);
                                            groupDoc.getReference().update("products", productsList)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d("Firestore", "Product removed successfully");
                                                    })
                                                    .addOnFailureListener(e -> Log.e("Firestore", "Error removing product: " + e.getMessage()));
                                            break;
                                        }
                                    }
                                    Log.d("Firestore", "After removal: " + productsList.toString());
                                }
                                break;
                            }
                        }
                    }
                });

                // Remove product from Room database
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        productDAO.deleteProduct(product);
                    }
                }).start();

                // Remove product from the list and update the UI
                productList.remove(position);
                notifyDataSetChanged();
            }
        });









        return convertView;
    }



    static class ViewHolder {
        ImageView productImage;
        TextView productName;
        TextView productAmount;
        CheckBox checkBox;
    }





}
