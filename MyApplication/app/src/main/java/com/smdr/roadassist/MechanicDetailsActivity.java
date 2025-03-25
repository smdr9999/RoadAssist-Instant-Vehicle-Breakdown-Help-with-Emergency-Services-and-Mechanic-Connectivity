package com.smdr.roadassist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MechanicDetailsActivity extends AppCompatActivity {

    private ImageView ivMechanicPhoto;
    private TextView tvMechanicName, tvExperience, tvDistance, tvMechanicPhone;
    private EditText etMessage;
    private Button btnRequest, btnLocate;

    // Mechanic's details passed via Intent extras.
    private String fullName, photo, experience, mechanicPhone, mechanicUserId;
    private double distance, latitude, longitude;

    // We no longer pass driver details via extras.
    // Instead, the request will fetch driver's details (name, phone, vehicle type, and location) from FirebaseAuth or from your driver record.
    // For this example, we'll assume the driver's details are fetched from FirebaseAuth.
    // If you need more advanced details (like vehicle type and permanent location), you can perform a separate database lookup.

    // Database reference for storing requests.
    private DatabaseReference requestsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("Started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_details);

        // Initialize UI components.
        ivMechanicPhoto = findViewById(R.id.ivMechanicPhoto);
        tvMechanicName = findViewById(R.id.tvMechanicName);
        tvExperience = findViewById(R.id.tvExperience);
        tvDistance = findViewById(R.id.tvDistance);
        tvMechanicPhone = findViewById(R.id.tvMechanicPhone);
        etMessage = findViewById(R.id.etMessage);
        btnRequest = findViewById(R.id.btnRequest);
        btnLocate = findViewById(R.id.btnLocate);

        // Retrieve mechanic details from Intent extras.
        Intent intent = getIntent();
        fullName = intent.getStringExtra("fullName");
        photo = intent.getStringExtra("photo");
        experience = intent.getStringExtra("experience");
        distance = intent.getDoubleExtra("distance", 0.0);
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);
        mechanicPhone = intent.getStringExtra("phone");
        mechanicUserId = intent.getStringExtra("userId");

        // Set the UI elements for mechanic details.
        tvMechanicName.setText("Name: " + (fullName != null ? fullName : "N/A"));
        if (experience != null) {
            tvExperience.setText("Experience: " + experience + " years");
        } else {
            tvExperience.setText("Experience: Not available");
        }
        tvDistance.setText(String.format("Distance: %.1f km", distance));

        // Decode and set the mechanic's photo.
        if (photo != null && !photo.isEmpty()) {
            Bitmap bitmap = decodeBase64(photo);
            if (bitmap != null) {
                ivMechanicPhoto.setImageBitmap(bitmap);
            } else {
                ivMechanicPhoto.setImageResource(android.R.drawable.ic_menu_help);
            }
        } else {
            ivMechanicPhoto.setImageResource(android.R.drawable.ic_menu_help);
        }

        // Initially hide the mechanic's phone number until a request is placed.
        tvMechanicPhone.setVisibility(View.GONE);

        // Initialize the database reference for requests.
        requestsRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Requests").child(mechanicUserId);

        // Set the Request button's click listener.
        btnRequest.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            // Message is optional; if empty, store null.
            sendRequest(message);
        });

        // Set the Locate button's click listener.
        btnLocate.setOnClickListener(v -> {
            // Open Google Maps with navigation to the driver's location.
            // Here, we assume that the mechanic can use the driver's location for navigation.
            // For this example, we pass the driver's location as extras from the adapter.
            // Alternatively, you could fetch the driver's location from the driver's profile in your database.
            // We'll assume that the driver's latitude and longitude have been passed via extras as "driverLatitude" and "driverLongitude".
            double driverLatitude = intent.getDoubleExtra("driverLatitude", 0.0);
            double driverLongitude = intent.getDoubleExtra("driverLongitude", 0.0);
            if (driverLatitude == 0.0 && driverLongitude == 0.0) {
                Toast.makeText(MechanicDetailsActivity.this, "Driver location not available", Toast.LENGTH_SHORT).show();
                return;
            }
            String uri = "google.navigation:q=" + latitude + "," + longitude;
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            mapIntent.setPackage("com.google.android.apps.maps");
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(MechanicDetailsActivity.this, "Google Maps not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to decode a Base64-encoded image string into a Bitmap.
    private Bitmap decodeBase64(String encodedImage) {
        try {
            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Send the request by writing the driver's details and message into the Requests node.
    private void sendRequest(String message) {
        // Assume driverId is available (for example, passed via intent)
        final String driverId = getIntent().getStringExtra("driverId");
        if (driverId == null || driverId.isEmpty()) {
            Toast.makeText(MechanicDetailsActivity.this, "Driver details not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a reference to the driver's details in the "drivers" node.
        DatabaseReference driverRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("drivers").child(driverId);

        // Fetch the driver's details from the Realtime Database.
        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String dName = snapshot.child("name").getValue(String.class);
                Double dLat = snapshot.child("currentLatitude").getValue(Double.class);
                Double dLon = snapshot.child("currentLongitude").getValue(Double.class);
                String dVehicle = snapshot.child("vehicle").getValue(String.class);
                // Use the phone number from FirebaseAuth since it is not stored in the driver's record.
                String dPhone = FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() : "N/A";

                // Prepare the request data.
                Map<String, Object> requestData = new HashMap<>();
                requestData.put("driverName", dName != null ? dName : "N/A");
                requestData.put("driverPhone", dPhone);
                requestData.put("driverVehicle", dVehicle != null ? dVehicle : "N/A");
                // Instead of a combined driverLocation string, we store the latitude and longitude.
                requestData.put("driverLatitude", dLat != null ? dLat : 0.0);
                requestData.put("driverLongitude", dLon != null ? dLon : 0.0);
                // Include the message (if provided) and a timestamp.
                requestData.put("message", message.isEmpty() ? "" : message);
                requestData.put("timestamp", System.currentTimeMillis());

                // Write the request data to the database under the "Requests" node for the particular mechanic.
                requestsRef.push().setValue(requestData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(MechanicDetailsActivity.this, "Request sent successfully", Toast.LENGTH_SHORT).show();
                            // Display the mechanic's phone number for the driver to call.
                            if (mechanicPhone != null && !mechanicPhone.isEmpty()) {
                                tvMechanicPhone.setText("Mechanic Phone: " + mechanicPhone);
                            } else {
                                tvMechanicPhone.setText("Mechanic Phone: Not Available");
                            }
                            tvMechanicPhone.setVisibility(View.VISIBLE);
                        })
                        .addOnFailureListener(e -> Toast.makeText(MechanicDetailsActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MechanicDetailsActivity.this, "Failed to fetch driver details", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
