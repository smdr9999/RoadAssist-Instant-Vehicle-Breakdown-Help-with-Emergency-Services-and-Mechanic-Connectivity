package com.smdr.roadassist;

public class ServiceRequest {
    private String customerName;
    private String vehicleType;
    private String issueDetails;

    public ServiceRequest() { }

    public ServiceRequest(String customerName, String vehicleType, String issueDetails) {
        this.customerName = customerName;
        this.vehicleType = vehicleType;
        this.issueDetails = issueDetails;
    }

    public String getCustomerName() { return customerName; }
    public String getVehicleType() { return vehicleType; }
    public String getIssueDetails() { return issueDetails; }
}
