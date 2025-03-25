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

public class TowingDetailsActivity extends AppCompatActivity {

    private ImageView ivTowingPhoto;
    private TextView tvTowingName, tvTowingServices, tvExperience, tvDistance, tvTowingPhone;
    private EditText etMessage;
    private Button btnRequest, btnLocate;

    // Towing company's details passed via Intent extras.
    private String towingName, photo, towingServices, experience, towingPhone, towingUserId;
    private double distance, latitude, longitude;

    // Driver details passed via Intent extras.
    private String driverId;
    private double driverLatitude, driverLongitude;

    // Database reference for storing requests.
    private DatabaseReference requestsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("Started");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_towing_details);

        // Initialize UI components.
        ivTowingPhoto = findViewById(R.id.ivTowingPhoto);
        tvTowingName = findViewById(R.id.tvTowingName);
        tvTowingServices = findViewById(R.id.tvTowingServices);
        tvExperience = findViewById(R.id.tvExperience);
        tvDistance = findViewById(R.id.tvDistance);
        tvTowingPhone = findViewById(R.id.tvTowingPhone);
        etMessage = findViewById(R.id.etMessage);
        btnRequest = findViewById(R.id.btnRequest);
        btnLocate = findViewById(R.id.btnLocate);

        // Retrieve towing details from Intent extras.
        Intent intent = getIntent();
        towingName = intent.getStringExtra("fullName");
        photo = intent.getStringExtra("photo");
        towingServices = intent.getStringExtra("towingServices");
        experience = intent.getStringExtra("experience");
        distance = intent.getDoubleExtra("distance", 0.0);
        latitude = intent.getDoubleExtra("latitude", 0.0);
        longitude = intent.getDoubleExtra("longitude", 0.0);
        towingPhone = intent.getStringExtra("phone");
        towingUserId = intent.getStringExtra("userId");

        // Retrieve driver's details.
        driverId = intent.getStringExtra("driverId");
        driverLatitude = intent.getDoubleExtra("driverLatitude", 0.0);
        driverLongitude = intent.getDoubleExtra("driverLongitude", 0.0);
        System.out.println(driverLatitude+" "+driverLongitude);
        // Set the UI elements.
        tvTowingName.setText("Name: " + (towingName != null ? towingName : "N/A"));
        tvTowingServices.setText("Services: " + (towingServices != null ? towingServices : "N/A"));
        if (experience != null) {
            tvExperience.setText("Experience: " + experience + " years");
        } else {
            tvExperience.setText("Experience: Not available");
        }
        tvDistance.setText(String.format("Distance: %.1f km", distance));
        if (photo != null && !photo.isEmpty()) {
            Bitmap bitmap = decodeBase64(photo);
            if (bitmap != null) {
                ivTowingPhoto.setImageBitmap(bitmap);
            } else {
                ivTowingPhoto.setImageResource(android.R.drawable.ic_menu_help);
            }
        } else {
            ivTowingPhoto.setImageResource(android.R.drawable.ic_menu_help);
        }
        tvTowingPhone.setVisibility(View.GONE);

        // Initialize the database reference for requests.
        requestsRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Requests").child(towingUserId);

        // Set the Request button's click listener.
        btnRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = etMessage.getText().toString().trim();
                sendRequest(message);
            }
        });

        // Set the Locate button's click listener.
        btnLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (latitude == 0.0 && longitude == 0.0) {
                    Toast.makeText(TowingDetailsActivity.this, "Driver location not available", Toast.LENGTH_SHORT).show();
                    return;
                }
                String uri = "google.navigation:q=" + latitude + "," + longitude;
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(TowingDetailsActivity.this, "Google Maps not available", Toast.LENGTH_SHORT).show();
                }
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
        // Fetch driver's details from the "drivers" node.
        DatabaseReference driverRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("drivers").child(driverId);
        driverRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String dName = snapshot.child("name").getValue(String.class);
                Double dLat = snapshot.child("currentLatitude").getValue(Double.class);
                Double dLon = snapshot.child("currentLongitude").getValue(Double.class);
                String dVehicle = snapshot.child("vehicle").getValue(String.class);
                String dPhone = FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() : "N/A";

                Map<String, Object> requestData = new HashMap<>();
                requestData.put("driverName", dName != null ? dName : "N/A");
                requestData.put("driverPhone", dPhone);
                requestData.put("driverVehicle", dVehicle != null ? dVehicle : "N/A");
                requestData.put("driverLatitude", dLat != null ? dLat : 0.0);
                requestData.put("driverLongitude", dLon != null ? dLon : 0.0);
                requestData.put("message", message.isEmpty() ? "" : message);
                requestData.put("timestamp", System.currentTimeMillis());

                requestsRef.push().setValue(requestData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(TowingDetailsActivity.this, "Request sent successfully", Toast.LENGTH_SHORT).show();
                            if (towingPhone != null && !towingPhone.isEmpty()) {
                                tvTowingPhone.setText("Towing Phone: " + towingPhone);
                            } else {
                                tvTowingPhone.setText("Towing Phone: Not Available");
                            }
                            tvTowingPhone.setVisibility(View.VISIBLE);
                        })
                        .addOnFailureListener(e -> Toast.makeText(TowingDetailsActivity.this, "Failed to send request", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TowingDetailsActivity.this, "Failed to fetch driver details", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
