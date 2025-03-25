package com.smdr.roadassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MechanicDashboardActivity extends AppCompatActivity {
    private Button btnViewProfile, btnViewRequests, btnLogout;
    private Button btnStartLocation, btnStopLocation; // Added buttons for location tracking

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_dashboard);

        btnViewProfile = findViewById(R.id.btnViewProfile);
        btnViewRequests = findViewById(R.id.btnViewRequests);
        btnLogout = findViewById(R.id.btnLogout);
        btnStartLocation = findViewById(R.id.btnStartLocation); // New button
        btnStopLocation = findViewById(R.id.btnStopLocation);   // New button

        btnViewProfile.setOnClickListener(v -> startActivity(new Intent(this, MechanicProfileActivity.class)));
        btnViewRequests.setOnClickListener(v -> startActivity(new Intent(this, RepairRequestsActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Start Mechanic Location Tracking
        btnStartLocation.setOnClickListener(v -> startLocationService());

        // Stop Mechanic Location Tracking
        btnStopLocation.setOnClickListener(v -> stopLocationService());
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, MechanicLocationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent); // For Android 8.0+ (API 26+)
        } else {
            startService(serviceIntent); // For Android 6.0 - 7.1 (API 23-25)
        }
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, MechanicLocationService.class);
        stopService(serviceIntent);
    }
}
