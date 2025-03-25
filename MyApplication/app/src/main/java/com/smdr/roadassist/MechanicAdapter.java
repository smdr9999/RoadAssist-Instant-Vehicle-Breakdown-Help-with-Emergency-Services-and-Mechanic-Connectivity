package com.smdr.roadassist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;

public class MechanicAdapter extends RecyclerView.Adapter<MechanicAdapter.ViewHolder> {
    private List<MechanicModel> mechanicList;
    private Context context;
    // Reference to the "Users" node that stores phone numbers with userId as key.
    private DatabaseReference usersRef;

    // Driver details to be sent to the mechanic details:
    private String driverId;  // Driver's ID
    private double driverLatitude;  // Driver's current latitude
    private double driverLongitude; // Driver's current longitude

    public MechanicAdapter(List<MechanicModel> mechanicList, Context context) {
        this.mechanicList = mechanicList;
        this.context = context;
        usersRef = FirebaseDatabase.getInstance("https://roadassist-3f31b-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("Users");
    }

    // Setter for driverId
    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    // Setter for driver's current location.
    public void setDriverLocation(double latitude, double longitude) {
        this.driverLatitude = latitude;
        this.driverLongitude = longitude;
    }

    @NonNull
    @Override
    public MechanicAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mechanic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MechanicAdapter.ViewHolder holder, int position) {
        MechanicModel mechanic = mechanicList.get(position);

        // Set mechanic name and experience (use default values if null)
        String name = (mechanic.getName() != null) ? mechanic.getName() : "N/A";
        String exp = (mechanic.getExperience() != null) ? mechanic.getExperience() : "0";
        holder.txtName.setText(name);
        holder.txtExperience.setText("Exp: " + exp + " years");

        // Set distance if available.
        if (mechanic.getDistance() >= 0) {
            holder.txtDistance.setText(String.format(Locale.US, "%.1f km away", mechanic.getDistance()));
        } else {
            holder.txtDistance.setText("Distance unavailable");
        }

        // Decode Base64 photo.
        String base64Photo = (mechanic.getPhoto() != null) ? mechanic.getPhoto() : "";
        Bitmap photoBitmap = decodeBase64(base64Photo);
        if (photoBitmap != null) {
            holder.imgMechanic.setImageBitmap(photoBitmap);
        } else {
            // Use a default placeholder.
            holder.imgMechanic.setImageResource(android.R.drawable.ic_menu_help);
        }

        // Change button text to "Show".
        holder.btnRequest.setText("Show");

        // When the button is clicked, retrieve the mechanic's phone number from the "Users" node,
        // then launch MechanicDetailsActivity with all details along with driverId and driver's location.
        holder.btnRequest.setOnClickListener(v -> {
            String mechanicUserId = mechanic.getUserId();
            if (mechanicUserId == null) {
                launchMechanicDetailsActivity(mechanic, "Not Available");
            } else {
                usersRef.child(mechanicUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String phone = snapshot.getValue(String.class);
                        launchMechanicDetailsActivity(mechanic, (phone != null && !phone.isEmpty()) ? phone : "Not Available");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        launchMechanicDetailsActivity(mechanic, "Not Available");
                    }
                });
            }
        });
    }

    private void launchMechanicDetailsActivity(MechanicModel mechanic, String phone) {
        Intent intent = new Intent(context, MechanicDetailsActivity.class);
        // Mechanic details.
        intent.putExtra("fullName", (mechanic.getName() != null) ? mechanic.getName() : "N/A");
        intent.putExtra("photo", (mechanic.getPhoto() != null) ? mechanic.getPhoto() : "");
        intent.putExtra("experience", (mechanic.getExperience() != null) ? mechanic.getExperience() : "0");
        intent.putExtra("distance", mechanic.getDistance());
        intent.putExtra("latitude", mechanic.getCurrentLatitude());
        intent.putExtra("longitude", mechanic.getCurrentLongitude());
        intent.putExtra("phone", phone);
        intent.putExtra("userId", (mechanic.getUserId() != null) ? mechanic.getUserId() : "");
        // Send the driver's id and location.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        driverId = currentUser.getUid();
        intent.putExtra("driverId", driverId);
        intent.putExtra("driverLatitude", driverLatitude);
        intent.putExtra("driverLongitude", driverLongitude);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return mechanicList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtExperience, txtDistance;
        ImageView imgMechanic;
        Button btnRequest;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtExperience = itemView.findViewById(R.id.txtExperience);
            txtDistance = itemView.findViewById(R.id.txtDistance);
            imgMechanic = itemView.findViewById(R.id.imgMechanic);
            btnRequest = itemView.findViewById(R.id.btnRequest);
        }
    }

    // Helper method to decode a Base64-encoded string into a Bitmap.
    private Bitmap decodeBase64(String encodedImage) {
        if (encodedImage == null || encodedImage.isEmpty()) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Update mechanic list and refresh RecyclerView.
    public void updateMechanicList(List<MechanicModel> newList) {
        this.mechanicList = newList;
        notifyDataSetChanged();
    }
}
