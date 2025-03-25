package com.smdr.roadassist;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class RepairRequestAdapter extends RecyclerView.Adapter<RepairRequestAdapter.ViewHolder> {

    private List<RepairRequestModel> requestList;
    private Context context;

    public RepairRequestAdapter(List<RepairRequestModel> requestList, Context context) {
        this.requestList = requestList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_repair_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RepairRequestModel request = requestList.get(position);
        holder.tvDriverName.setText("Driver: " + request.getDriverName());
        holder.tvDriverPhone.setText("Phone: " + request.getDriverPhone());
        holder.tvDriverVehicle.setText("Vehicle: " + request.getDriverVehicle());
        holder.tvMessage.setText("Message: " + (request.getMessage() != null ? request.getMessage() : "No message"));
        holder.tvDistance.setText(String.format(Locale.US, "Distance: %.1f km", request.getDistance()));

        // When a request is clicked, launch the detailed view.
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RepairRequestDetailsActivity.class);
            intent.putExtra("driverName", request.getDriverName());
            intent.putExtra("driverPhone", request.getDriverPhone());
            intent.putExtra("driverVehicle", request.getDriverVehicle());
            intent.putExtra("message", request.getMessage());
            intent.putExtra("distance", request.getDistance());
            intent.putExtra("driverLatitude", request.getDriverLatitude());
            intent.putExtra("driverLongitude", request.getDriverLongitude());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName, tvDriverPhone, tvDriverVehicle, tvMessage, tvDistance;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvDriverPhone = itemView.findViewById(R.id.tvDriverPhone);
            tvDriverVehicle = itemView.findViewById(R.id.tvDriverVehicle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvDistance = itemView.findViewById(R.id.tvDistance);
        }
    }

    public void updateRequestList(List<RepairRequestModel> newList) {
        this.requestList = newList;
        notifyDataSetChanged();
    }
}
