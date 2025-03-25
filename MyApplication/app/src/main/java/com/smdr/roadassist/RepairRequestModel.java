package com.smdr.roadassist;

import static android.content.ContentValues.TAG;

import android.util.Log;

public class RepairRequestModel {
    private String driverName;
    private String driverPhone;
    private String driverVehicle;
    private String message;
    private double distance;
    private double driverLatitude;
    private double driverLongitude;
    private long timestamp;

    public RepairRequestModel() {
        // Default constructor required for Firebase deserialization
    }

    // Getters and setters
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }

    public String getDriverVehicle() { return driverVehicle; }
    public void setDriverVehicle(String driverVehicle) { this.driverVehicle = driverVehicle; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public double getDistance() {
        Log.d(TAG,"Message: "+ distance);
        return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public double getDriverLatitude() { return driverLatitude; }
    public void setDriverLatitude(double driverLatitude) { this.driverLatitude = driverLatitude; }

    public double getDriverLongitude() { return driverLongitude; }
    public void setDriverLongitude(double driverLongitude) { this.driverLongitude = driverLongitude; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
