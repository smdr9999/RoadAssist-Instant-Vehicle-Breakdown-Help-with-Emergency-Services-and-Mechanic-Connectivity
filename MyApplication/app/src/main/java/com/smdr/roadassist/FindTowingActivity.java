package com.smdr.roadassist;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FindTowingActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int GPS_ENABLE_REQUEST_CODE = 2;
    private static final String TAG = "FindTowingActivity";

    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;
    private DatabaseReference towingRef;
    private RecyclerView recyclerView;
    private TowingAdapter towingAdapter;
    private List<TowingModel> towingList = new ArrayList<>();

    private ProgressBar progressBar;
    private TextView noTowingTextView, userLocationTextView;
    private Handler handler = new Handler(Looper.getMainLooper());

    // Driver details: towingServices and userId fetched from "drivers" node.
    private String driverTowingServices = "";
    private String driverId = "";

    // Reference to "drivers" node.
    private DatabaseReference driversRef;

    // Search radius in km. Initial radius set to 20.
    private double searchRadius = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_towing);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        noTowingTextView = findViewById(R.id.noTowingTextView);
        userLocationTextView = findViewById(R.id.userLocationTextView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        towingAdapter = new TowingAdapter(towingList, this);
        recyclerView.setAdapter(towingAdapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize Firebase Database references with your explicit URL.
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app");
        towingRef = database.getReference("towingServices");
        driversRef = database.getReference("drivers");

        loadDriverDetails();
    }

    // Load driver's details (ID and towing services) from the "drivers" node.
    private void loadDriverDetails() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        driverId = currentUser.getUid();
        driversRef.child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String towingServices = snapshot.child("towingServices").getValue(String.class);
                if (towingServices != null && !towingServices.isEmpty()) {
                    driverTowingServices = towingServices;
                    Log.d(TAG, "Driver towing services retrieved: " + driverTowingServices);
                } else {
                    Log.w(TAG, "Driver towing services not provided in profile.");
                    driverTowingServices = "";
                }
                checkLocationPermissionAndGPS();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(FindTowingActivity.this, "Failed to load driver details.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error loading driver details: " + error.getMessage());
                checkLocationPermissionAndGPS();
            }
        });
    }

    /** Step 1: Check permissions and GPS before proceeding */
    private void checkLocationPermissionAndGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Location permission not granted. Requesting permission.");
            requestLocationPermission();
        } else if (!isGPSEnabled()) {
            Log.d(TAG, "GPS is not enabled.");
            promptEnableGPS();
        } else {
            Log.d(TAG, "Permissions granted and GPS enabled. Fetching location.");
            getUserLocation();
        }
    }

    /** Step 2: Request Location Permission */
    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    /** Step 3: Check if GPS is enabled */
    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /** Step 4: Prompt user to enable GPS */
    private void promptEnableGPS() {
        new AlertDialog.Builder(this)
                .setTitle("Enable GPS")
                .setMessage("GPS is required to find towing services. Please enable it.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GPS_ENABLE_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> showError("GPS is required to find nearby towing services."))
                .setCancelable(false)
                .show();
    }

    /** Step 5: Get user's location */
    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showError("Location permission not granted.");
            return;
        }
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    userLocation = location;
                    Log.d(TAG, "User location found: " + location.getLatitude() + ", " + location.getLongitude());
                    displayUserLocation();
                    updateDriverLocation(userLocation.getLatitude(), userLocation.getLongitude());
                    towingAdapter.setDriverLocation(userLocation.getLatitude(), userLocation.getLongitude());
                    towingAdapter.setDriverId(driverId);
                    fetchNearbyTowing();
                } else {
                    Log.d(TAG, "Last known location is null, requesting new location.");
                    requestNewLocation();
                }
            }).addOnFailureListener(e -> {
                showError("Failed to get last known location.");
                requestNewLocation();
            });
            handler.postDelayed(() -> {
                if (userLocation == null) {
                    showError("Location request timed out. Please enable GPS and try again.");
                }
            }, 20000);
        } catch (SecurityException e) {
            showError("Location permission required.");
        }
    }

    /** Update the driver's current location in the "drivers" node. */
    private void updateDriverLocation(double latitude, double longitude) {
        if (driverId == null || driverId.isEmpty()) {
            Log.w(TAG, "Driver ID is not available; cannot update location.");
            return;
        }
        driversRef.child(driverId).child("currentLatitude").setValue(latitude);
        driversRef.child(driverId).child("currentLongitude").setValue(longitude);
        Log.d(TAG, "Driver location updated in database: " + latitude + ", " + longitude);
    }

    /** Step 6: Request new location if last known location is null */
    private void requestNewLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showError("Location permission not granted.");
            return;
        }
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000)
                .setNumUpdates(1);
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    userLocation = locationResult.getLastLocation();
                    Log.d(TAG, "New location received: " + userLocation.getLatitude() + ", " + userLocation.getLongitude());
                    displayUserLocation();
                    updateDriverLocation(userLocation.getLatitude(), userLocation.getLongitude());
                    fetchNearbyTowing();
                    fusedLocationClient.removeLocationUpdates(this);
                } else {
                    showError("Failed to get location.");
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /** Step 7: Display user's location in the UI */
    private void displayUserLocation() {
        if (userLocation != null) {
            userLocationTextView.setText("Your Location: " + userLocation.getLatitude() + ", " + userLocation.getLongitude());
        }
    }

    /** Step 8: Fetch nearby towing companies from Firebase Realtime Database and calculate driving distance using Google Directions API with dynamic radius */
    private void fetchNearbyTowing() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noTowingTextView.setVisibility(View.GONE);

        if (userLocation == null) {
            showError("Unable to fetch location.");
            return;
        }

        towingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                towingList.clear();
                Log.d(TAG, "Towing companies snapshot count: " + snapshot.getChildrenCount());
                final int totalCompanies = (int) snapshot.getChildrenCount();
                final int[] count = {0};
                final List<TowingModel> tempTowingList = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    TowingModel towing = dataSnapshot.getValue(TowingModel.class);
                    if (towing != null) {
                        // If the towing company has towingServices defined and the driver's selected towing services are provided,
                        // check if the company offers any of the required services.
                        if (towing.getTowingServices() != null && !driverTowingServices.isEmpty()) {
                            String towingServicesLower = towing.getTowingServices().toLowerCase();
                            String driverTowingLower = driverTowingServices.toLowerCase();
                            if (!towingServicesLower.contains(driverTowingLower)) {
                                count[0]++;
                                if (count[0] == totalCompanies) finalizeTowingList(tempTowingList);
                                continue;
                            }
                        }
                        calculateDrivingDistance(userLocation.getLatitude(), userLocation.getLongitude(),
                                towing.getCurrentLatitude(), towing.getCurrentLongitude(), new DistanceCallback() {
                                    @Override
                                    public void onDistanceCalculated(double routeDistance) {
                                        Log.d(TAG, "Towing Company " + towing.getName() + " at (" +
                                                towing.getCurrentLatitude() + ", " + towing.getCurrentLongitude() +
                                                ") is " + routeDistance + " km away.");
                                        if (routeDistance <= searchRadius) {
                                            towing.setDistance(routeDistance);
                                            tempTowingList.add(towing);
                                        }
                                        count[0]++;
                                        if (count[0] == totalCompanies) {
                                            // If fewer than 3 towing companies found and current radius is less than 50 km, increase radius and re-fetch.
                                            if (tempTowingList.size() < 3 && searchRadius < 50) {
                                                searchRadius = (searchRadius == 20) ? 30 : (searchRadius == 30) ? 40 : 50;
                                                Log.d(TAG, "Towing count (" + tempTowingList.size() + ") less than 3. Increasing search radius to " + searchRadius + " km and re-fetching.");
                                                fetchNearbyTowing();
                                            } else {
                                                finalizeTowingList(tempTowingList);
                                            }
                                        }
                                    }
                                });
                    } else {
                        count[0]++;
                        if (count[0] == totalCompanies) {
                            finalizeTowingList(tempTowingList);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                showError("Error fetching towing companies: " + error.getMessage());
            }
        });
    }

    /** Finalize and update the UI once all distance calculations are complete. */
    private void finalizeTowingList(List<TowingModel> list) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            if (list.isEmpty()) {
                showError("No towing companies found nearby that offer your required services.");
            } else {
                noTowingTextView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
                towingAdapter.updateTowingList(list);
            }
        });
    }

    /** Interface for distance callback. */
    public interface DistanceCallback {
        void onDistanceCalculated(double distance);
    }

    /** Calculate driving distance using Google Directions API. */
    private void calculateDrivingDistance(double originLat, double originLng, double destLat, double destLng, final DistanceCallback callback) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + originLat + "," + originLng
                + "&destination=" + destLat + "," + destLng
                + "&mode=driving&key=";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                float[] results = new float[1];
                Location.distanceBetween(originLat, originLng, destLat, destLng, results);
                double fallbackDistance = results[0] / 1000.0;
                callback.onDistanceCalculated(fallbackDistance);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    float[] results = new float[1];
                    Location.distanceBetween(originLat, originLng, destLat, destLng, results);
                    double fallbackDistance = results[0] / 1000.0;
                    callback.onDistanceCalculated(fallbackDistance);
                } else {
                    String responseData = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseData);
                        JSONArray routes = json.getJSONArray("routes");
                        if (routes.length() > 0) {
                            JSONObject route = routes.getJSONObject(0);
                            JSONArray legs = route.getJSONArray("legs");
                            if (legs.length() > 0) {
                                JSONObject leg = legs.getJSONObject(0);
                                JSONObject distanceObj = leg.getJSONObject("distance");
                                double distanceMeters = distanceObj.getDouble("value");
                                double routeDistance = distanceMeters / 1000.0;
                                callback.onDistanceCalculated(routeDistance);
                                return;
                            }
                        }
                        float[] results = new float[1];
                        Location.distanceBetween(originLat, originLng, destLat, destLng, results);
                        double fallbackDistance = results[0] / 1000.0;
                        callback.onDistanceCalculated(fallbackDistance);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        float[] results = new float[1];
                        Location.distanceBetween(originLat, originLng, destLat, destLng, results);
                        double fallbackDistance = results[0] / 1000.0;
                        callback.onDistanceCalculated(fallbackDistance);
                    }
                }
            }
        });
    }

    /** Handle location permission result */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissionAndGPS();
            } else {
                showError("Location permission is required to find towing companies.");
            }
        }
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        noTowingTextView.setVisibility(View.VISIBLE);
        noTowingTextView.setText(message);
        recyclerView.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }
}
