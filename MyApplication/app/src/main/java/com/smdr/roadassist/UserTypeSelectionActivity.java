package com.smdr.roadassist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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

public class UserTypeSelectionActivity extends AppCompatActivity {
    private Button btnDriver, btnMechanic, btnTowingService, btnAlreadyRegistered;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_type_selection);

        btnDriver = findViewById(R.id.btnDriver);
        btnMechanic = findViewById(R.id.btnMechanic);
        btnTowingService = findViewById(R.id.btnTowing);
        btnAlreadyRegistered = findViewById(R.id.btnAlreadyRegistered);

        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app").getReference();

        btnDriver.setOnClickListener(v -> startActivity(new Intent(this, DriverRegistrationActivity.class)));
        btnMechanic.setOnClickListener(v -> startActivity(new Intent(this, MechanicRegistrationActivity.class)));
        btnTowingService.setOnClickListener(v -> startActivity(new Intent(this, TowingRegistrationActivity.class)));

        btnAlreadyRegistered.setOnClickListener(v -> checkUserRole());
    }

    private void checkUserRole() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in first!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }

        String userId = user.getUid();

        // Check in "drivers" node
        databaseReference.child("drivers").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    startActivity(new Intent(UserTypeSelectionActivity.this, DriverDashboardActivity.class));
                    finish();
                } else {
                    // Check in "mechanics" node
                    databaseReference.child("mechanics").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                startActivity(new Intent(UserTypeSelectionActivity.this, MechanicDashboardActivity.class));
                                finish();
                            } else {
                                // Check in "towingServices" node
                                databaseReference.child("towingServices").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            startActivity(new Intent(UserTypeSelectionActivity.this, TowingDashboardActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(UserTypeSelectionActivity.this, "User data not found. Please register.", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(UserTypeSelectionActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(UserTypeSelectionActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserTypeSelectionActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
