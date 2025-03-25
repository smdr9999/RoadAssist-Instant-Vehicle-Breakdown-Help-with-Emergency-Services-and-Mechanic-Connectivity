package com.smdr.roadassist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DriverProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private TextView tvDriverName, tvVehicleType, tvPhone;
    private ImageView imgProfile, imgLicense, imgInsurance;
    private Button btnFindMechanics, btnLogout;

    // Store the driver's vehicle type (from the "vehicle" field in DB)
    private String driverVehicleType = null;

    private static final String TAG = "DriverProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_profile);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Connect to Firebase Realtime Database using explicit URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app");
        dbRef = database.getReference("drivers");

        // Initialize UI elements
        tvDriverName = findViewById(R.id.tvDriverName);
        tvVehicleType = findViewById(R.id.tvVehicleType);
        tvPhone = findViewById(R.id.tvPhone);
        imgProfile = findViewById(R.id.imgProfile);
        imgLicense = findViewById(R.id.imgLicense);
        imgInsurance = findViewById(R.id.imgInsurance);
        btnFindMechanics = findViewById(R.id.btnFindMechanics);
        btnLogout = findViewById(R.id.btnLogout);

        // Initially disable the "Find Nearby Mechanics" button until data is loaded.
        btnFindMechanics.setEnabled(false);

        // Get the logged-in user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String phoneNumber = currentUser.getPhoneNumber(); // Fetch phone number from Firebase Auth
            tvPhone.setText("Phone: " + (phoneNumber != null ? phoneNumber : "Not Available"));

            // Load profile details (including vehicle type)
            loadDriverProfile(userId);
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        // Logout Button
        btnLogout.setOnClickListener(view -> {
            mAuth.signOut();
            startActivity(new Intent(DriverProfileActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void loadDriverProfile(String userId) {
        dbRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    // Read the vehicle type from the "vehicle" key
                    driverVehicleType = snapshot.child("vehicle").getValue(String.class);
                    String profileBase64 = snapshot.child("photo").getValue(String.class);
                    String licenseBase64 = snapshot.child("license").getValue(String.class);
                    String insuranceBase64 = snapshot.child("insurance").getValue(String.class);

                    tvDriverName.setText("Name: " + (name != null ? name : "N/A"));
                    tvVehicleType.setText("Vehicle: " + (driverVehicleType != null ? driverVehicleType : "N/A"));

                    Log.d(TAG, "Retrieved vehicle type: " + driverVehicleType);

                    // Decode and set images
                    if (profileBase64 != null) {
                        imgProfile.setImageBitmap(decodeBase64(profileBase64));
                    }
                    if (licenseBase64 != null) {
                        imgLicense.setImageBitmap(decodeBase64(licenseBase64));
                    }
                    if (insuranceBase64 != null) {
                        imgInsurance.setImageBitmap(decodeBase64(insuranceBase64));
                    }

                    // Enable and set click listener for the "Find Nearby Mechanics" button only after data is loaded.
                    btnFindMechanics.setEnabled(true);
                    btnFindMechanics.setOnClickListener(view -> {
                        Intent intent = new Intent(DriverProfileActivity.this, FindMechanicsActivity.class);
                        intent.putExtra("vehicleType", driverVehicleType);
                        Log.d("FindMechanicsActivity", "Retrieved vehicle type: " + driverVehicleType);
                        startActivity(intent);
                    });
                } else {
                    Toast.makeText(DriverProfileActivity.this, "Driver data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DriverProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Decode Base64 string into a Bitmap.
    private Bitmap decodeBase64(String encodedImage) {
        try {
            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Error decoding image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
