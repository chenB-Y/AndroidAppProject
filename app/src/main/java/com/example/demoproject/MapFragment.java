package com.example.demoproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.demoproject.R;
import com.example.demoproject.model.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFragment extends Fragment {

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private List<Product> productList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Initialize the map view
        mapView = view.findViewById(R.id.mapView);
        progressBar = view.findViewById(R.id.progressBar);

        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Initialize the OSMDroid configuration
        Configuration.getInstance().load(getContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        productList = new ArrayList<>();

        // Initialize location overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // Request location permissions
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        // Show the spinner while fetching the location
        progressBar.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocation();
            }
        }
    }

    private void enableLocation() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    GeoPoint myLocation = myLocationOverlay.getMyLocation();
                    if (myLocation != null) {
                        addMarkerAtLocation(myLocation);
                        centerMapOnLocation(myLocation);
                        // Fetch product locations and add markers
                        fetchProductLocations();
                    }
                    // Hide the spinner once the location is obtained
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void addMarkerAtLocation(GeoPoint location) {
        Marker myLocationMarker = new Marker(mapView);
        myLocationMarker.setPosition(location);
        myLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        myLocationMarker.setTitle("You are here");
        mapView.getOverlays().add(myLocationMarker);
        mapView.invalidate(); // Refresh the map
        centerMapOnLocation(location);
    }

    private void centerMapOnLocation(GeoPoint location) {
        mapView.getController().setCenter(location);
        mapView.getController().setZoom(15);
    }

    private void fetchProductLocations() {
        progressBar.setVisibility(View.VISIBLE);
        String userEmail = mAuth.getCurrentUser().getEmail();
        db.collection("groups").whereArrayContains("users", userEmail).get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                DocumentSnapshot groupDoc = queryDocumentSnapshots.getDocuments().get(0);
                String groupId = groupDoc.getId();
                if (groupId != null) {
                    db.collection("groups").document(groupId).get().addOnSuccessListener(groupDocument -> {
                        if (groupDocument.exists()) {
                            List<Map<String, Object>> productsList = (List<Map<String, Object>>) groupDocument.get("products");
                            if (productsList != null && !productsList.isEmpty()) {
                                Map<String, List<Product>> productsMap = new HashMap<>();
                                for (Map<String, Object> product : productsList) {
                                    String productName = (String) product.get("name");
                                    Double latitude = (Double) product.get("latitude");
                                    Double longitude = (Double) product.get("longitude");
                                    String productId = (String) product.get("productId");
                                    if (productName != null && latitude != null && longitude != null && productId != null) {
                                        Product newProduct = new Product("", productName, "", userEmail, productId);
                                        GeoPoint productLocation = new GeoPoint(latitude, longitude);
                                        String locationKey = productLocation.toString(); // Using location as the key
                                        if (!productsMap.containsKey(locationKey)) {
                                            productsMap.put(locationKey, new ArrayList<>());
                                        }
                                        productsMap.get(locationKey).add(newProduct);
                                        // Add markers for each location with multiple products
                                        addProductMarker(locationKey, productsMap.get(locationKey));
                                    }
                                }
                                progressBar.setVisibility(View.GONE);

                                mapView.invalidate(); // Refresh the map after adding all product markers
                            } else {
                                Log.e("FetchProducts", "No products found in the group.");
                            }
                        } else {
                            Log.e("FetchProducts", "Group document does not exist.");
                        }
                    }).addOnFailureListener(e -> {
                        Log.e("FetchProducts", "Error getting group document: ", e);
                    });
                } else {
                    Log.e("FetchProducts", "Group ID is null.");
                }
            } else {
                Log.e("FetchProducts", "No user data found in any group.");
            }
        }).addOnFailureListener(e -> {
            Log.e("FetchProducts", "Error fetching group data: ", e);
        });
    }



    private void addProductMarker(String locationKey, List<Product> products) {
        // Convert locationKey back to GeoPoint
        GeoPoint productLocation = GeoPointFromString(locationKey);
        if (productLocation != null) {
            Marker productMarker = new Marker(mapView);
            productMarker.setPosition(productLocation);
            productMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            StringBuilder titleBuilder = new StringBuilder();
            for (Product product : products) {
                titleBuilder.append(product.getName()).append("\n");
            }
            productMarker.setTitle(titleBuilder.toString());
            mapView.getOverlays().add(productMarker);
        }
    }

    private GeoPoint GeoPointFromString(String locationKey) {
        String[] parts = locationKey.split(",");
        if (parts.length >= 2) {
            try {
                double latitude = Double.parseDouble(parts[0]);
                double longitude = Double.parseDouble(parts[1]);
                return new GeoPoint(latitude, longitude);
            } catch (NumberFormatException e) {
                Log.e("GeoPointFromString", "Error parsing GeoPoint from string: " + locationKey);
            }
        }
        return null;
    }
}
