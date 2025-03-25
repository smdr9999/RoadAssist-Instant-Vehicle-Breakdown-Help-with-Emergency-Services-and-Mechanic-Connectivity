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

public class TowingProfileActivity extends AppCompatActivity {

    private TextView tvFullName, tvTowingServices, tvExperience;
    private ImageView imgProfile;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_towing_profile);

        tvFullName = findViewById(R.id.tvFullName);
        tvTowingServices = findViewById(R.id.tvTowingServices);
        tvExperience = findViewById(R.id.tvExperience);
        imgProfile = findViewById(R.id.imgProfile);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = currentUser.getUid();

        // Connect to the "towingCompanies" node using your Realtime Database URL.
        dbRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("towingServices").child(userId);

        loadTowingProfile();
    }

    private void loadTowingProfile() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    String towingServices = snapshot.child("towingServices").getValue(String.class);
                    String experience = snapshot.child("experience").getValue(String.class);
                    String encodedImage = snapshot.child("photo").getValue(String.class);

                    tvFullName.setText("Full Name: " + (fullName != null ? fullName : "N/A"));
                    tvTowingServices.setText("Towing Services: " + (towingServices != null ? towingServices : "N/A"));
                    tvExperience.setText("Experience: " + (experience != null ? experience : "N/A"));

                    if (encodedImage != null && !encodedImage.isEmpty()) {
                        Bitmap bitmap = decodeBase64(encodedImage);
                        if (bitmap != null) {
                            imgProfile.setImageBitmap(bitmap);
                        } else {
                            imgProfile.setImageResource(android.R.drawable.ic_menu_help);
                        }
                    } else {
                        imgProfile.setImageResource(android.R.drawable.ic_menu_help);
                    }
                } else {
                    Toast.makeText(TowingProfileActivity.this, "Profile not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(TowingProfileActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Helper method to decode Base64-encoded images.
    private Bitmap decodeBase64(String encodedImage) {
        try {
            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }
}
