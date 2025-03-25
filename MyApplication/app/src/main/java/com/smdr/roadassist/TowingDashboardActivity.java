package com.smdr.roadassist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class TowingDashboardActivity extends AppCompatActivity {
    private Button btnViewProfile, btnViewServiceRequests, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_towing_dashboard);

        btnViewProfile = findViewById(R.id.btnViewProfile);
        btnViewServiceRequests = findViewById(R.id.btnViewServiceRequests);
        btnLogout = findViewById(R.id.btnLogout);

        btnViewProfile.setOnClickListener(v -> startActivity(new Intent(this, TowingProfileActivity.class)));
        btnViewServiceRequests.setOnClickListener(v -> startActivity(new Intent(this, ServiceRequestsActivity.class)));
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }
}
