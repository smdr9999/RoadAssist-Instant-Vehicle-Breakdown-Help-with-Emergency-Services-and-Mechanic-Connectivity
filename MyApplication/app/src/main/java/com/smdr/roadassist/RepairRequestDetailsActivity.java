package com.smdr.roadassist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class RepairRequestDetailsActivity extends AppCompatActivity {

    private TextView tvDriverName, tvDriverPhone, tvDriverVehicle, tvMessage, tvDistance;
    private Button btnLocate;
    private double driverLatitude, driverLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair_request_details);

        tvDriverName = findViewById(R.id.tvDriverName);
        tvDriverPhone = findViewById(R.id.tvDriverPhone);
        tvDriverVehicle = findViewById(R.id.tvDriverVehicle);
        tvMessage = findViewById(R.id.tvMessage);
        tvDistance = findViewById(R.id.tvDistance);
        btnLocate = findViewById(R.id.btnLocate);

        // Retrieve details from intent extras.
        Intent intent = getIntent();
        String driverName = intent.getStringExtra("driverName");
        String driverPhone = intent.getStringExtra("driverPhone");
        String driverVehicle = intent.getStringExtra("driverVehicle");
        String message = intent.getStringExtra("message");
        double distance = intent.getDoubleExtra("distance", 0.0);
        driverLatitude = intent.getDoubleExtra("driverLatitude", 0.0);
        driverLongitude = intent.getDoubleExtra("driverLongitude", 0.0);

        tvDriverName.setText("Driver: " + (driverName != null ? driverName : "N/A"));
        tvDriverPhone.setText("Phone: " + (driverPhone != null ? driverPhone : "N/A"));
        tvDriverVehicle.setText("Vehicle: " + (driverVehicle != null ? driverVehicle : "N/A"));
        tvMessage.setText("Message: " + (message != null ? message : "No message"));
        tvDistance.setText(String.format(Locale.US, "Distance: %.1f km", distance));

        btnLocate.setOnClickListener(v -> {
            if (driverLatitude == 0.0 && driverLongitude == 0.0) {
                Toast.makeText(RepairRequestDetailsActivity.this, "Driver location not available", Toast.LENGTH_SHORT).show();
            } else {
                String uri = "google.navigation:q=" + driverLatitude + "," + driverLongitude;
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(RepairRequestDetailsActivity.this, "Google Maps not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
