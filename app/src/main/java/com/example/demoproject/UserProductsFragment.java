package com.example.demoproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.room.Room;

import com.example.demoproject.MyProductsModel.MyProduct;
import com.example.demoproject.MyProductsModel.MyProductDatabase;
import com.example.demoproject.model.Product;
import com.example.demoproject.model.ProductDatabase;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProductsFragment extends Fragment {
    private ListView productListView;
    private MyProductDatabase myProductDB;

    private ProductDatabase productDB;
    private ExecutorService executorService;
    private Handler handler;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;

    private NavigationView navigationView;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 101;
    private MyProductsAdapter myProductsAdapter;


    ;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    public ActivityResultLauncher<Intent> getCameraLauncher() {
        return cameraLauncher;
    }

    public ActivityResultLauncher<Intent> getGalleryLauncher() {
        return galleryLauncher;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_product_list, container, false);
        initializeViews(view);
        setupToolbar();
        setupDrawer();
        setupNavigationView();
        productListView = view.findViewById(R.id.user_prodcut_list_userproductListView);
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        try {
            productDB = Room.databaseBuilder(requireActivity().getApplicationContext(), ProductDatabase.class
                    , "ProductDB").build();
        } catch (Exception e) {
            Log.e("TAG", "Error loading product list", e);
        }

        loadProductList();


        return view;
    }

    private void loadProductList() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "No user is signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // Assume your Firestore database reference is db
        db.collection("groups")
                .whereArrayContains("users", currentUser.getEmail())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        List<Product> productList = documentSnapshot.toObject(Group.class).getProducts();
                        List<Product> userProducts = new LinkedList<>();
                        for (Product product : productList) {
                            // Check if the product belongs to the current user's group
                            if (product.getUserId().equals(userId)) {
                                userProducts.add(product);
                            }
                        }
                        myProductsAdapter = new MyProductsAdapter(requireContext(), productList);

                        handler.post(() -> {
                            MyProductsAdapter adapter = new MyProductsAdapter(requireContext(), userProducts);
                            productListView.setAdapter(adapter);
                            Log.d("TAG", "User product list loaded successfully and the size is " + userProducts.size());
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    handler.post(() -> Toast.makeText(requireContext(), "Failed to load user products", Toast.LENGTH_SHORT).show());
                    Log.e("TAG", "Error loading user product list", e);
                });

    }


    private void initializeViews(View view) {
        drawerLayout = view.findViewById(R.id.myproduct_drawer_layout);
        toolbar = view.findViewById(R.id.change_password_list_toolbar);
        navigationView = view.findViewById(R.id.myprodcut_list_navigation_view);
    }


    private void setupToolbar() {
        toolbar.setTitle("Product List");
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void setupDrawer() {
        drawerToggle = new ActionBarDrawerToggle(requireActivity(), drawerLayout, toolbar,
                R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    private void setupNavigationView() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int hamburgerButton = item.getItemId();
            if (hamburgerButton == R.id.menu_item1) {
                Navigation.findNavController(requireView()).navigate(R.id.action_userProductsFragment_to_productListFragment);
                drawerLayout.closeDrawers();
            }
            if (hamburgerButton == R.id.menu_item2)
                navigateToAddProduct();
            if (hamburgerButton == R.id.menu_item3)
                navigateToUserProducts();
            if (hamburgerButton == R.id.menu_item4)
                navigateToEditUserProfile();
            if(hamburgerButton == R.id.menu_item6){
                navigateToRecipes();
            }
            if (hamburgerButton == R.id.menu_item5)
                signOut();

            return true;
        });
    }
    private void navigateToRecipes() {
        Navigation.findNavController(requireView()).navigate(R.id.action_userProductsFragment_to_recipesFragment);
        drawerLayout.closeDrawers();
    }

    private void navigateToAddProduct() {
        Navigation.findNavController(requireView()).navigate(R.id.action_userProductsFragment_to_addProductFragment);
        drawerLayout.closeDrawers();
    }

    private void navigateToUserProducts() {
        if (productDB != null) {
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executorService.execute(() -> {
                try {
                    handler.post(() -> {
                        drawerLayout.closeDrawers();
                    });
                } catch (Exception e) {
                    handler.post(() -> Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show());
                    Log.e("TAG", "Error loading product list", e);
                }
            });
        } else {
            Toast.makeText(requireContext(), "Database not initialized", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToEditUserProfile() {
        Navigation.findNavController(requireView()).navigate(R.id.action_userProductsFragment_to_editProfileFragment);
        drawerLayout.closeDrawers();
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Navigation.findNavController(requireView()).navigate(R.id.action_userProductsFragment_to_loginFragment);
        drawerLayout.closeDrawers();
    }


    private Uri getImageUri(Context context, Bitmap bitmap) {
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, "Image", null);
        return Uri.parse(path);
    }
}