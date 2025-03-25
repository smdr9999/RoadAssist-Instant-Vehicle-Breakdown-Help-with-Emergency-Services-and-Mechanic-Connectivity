package com.smdr.roadassist;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DriverRegistrationActivity extends AppCompatActivity {
    private static final int PICK_PROFILE_PHOTO = 1;
    private static final int PICK_LICENSE = 2;
    private static final int PICK_INSURANCE = 3;

    private EditText etDriverName;
    private Spinner spinnerVehicleType;
    private Button btnUploadDriverPhoto, btnUploadLicense, btnUploadInsurance, btnRegisterDriver;

    private DatabaseReference databaseReference;
    private FirebaseAuth auth;

    private String base64ProfilePhoto = null, base64License = null, base64Insurance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_registration);

        // Initialize Views
        etDriverName = findViewById(R.id.etDriverName);
        spinnerVehicleType = findViewById(R.id.spinnerVehicleType);
        btnUploadDriverPhoto = findViewById(R.id.btnUploadDriverPhoto);
        btnUploadLicense = findViewById(R.id.btnUploadLicense);
        btnUploadInsurance = findViewById(R.id.btnUploadInsurance);
        btnRegisterDriver = findViewById(R.id.btnRegisterDriver);

        // Set up the Spinner with vehicle type options.
        String[] vehicleTypes = {"Car", "Lorry", "Bike", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vehicleTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVehicleType.setAdapter(adapter);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("drivers");

        // Set Listeners for Upload Buttons
        btnUploadDriverPhoto.setOnClickListener(v -> openFileChooser(PICK_PROFILE_PHOTO));
        btnUploadLicense.setOnClickListener(v -> openFileChooser(PICK_LICENSE));
        btnUploadInsurance.setOnClickListener(v -> openFileChooser(PICK_INSURANCE));

        // Register Driver
        btnRegisterDriver.setOnClickListener(v -> registerDriver());
    }

    private void openFileChooser(int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri == null) {
                Toast.makeText(this, "Image selection failed!", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                String encodedImage = encodeImage(bitmap);

                switch (requestCode) {
                    case PICK_PROFILE_PHOTO:
                        base64ProfilePhoto = encodedImage;
                        btnUploadDriverPhoto.setText("Profile Photo Selected");
                        break;
                    case PICK_LICENSE:
                        base64License = encodedImage;
                        btnUploadLicense.setText("License Selected");
                        break;
                    case PICK_INSURANCE:
                        base64Insurance = encodedImage;
                        btnUploadInsurance.setText("Insurance Selected");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);  // 70% compression
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void registerDriver() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etDriverName.getText().toString().trim();
        String vehicleType = spinnerVehicleType.getSelectedItem().toString();

        if (name.isEmpty() || vehicleType.isEmpty() || base64ProfilePhoto == null || base64License == null || base64Insurance == null) {
            Toast.makeText(this, "All fields and files are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save Data to Firebase Realtime Database
        Map<String, Object> driverData = new HashMap<>();
        driverData.put("userId", user.getUid());
        driverData.put("name", name);
        driverData.put("vehicle", vehicleType);  // Store the selected vehicle type
        driverData.put("photo", base64ProfilePhoto);
        driverData.put("license", base64License);
        driverData.put("insurance", base64Insurance);

        databaseReference.child(user.getUid())
                .setValue(driverData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Driver Registered Successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(DriverRegistrationActivity.this, UserTypeSelectionActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseError", "Error registering driver", e);
                    Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
                });
    }
}
