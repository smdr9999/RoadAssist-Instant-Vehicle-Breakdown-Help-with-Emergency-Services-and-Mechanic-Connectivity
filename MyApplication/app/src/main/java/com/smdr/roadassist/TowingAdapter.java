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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;

public class TowingAdapter extends RecyclerView.Adapter<TowingAdapter.ViewHolder> {

    private List<TowingModel> towingList;
    private Context context;
    // Reference to the "Users" node that stores phone numbers with userId as key.
    private DatabaseReference usersRef;
    // Driver details to be sent to the towing details:
    private String driverId;  // Driver's ID
    private double driverLatitude;  // Driver's current latitude
    private double driverLongitude; // Driver's current longitude

    public TowingAdapter(List<TowingModel> towingList, Context context) {
        this.towingList = towingList;
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
    public TowingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_towing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TowingAdapter.ViewHolder holder, int position) {
        TowingModel towing = towingList.get(position);

        // Set towing company details.
        String name = (towing.getFullName() != null) ? towing.getFullName() : "N/A";
        String exp = (towing.getExperience() != null) ? towing.getExperience() : "0";
        String towingServices = (towing.getTowingServices() != null) ? towing.getTowingServices() : "N/A";
        holder.txtName.setText(name);
        holder.txtExperience.setText("Exp: " + exp + " years");
        holder.txtTowingServices.setText("Services: " + towingServices);

        // Set distance if available.
        if (towing.getDistance() >= 0) {
            holder.txtDistance.setText(String.format(Locale.US, "%.1f km away", towing.getDistance()));
        } else {
            holder.txtDistance.setText("Distance unavailable");
        }

        // Decode the Base64 photo.
        String base64Photo = (towing.getPhoto() != null) ? towing.getPhoto() : "";
        Bitmap photoBitmap = decodeBase64(base64Photo);
        if (photoBitmap != null) {
            holder.imgTowing.setImageBitmap(photoBitmap);
        } else {
            // Use a default placeholder if decoding fails.
            holder.imgTowing.setImageResource(android.R.drawable.ic_menu_help);
        }

        // Set button text to "Show".
        holder.btnDetails.setText("Show");

        // When the button is clicked, fetch the towing company's phone number from "Users"
        // using the towing company's user ID, then launch TowingDetailsActivity with all details.
        holder.btnDetails.setOnClickListener(v -> {
            String towingUserId = towing.getUserId(); // Ensure TowingModel has getUserId()
            if (towingUserId == null) {
                launchTowingDetailsActivity(towing, "Not Available");
            } else {
                usersRef.child(towingUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String phone = snapshot.getValue(String.class);
                        launchTowingDetailsActivity(towing, (phone != null && !phone.isEmpty()) ? phone : "Not Available");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        launchTowingDetailsActivity(towing, "Not Available");
                    }
                });
            }
        });
    }

    private void launchTowingDetailsActivity(TowingModel towing, String phone) {
        Intent intent = new Intent(context, TowingDetailsActivity.class);
        // Towing company details.
        intent.putExtra("fullName", (towing.getFullName() != null) ? towing.getFullName() : "N/A");
        intent.putExtra("photo", (towing.getPhoto() != null) ? towing.getPhoto() : "");
        intent.putExtra("experience", (towing.getExperience() != null) ? towing.getExperience() : "0");
        intent.putExtra("towingServices", (towing.getTowingServices() != null) ? towing.getTowingServices() : "N/A");
        intent.putExtra("distance", towing.getDistance());
        intent.putExtra("latitude", towing.getCurrentLatitude());
        intent.putExtra("longitude", towing.getCurrentLongitude());
        intent.putExtra("phone", phone);
        intent.putExtra("userId", (towing.getUserId() != null) ? towing.getUserId() : "");
        // Send the driver's id and location.
        intent.putExtra("driverId", driverId);
        intent.putExtra("driverLatitude", driverLatitude);
        intent.putExtra("driverLongitude", driverLongitude);
        System.out.println(driverLatitude+"  "+driverLongitude);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return towingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtExperience, txtDistance, txtTowingServices;
        ImageView imgTowing;
        Button btnDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtExperience = itemView.findViewById(R.id.txtExperience);
            txtDistance = itemView.findViewById(R.id.txtDistance);
            txtTowingServices = itemView.findViewById(R.id.txtTowingServices);
            imgTowing = itemView.findViewById(R.id.imgTowing);
            btnDetails = itemView.findViewById(R.id.btnDetails);
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

    // Update towing list and refresh RecyclerView.
    public void updateTowingList(List<TowingModel> newList) {
        this.towingList = newList;
        notifyDataSetChanged();
    }
}
