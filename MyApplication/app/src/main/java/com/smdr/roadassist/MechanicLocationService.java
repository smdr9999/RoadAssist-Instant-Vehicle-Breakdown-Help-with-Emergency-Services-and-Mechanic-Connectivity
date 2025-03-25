package com.smdr.roadassist;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MechanicLocationService extends Service {
    private static final String TAG = "MechanicLocationService";
    private static final int LOCATION_UPDATE_INTERVAL = 5000; // 5 seconds
    private static final String CHANNEL_ID = "MechanicLocationServiceChannel";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference databaseReference;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        databaseReference = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app").getReference("mechanics");

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateLocationInDatabase(location.getLatitude(), location.getLongitude());
                }
            }
        };

        requestLocationUpdates();

        startForeground(1, getNotification());
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(LOCATION_UPDATE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void updateLocationInDatabase(double latitude, double longitude) {
        String mechanicId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (mechanicId != null) {
            // Update only latitude & longitude fields
            Map<String, Object> locationUpdates = new HashMap<>();
            locationUpdates.put("currentLatitude", latitude);
            locationUpdates.put("currentLongitude", longitude);

            databaseReference.child(mechanicId).updateChildren(locationUpdates)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Lat = " + latitude + ", Lng = " + longitude +" - Location updated successfully in Firebase"))
                    .addOnFailureListener(e -> Log.e(TAG, "Lat = " + latitude + ", Lng = " + longitude +" - Failed to update location in Firebase: " + e.getMessage()));
        } else {
            Log.e(TAG, "User not authenticated. Cannot update location.");
        }
    }

    private Notification getNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Mechanic Location Tracking")
                .setContentText("Updating your location in real-time...")
                .setSmallIcon(R.drawable.ic_location) // Ensure you have this icon in res/drawable
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mechanic Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d(TAG, "Location updates stopped.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Inner class to store location data
    public static class MechanicLocation {
        public double latitude;
        public double longitude;

        public MechanicLocation() {}

        public MechanicLocation(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
