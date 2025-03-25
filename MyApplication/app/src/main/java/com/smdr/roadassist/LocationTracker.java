package com.smdr.roadassist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LocationTracker extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Locations");

        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private void requestLocationUpdates() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = new LocationRequest()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000);

            LocationCallback locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        // Store location in Firebase
                        databaseReference.child("mechanic_1").setValue(new LocationModel(latitude, longitude));

                        Log.d("LocationTracker", "Location Updated: " + latitude + ", " + longitude);
                    }
                }
            };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    // Model class to store location in Firebase
    public static class LocationModel {
        public double latitude;
        public double longitude;

        public LocationModel() {} // Needed for Firebase
        public LocationModel(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
