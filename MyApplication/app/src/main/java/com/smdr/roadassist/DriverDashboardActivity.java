package com.smdr.roadassist;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverDashboardActivity extends AppCompatActivity {

    private Button btnFindMechanics, btnFindTowing, btnViewProfile, btnLogout, btnSOS, btnAddEmergencyContacts;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth mAuth;
    private DatabaseReference driversRef, emergencyContactsRef, mechanicsRef, requestsForMechanicsRef;
    private Location currentLocation;
    private long lastSOSClickTime = 0;
    private int sosClickCount = 0;
    private static final String TAG = "DriverDashboardActivity";

    // Search radius for finding mechanics; initial value set to 20 km.
    private double searchRadius = 20;

    private String name, vehicle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_dashboard);

        // Initialize UI elements
        btnFindMechanics = findViewById(R.id.btnFindMechanics);
        btnFindTowing = findViewById(R.id.btnFindTowing);
        btnViewProfile = findViewById(R.id.btnViewProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnSOS = findViewById(R.id.btnSOS);
        btnAddEmergencyContacts = findViewById(R.id.btnAddEmergencyContacts);

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        driversRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("drivers");
        emergencyContactsRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("emergencyContacts");
        mechanicsRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("mechanics");
        // Requests for mechanics are stored under "RequestsForMechanics"
        requestsForMechanicsRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Requests");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference driversRef = FirebaseDatabase
                    .getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                    .getReference("drivers");

            driversRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    name = snapshot.child("name").getValue(String.class);
                    vehicle = snapshot.child("vehicle").getValue(String.class);
                    // Use the fetched values as needed.
                    System.out.println("Name: " + name + ", Vehicle: " + vehicle);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("DriverData", "Failed to fetch driver details: " + error.getMessage());
                }
            });
        } else {
            Log.e("DriverData", "User not logged in.");
        }
        // Set button listeners for standard dashboard actions
        btnFindMechanics.setOnClickListener(v -> startActivity(new Intent(this, FindMechanicsActivity.class)));
        btnFindTowing.setOnClickListener(v -> startActivity(new Intent(this, FindTowingActivity.class)));
        btnViewProfile.setOnClickListener(v -> startActivity(new Intent(this, DriverProfileActivity.class)));
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        btnAddEmergencyContacts.setOnClickListener(v -> startActivity(new Intent(this, AddEmergencyContactsActivity.class)));

        // SOS button handles single and double click functionalities.
        btnSOS.setOnClickListener(v -> handleSOSClick());

        // Fetch the current location (used for both SOS and updating driver location)
        fetchCurrentLocation();
    }

    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                System.out.println("current location: "+currentLocation+" "+currentLocation.getLatitude()+" "+currentLocation.getLongitude());
                updateDriverLocation(location.getLatitude(), location.getLongitude());
            } else {
                // Fetch the currentLatitude and currentLongitude from the driver's database record
                System.out.println("current location is null");
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String driverId = currentUser.getUid();
                    driversRef.child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Double lat = snapshot.child("currentLatitude").getValue(Double.class);
                            Double lon = snapshot.child("currentLongitude").getValue(Double.class);
                            if (lat != null && lon != null) {
                                // Create a new Location object and assign it to currentLocation
                                Location loc = new Location("");
                                loc.setLatitude(lat);
                                loc.setLongitude(lon);
                                currentLocation = loc;
                                updateDriverLocation(lat, lon);
                            } else {
                                Log.e(TAG, "Driver location not found in database");
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to fetch driver location: " + error.getMessage());
                        }
                    });
                } else {
                    Log.e(TAG, "Current location is null and user is not logged in");
                }
            }
        });

    }

    private void updateDriverLocation(double latitude, double longitude) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        String driverId = currentUser.getUid();
        driversRef.child(driverId).child("currentLatitude").setValue(latitude);
        driversRef.child(driverId).child("currentLongitude").setValue(longitude);
        Log.d(TAG, "Driver location updated: " + latitude + ", " + longitude);
    }

    // Handle SOS button click: single click sends SMS to emergency contacts; double click sends SOS requests to mechanics.
    private void handleSOSClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSOSClickTime < 1000) {
            sosClickCount++;
        } else {
            sosClickCount = 1;
        }
        lastSOSClickTime = currentTime;
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (sosClickCount == 1) {
                sendSOSToEmergencyContacts();
            } else if (sosClickCount >= 2) {
                sendSOSRequestToMechanics();
            }
            sosClickCount = 0;
        }, 1000);
    }

    // Send SOS SMS to emergency contacts with driver's location.
    private void sendSOSToEmergencyContacts() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentLocation == null) {
            Toast.makeText(this, "Unable to send SOS. User or location not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        String driverId = currentUser.getUid();
        emergencyContactsRef.child(driverId).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                List<String> contacts = new ArrayList<>();
                for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                    String contact = ds.getValue(String.class);
                    if (contact != null && !contact.isEmpty()) {
                        contacts.add(contact);
                    }
                }
                if (contacts.isEmpty()) {
                    Toast.makeText(DriverDashboardActivity.this, "No emergency contacts found.", Toast.LENGTH_SHORT).show();
                } else {
                    String message = "HELP! I'm at: https://maps.google.com/?q=" +
                            currentLocation.getLatitude() + "%2C" + currentLocation.getLongitude();
                    Log.d(TAG, "Sending SOS message: " + message);
                    sendSMSToContacts(contacts, message);
                }
            }
            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(DriverDashboardActivity.this, "Failed to fetch emergency contacts.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Send SOS request to mechanics by finding nearby mechanics and writing a request entry for each.
    private void sendSOSRequestToMechanics() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentLocation == null) {
            Toast.makeText(this, "Unable to send SOS request.", Toast.LENGTH_SHORT).show();
            return;
        }
        String driverId = currentUser.getUid();
        String driverPhone = currentUser.getPhoneNumber();
        // For this example, we assume a fixed search radius (you can modify to use dynamic radius logic as in FindMechanicsActivity)
        double fixedRadius = 30; // km

        // Query mechanics and send request to each mechanic within the radius.
        mechanicsRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                    MechanicModel mechanic = ds.getValue(MechanicModel.class);
                    if (mechanic != null) {
                        double computedDistance = calculateDistance(
                                currentLocation.getLatitude(), currentLocation.getLongitude(),
                                mechanic.getCurrentLatitude(), mechanic.getCurrentLongitude());
                        if (computedDistance <= fixedRadius) {
                            // Build request data for this mechanic.
                            Map<String, Object> requestData = new HashMap<>();
                            requestData.put("driverId", driverId);
                            //String name = snapshot.child("name").getValue(String.class);
                            //String vehicle = snapshot.child("vehicle").getValue(String.class);
                            System.out.println("Name:"+name+" Vehicle:"+vehicle);
                            requestData.put("driverName", name != null ? name : "N/A");
                            requestData.put("driverVehicle", vehicle != null ? vehicle : "N/A");
                            requestData.put("driverPhone", driverPhone != null ? driverPhone : "N/A");
                            requestData.put("driverLatitude", currentLocation.getLatitude());
                            requestData.put("driverLongitude", currentLocation.getLongitude());
                            requestData.put("message", "SOS - Need immediate assistance. My phone number is " +
                                    (driverPhone != null ? driverPhone : "N/A"));
                            requestData.put("timestamp", System.currentTimeMillis());
                            // Write the request data under the mechanic's Requests node.
                            FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                                    .getReference("Requests")
                                    .child(mechanic.getUserId())
                                    .push()
                                    .setValue(requestData);
                        }
                    }
                }
                Toast.makeText(DriverDashboardActivity.this, "SOS request sent to mechanics", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(DriverDashboardActivity.this, "Failed to send SOS request", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Use SmsManager to send SMS messages to a list of contacts.
    private void sendSMSToContacts(List<String> contacts, String message) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 101);
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SmsManager smsManager = SmsManager.getDefault();
            for (String contact : contacts) {
                smsManager.sendTextMessage(contact, null, message, null, null);
            }
            Toast.makeText(this, "SOS message sent to emergency contacts", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "SOS message sent successfully");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SOS message", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "SMS sending failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Calculate straight-line distance (fallback) in kilometers between two coordinates. */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000.0;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
