package com.smdr.roadassist;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TowingRegistrationActivity extends AppCompatActivity {

    private static final int PICK_PHOTO = 1;
    private static final int PICK_LICENSE = 2;
    private static final int PICK_POLICE_VERIFICATION = 3;
    private int currentRequestCode = -1;

    private EditText etFullName, etExperience, etPermanentLocation;
    private TextView tvCurrentLocation, tvSelectedTowingServices;
    private Button btnUploadPhoto, btnUploadLicense, btnUploadPoliceVerification, btnRegister, btnSelectLocation, btnSelectTowingServices;

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private FusedLocationProviderClient fusedLocationClient;

    private String base64Photo, base64License, base64PoliceVerification;
    private double currentLatitude, currentLongitude;
    // Comma-separated string for selected towing service types.
    private String selectedTowingServices = "";

    private final ActivityResultLauncher<String> storagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openFileChooser(PICK_PHOTO);
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        processImage(currentRequestCode, imageUri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> placePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    etPermanentLocation.setText(place.getAddress());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_towing_registration);

        initializeUI();
        checkGPSAndRequestLocation();

        btnUploadPhoto.setOnClickListener(v -> openFileChooser(PICK_PHOTO));
        btnUploadLicense.setOnClickListener(v -> openFileChooser(PICK_LICENSE));
        btnUploadPoliceVerification.setOnClickListener(v -> openFileChooser(PICK_POLICE_VERIFICATION));
        btnRegister.setOnClickListener(v -> registerTowingService());
        btnSelectLocation.setOnClickListener(v -> openPlacePicker());
        btnSelectTowingServices.setOnClickListener(v -> openTowingTypesDialog());
    }

    private void initializeUI() {
        etFullName = findViewById(R.id.etFullName);
        etExperience = findViewById(R.id.etExperience);
        etPermanentLocation = findViewById(R.id.etPermanentLocation);
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        tvSelectedTowingServices = findViewById(R.id.tvSelectedTowingServices);
        btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
        btnUploadLicense = findViewById(R.id.btnUploadLicense);
        btnUploadPoliceVerification = findViewById(R.id.btnUploadPoliceVerification);
        btnRegister = findViewById(R.id.btnRegister);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        btnSelectTowingServices = findViewById(R.id.btnSelectTowingServices);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("towingServices");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "");
        }
    }

    private void checkGPSAndRequestLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please enable GPS", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                tvCurrentLocation.setText("Lat: " + currentLatitude + ", Lon: " + currentLongitude);
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openFileChooser(int requestCode) {
        currentRequestCode = requestCode;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void processImage(int requestCode, Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            String encodedImage = encodeImage(bitmap);
            switch (requestCode) {
                case PICK_PHOTO:
                    base64Photo = encodedImage;
                    btnUploadPhoto.setText("Photo Selected");
                    break;
                case PICK_LICENSE:
                    base64License = encodedImage;
                    btnUploadLicense.setText("License Selected");
                    break;
                case PICK_POLICE_VERIFICATION:
                    base64PoliceVerification = encodedImage;
                    btnUploadPoliceVerification.setText("Police Verification Selected");
                    break;
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this);
        placePickerLauncher.launch(intent);
    }

    // Opens a multi-choice dialog for selecting towing service types.
    private void openTowingTypesDialog() {
        final String[] towingOptions = {"Tow Truck", "Flatbed", "Motorcycle Tow", "Heavy Duty Tow", "Other"};
        final boolean[] selectedOptions = new boolean[towingOptions.length];
        final ArrayList<String> selectedList = new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Towing Services Available");
        builder.setMultiChoiceItems(towingOptions, selectedOptions, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int index, boolean isChecked) {
                if (isChecked) {
                    selectedList.add(towingOptions[index]);
                } else {
                    selectedList.remove(towingOptions[index]);
                }
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedTowingServices = "";
                for (int i = 0; i < selectedList.size(); i++) {
                    selectedTowingServices += selectedList.get(i);
                    if (i != selectedList.size() - 1) {
                        selectedTowingServices += ", ";
                    }
                }
                tvSelectedTowingServices.setText("Towing Services: " + selectedTowingServices);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void registerTowingService() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> towingData = new HashMap<>();
        towingData.put("userId", user.getUid());
        towingData.put("fullName", etFullName.getText().toString().trim());
        towingData.put("experience", etExperience.getText().toString().trim());
        towingData.put("permanentLocation", etPermanentLocation.getText().toString().trim());
        towingData.put("photo", base64Photo);
        towingData.put("license", base64License);
        towingData.put("policeVerification", base64PoliceVerification);
        towingData.put("currentLatitude", currentLatitude);
        towingData.put("currentLongitude", currentLongitude);
        towingData.put("towingServices", selectedTowingServices);

        databaseReference.child(user.getUid()).setValue(towingData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Towing Service Registered Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show());
    }
}
