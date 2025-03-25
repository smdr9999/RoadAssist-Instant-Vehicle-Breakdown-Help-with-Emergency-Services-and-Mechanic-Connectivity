package com.smdr.roadassist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
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

public class MechanicProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private TextView tvMechanicName, tvExperience;
    private ImageView ivMechanicPhoto, ivMechanicLicense, ivMechanicPoliceVerification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mechanic_profile);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        tvMechanicName = findViewById(R.id.tvMechanicName);
        tvExperience = findViewById(R.id.tvExperience);
        ivMechanicPhoto = findViewById(R.id.ivMechanicPhoto);
        ivMechanicLicense = findViewById(R.id.ivMechanicLicense);
        ivMechanicPoliceVerification = findViewById(R.id.ivMechanicPoliceVerification);

        // Get current logged-in user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseReference = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app").getReference("mechanics").child(userId);
            fetchMechanicDetails();
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchMechanicDetails() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Fetch and display text data
                    if (snapshot.hasChild("fullName")) {
                        tvMechanicName.setText(snapshot.child("fullName").getValue(String.class));
                    }
                    if (snapshot.hasChild("experience")) {
                        tvExperience.setText(snapshot.child("experience").getValue(String.class) + " years");
                    }

                    // Fetch and display images
                    if (snapshot.hasChild("photo")) {
                        ivMechanicPhoto.setImageBitmap(decodeBase64(snapshot.child("photo").getValue(String.class)));
                    }
                    if (snapshot.hasChild("license")) {
                        ivMechanicLicense.setImageBitmap(decodeBase64(snapshot.child("license").getValue(String.class)));
                    }
                    if (snapshot.hasChild("policeVerification")) {
                        ivMechanicPoliceVerification.setImageBitmap(decodeBase64(snapshot.child("policeVerification").getValue(String.class)));
                    }
                } else {
                    Toast.makeText(MechanicProfileActivity.this, "Mechanic profile not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MechanicProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                error.toException().printStackTrace();
            }
        });
    }

    private Bitmap decodeBase64(String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) return null;
        byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }
}
