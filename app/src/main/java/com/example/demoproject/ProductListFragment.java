package com.example.demoproject;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.room.Room;

import com.example.demoproject.model.Product;
import com.example.demoproject.model.ProductDatabase;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductListFragment extends Fragment implements ProductAdapter.OnPermissionGrantedListener{

    private ListView productListView;
    private ProductDatabase productDB;
    private Button addProductButton;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private Toolbar toolbar;
    private NavigationView navigationView;
    private FirebaseFirestore db;

    private Button mapButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_list, container, false);
        initializeViews(view);
        setupToolbar();
        setupDrawer();
        setupNavigationView();

        db = FirebaseFirestore.getInstance();

        addProductButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_productListFragment_to_addProductFragment));

        initializeDatabase();
        mapButton.setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_productListFragment_to_mapFragment));

        // Check if user is authenticated
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            loadProductList();
        } else {
            // If not authenticated, navigate to login fragment
            Navigation.findNavController(requireView()).navigate(R.id.action_productListFragment_to_loginFragment);
        }

        return view;
    }

    private void initializeViews(View view) {
        productListView = view.findViewById(R.id.user_prodcut_list_userproductListView);
        addProductButton = view.findViewById(R.id.prodcut_list_addProductButton);
        drawerLayout = view.findViewById(R.id.myproduct_drawer_layout);
        toolbar = view.findViewById(R.id.change_password_list_toolbar);
        navigationView = view.findViewById(R.id.myprodcut_list_navigation_view);
        mapButton = view.findViewById(R.id.Map_button_product_list);
    }

    public void onPermissionGranted(Uri photoUri) {
        // Handle permission granted, for example, load the product list
        loadProductList();
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
            if (hamburgerButton == R.id.menu_item1)
                drawerLayout.closeDrawers();
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
        Navigation.findNavController(requireView()).navigate(R.id.action_productListFragment_to_recipesFragment);
        drawerLayout.closeDrawers();
    }


    private void navigateToAddProduct() {
        Navigation.findNavController(requireView()).navigate(R.id.action_productListFragment_to_addProductFragment);
        drawerLayout.closeDrawers();
    }

    private void navigateToUserProducts() {
        Navigation.findNavController(requireView()).navigate(R.id.action_productListFragment_to_userProductsFragment);
        drawerLayout.closeDrawers();
    }

    private void navigateToEditUserProfile() {
        Navigation.findNavController(requireView()).navigate(R.id.action_productListFragment_to_editProfileFragment);
        drawerLayout.closeDrawers();
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        Navigation.findNavController(requireView()).navigate(R.id.action_productListFragment_to_loginFragment);
        drawerLayout.closeDrawers();
    }

    private void initializeDatabase() {
        productDB = Room.databaseBuilder(requireActivity().getApplicationContext(),
                ProductDatabase.class, "productDB").build();
    }

    private void loadProductList() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() != null) {
            String email = firebaseAuth.getCurrentUser().getEmail();
            Log.d("TAG", "Fetching group ID for user: " + email); // Log fetching group ID
            getGroupIdForUser(email, groupId -> {
                if (groupId != null) {
                    Log.d("TAG", "Group ID retrieved: " + groupId); // Log retrieved group ID
                    // Load products for the group
                    loadProductsForGroup(groupId);
                } else {
                    Log.e("TAG", "Failed to retrieve group ID for user: " + email);
                    Toast.makeText(requireContext(), "Failed to get group ID", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e("TAG", "User is not authenticated");
            Toast.makeText(requireContext(), "User is not authenticated", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProductsForGroup(String groupId) {
        db.collection("groups").document(groupId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Get the "products" array from the document snapshot
                            List<Product> productList = new LinkedList<>();
                            List<Map<String, Object>> products = (List<Map<String, Object>>) document.get("products");
                            if (products != null) {
                                // Parse each product in the array
                                for (Map<String, Object> productMap : products) {
                                    try {
                                        // Convert the productMap to a Product object
                                        String name = (String) productMap.get("name");
                                        String amount = (String) productMap.get("amount");
                                        String image = (String) productMap.get("image");
                                        String productId = (String) productMap.get("productId");
                                        String userId = (String) productMap.get("userId");
                                        // Create a Product object
                                        Product product = new Product(image, name, amount,userId , productId); // Assuming userId is not retrieved from Firestore
                                        // Add the product to the list
                                        productList.add(product);
                                    } catch (Exception e) {
                                        Log.e("TAG", "Error parsing product", e);
                                    }
                                }
                                // Update UI with the fetched product list
                                requireActivity().runOnUiThread(() -> {
                                    // Create the adapter
                                    ProductAdapter adapter = new ProductAdapter(requireContext(), productList, this); // Pass the listener
                                    // Set the adapter on the ListView
                                    productListView.setAdapter(adapter);
                                    // Notify the adapter of data changes
                                    adapter.notifyDataSetChanged();
                                });

                                Log.d("TAG", "Number of products retrieved: " + productList.size());
                                Log.d("TAG", "Product list loaded successfully from Firestore");
                            } else {
                                Log.d("TAG", "No products found for group: " + groupId);
                                // Handle case when there are no products in the array
                                // You can display a message to the user or update UI accordingly
                            }
                        } else {
                            Log.e("TAG", "No such document");
                            // Handle case when the document does not exist
                        }
                    } else {
                        Log.e("TAG", "Error getting documents: ", task.getException());
                        Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
                    }
                });
    }





    private void getGroupIdForUser(String email, GroupIdCallback callback) {
        db.collection("groups")
                .whereArrayContains("users", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String groupId = document.getId();
                            callback.onGroupIdRetrieved(groupId);
                        } else {
                            Log.e("TAG", "No group found for user with email: " + email);
                            callback.onGroupIdRetrieved(null);
                        }
                    } else {
                        Log.e("TAG", "Error getting group ID for user with email: " + email, task.getException());
                        callback.onGroupIdRetrieved(null);
                    }
                });
    }


    private interface GroupIdCallback {
        void onGroupIdRetrieved(String groupId);
    }
}
