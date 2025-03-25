package com.smdr.roadassist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;

import java.util.List;

public class ServiceRequestAdapter extends ArrayAdapter<ServiceRequest> {
    public ServiceRequestAdapter(@NonNull Context context, List<ServiceRequest> requests) {
        super(context, 0, requests);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_service_request, parent, false);
        }

        ServiceRequest request = getItem(position);

        TextView tvCustomerName = convertView.findViewById(R.id.tvCustomerName);
        TextView tvVehicleType = convertView.findViewById(R.id.tvVehicleType);
        TextView tvIssueDetails = convertView.findViewById(R.id.tvIssueDetails);

        if (request != null) {
            tvCustomerName.setText("Customer: " + request.getCustomerName());
            tvVehicleType.setText("Vehicle: " + request.getVehicleType());
            tvIssueDetails.setText("Issue: " + request.getIssueDetails());
        }

        return convertView;
    }
}
