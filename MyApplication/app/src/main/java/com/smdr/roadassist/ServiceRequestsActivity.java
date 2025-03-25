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
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ServiceRequestsActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int GPS_ENABLE_REQUEST_CODE = 2;
    private static final String TAG = "ServiceRequestsActivity";
    // Google Directions API key – replace with your key.
    private static final String DIRECTIONS_API_KEY = "AIzaSyBIm8OGgUT8UV8QJitZ7aSwNb2FSQ-RyAA";

    private FusedLocationProviderClient fusedLocationClient;
    private Location mechanicLocation; // Mechanic's current location (from GPS)
    private DatabaseReference requestsRef; // Repair requests for this mechanic
    private RecyclerView recyclerView;
    private RepairRequestAdapter adapter;
    private List<RepairRequestModel> requestList = new ArrayList<>();

    private ProgressBar progressBar;
    private TextView noRequestsTextView;

    // Mechanic's user ID (used to fetch repair requests for that mechanic)
    private String mechanicId;

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair_requests);

        recyclerView = findViewById(R.id.recyclerViewRequests);
        progressBar = findViewById(R.id.progressBarRequests);
        noRequestsTextView = findViewById(R.id.tvNoRequests);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RepairRequestAdapter(requestList, this);
        recyclerView.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mechanicId = currentUser.getUid();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app");
        requestsRef = database.getReference("Requests").child(mechanicId);

        checkLocationPermissionAndGPS();
    }

    /** Step 1: Check location permission and if GPS is enabled */
    private void checkLocationPermissionAndGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else if (!isGPSEnabled()) {
            promptEnableGPS();
        } else {
            getMechanicLocation();
        }
    }

    /** Step 2: Check if GPS is enabled */
    private boolean isGPSEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return (locationManager != null) && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /** Step 3: Prompt user to enable GPS */
    private void promptEnableGPS() {
        new AlertDialog.Builder(this)
                .setTitle("Enable GPS")
                .setMessage("GPS is required to fetch your current location. Please enable GPS.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), GPS_ENABLE_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    showError("GPS must be enabled to view repair requests.");
                })
                .setCancelable(false)
                .show();
    }

    /** Step 4: Fetch the mechanic’s current location */
    private void getMechanicLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            showError("Location permission not granted.");
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                mechanicLocation = location;
                Log.d(TAG, "Mechanic location: " + location.getLatitude() + ", " + location.getLongitude());
                updateMechanicLocationInDB(location);
                fetchRepairRequests();
            } else {
                requestNewLocation();
            }
        }).addOnFailureListener(e -> {
            showError("Failed to fetch current location.");
            requestNewLocation();
        });

        handler.postDelayed(() -> {
            if (mechanicLocation == null) {
                showError("Location request timed out. Please enable GPS and try again.");
            }
        }, 20000);
    }

    /** Request a new location update if the last known location is null */
    private void requestNewLocation() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(2000)
                .setNumUpdates(1);
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null && locationResult.getLastLocation() != null) {
                    mechanicLocation = locationResult.getLastLocation();
                    Log.d(TAG, "New mechanic location: " + mechanicLocation.getLatitude() + ", " + mechanicLocation.getLongitude());
                    updateMechanicLocationInDB(mechanicLocation);
                    fetchRepairRequests();
                    fusedLocationClient.removeLocationUpdates(this);
                } else {
                    showError("Failed to get location update.");
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /** Optionally update the mechanic's current location in the database (if desired) */
    private void updateMechanicLocationInDB(Location location) {
        FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("towingServices")
                .child(mechanicId)
                .child("currentLatitude").setValue(location.getLatitude());
        FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("towingServices")
                .child(mechanicId)
                .child("currentLongitude").setValue(location.getLongitude());
    }

    /** Display the mechanic's location (optional UI update) */
    private void displayMechanicLocation() {
        // Optionally, update a TextView if you wish to display the current location.
    }

    /** Step 5: Fetch repair requests from the database and compute driving distance via Directions API */
    private void fetchRepairRequests() {
        progressBar.setVisibility(View.VISIBLE);
        // Clear the current list.
        requestList.clear();

        requestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "Repair requests count: " + snapshot.getChildrenCount());
                if (snapshot.getChildrenCount() == 0) {
                    progressBar.setVisibility(View.GONE);
                    showError("No repair requests found.");
                    return;
                }
                // For each request, fetch route distance asynchronously.
                for (DataSnapshot ds : snapshot.getChildren()) {
                    RepairRequestModel request = ds.getValue(RepairRequestModel.class);
                    if (request != null) {
                        // For each request, use the stored driver's location.
                        double driverLat = request.getDriverLatitude();
                        double driverLng = request.getDriverLongitude();
                        // Call the Google Directions API to get the driving distance.
                        fetchRouteDistance(mechanicLocation.getLatitude(), mechanicLocation.getLongitude(),
                                driverLat, driverLng, new DrivingDistanceCallback() {
                                    @Override
                                    public void onDistanceFetched(double routeDistance) {
                                        // Set the route distance in the request.
                                        request.setDistance(routeDistance);
                                        // Only add the request if within a desired threshold, e.g., 20 km.
                                        if (routeDistance <= 30) {
                                            requestList.add(request);
                                        }
                                        // Update the adapter each time a request's distance is computed.
                                        runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            if (requestList.isEmpty()) {
                                                showError("No repair requests found within 20 km.");
                                            } else {
                                                noRequestsTextView.setVisibility(View.GONE);
                                                recyclerView.setVisibility(View.VISIBLE);
                                                adapter.updateRequestList(requestList);
                                            }
                                        });
                                    }
                                });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Error fetching requests: " + error.getMessage());
            }
        });
    }

    /** Step 6: Calculate the driving distance using the Google Directions API */
    private void fetchRouteDistance(double originLat, double originLng, double destLat, double destLng,
                                    DrivingDistanceCallback callback) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + originLat + "," + originLng
                + "&destination=" + destLat + "," + destLng
                + "&mode=driving&key=" + DIRECTIONS_API_KEY;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                // Fall back to the straight-line distance in case of failure.
                double fallbackDistance = calculateDistance(originLat, originLng, destLat, destLng);
                callback.onDistanceFetched(fallbackDistance);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
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
                                // "value" is in meters.
                                double distanceMeters = distanceObj.getDouble("value");
                                double routeDistanceKm = distanceMeters / 1000.0;
                                callback.onDistanceFetched(routeDistanceKm);
                                return;
                            }
                        }
                        // If parsing fails, fall back.
                        double fallbackDistance = calculateDistance(originLat, originLng, destLat, destLng);
                        callback.onDistanceFetched(fallbackDistance);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        double fallbackDistance = calculateDistance(originLat, originLng, destLat, destLng);
                        callback.onDistanceFetched(fallbackDistance);
                    }
                } else {
                    double fallbackDistance = calculateDistance(originLat, originLng, destLat, destLng);
                    callback.onDistanceFetched(fallbackDistance);
                }
            }
        });
    }

    /** Callback interface for the route distance */
    public interface DrivingDistanceCallback {
        void onDistanceFetched(double distance);
    }

    /** Step 7: Calculate the straight-line distance (fallback) */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000; // in kilometers
    }

    /** Utility method to show error messages */
    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        Log.e(TAG, message);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissionAndGPS();
            } else {
                showError("Location permission is required to fetch repair requests.");
            }
        }
    }
}
