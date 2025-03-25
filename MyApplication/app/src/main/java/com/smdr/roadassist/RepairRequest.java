package com.smdr.roadassist;

public class RepairRequest {
    private String driverName;
    private String driverPhone;
    private String driverVehicle;
    private String driverLocation;
    private String message;
    private long timestamp; // Optional, to record when the request was sent

    // No-argument constructor required for Firebase deserialization
    public RepairRequest() { }

    public RepairRequest(String driverName, String driverPhone, String driverVehicle, String driverLocation, String message, long timestamp) {
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.driverVehicle = driverVehicle;
        this.driverLocation = driverLocation;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public String getDriverVehicle() {
        return driverVehicle;
    }

    public void setDriverVehicle(String driverVehicle) {
        this.driverVehicle = driverVehicle;
    }

    public String getDriverLocation() {
        return driverLocation;
    }

    public void setDriverLocation(String driverLocation) {
        this.driverLocation = driverLocation;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
