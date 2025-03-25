package com.smdr.roadassist;

public class MechanicModel {
    // Fields matching the Realtime Database keys
    private String userId;            // Mechanic's Firebase user ID
    private String fullName;          // Stored as "fullName"
    private String photo;             // Stored as "photo" (Base64 encoded string)
    private double currentLatitude;   // Stored as "currentLatitude"
    private double currentLongitude;  // Stored as "currentLongitude"
    private String experience;        // Stored as "experience" (using String to match DB value)
    private String permanentLocation; // Stored as "permanentLocation"
    private String license;           // Stored as "license" (Base64 encoded string)
    private String policeVerification;// Stored as "policeVerification" (Base64 encoded string)
    private String repairTypes;       // New field: comma-separated vehicle types the mechanic can repair
    private double distance;          // Computed value (not stored in DB)

    // No-argument constructor required for Firebase deserialization
    public MechanicModel() { }

    // Parameterized constructor (optional, for manual creation)
    public MechanicModel(String userId, String fullName, String photo, double currentLatitude, double currentLongitude,
                         String experience, String license, String policeVerification, String permanentLocation,
                         String repairTypes) {
        this.userId = userId;
        this.fullName = fullName;
        this.photo = photo;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.experience = experience;
        this.license = license;
        this.policeVerification = policeVerification;
        this.permanentLocation = permanentLocation;
        this.repairTypes = repairTypes;
        this.distance = 0; // Default, to be computed later
    }

    // Getters and setters

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Full Name (alias: getName)
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getName() {
        return fullName;
    }

    // Photo (Base64 string)
    public String getPhoto() {
        return photo;
    }
    public void setPhoto(String photo) {
        this.photo = photo;
    }

    // Current Latitude
    public double getCurrentLatitude() {
        return currentLatitude;
    }
    public void setCurrentLatitude(double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    // Current Longitude
    public double getCurrentLongitude() {
        return currentLongitude;
    }
    public void setCurrentLongitude(double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    // Experience (as String)
    public String getExperience() {
        return experience;
    }
    public void setExperience(String experience) {
        this.experience = experience;
    }

    // Permanent Location
    public String getPermanentLocation() {
        return permanentLocation;
    }
    public void setPermanentLocation(String permanentLocation) {
        this.permanentLocation = permanentLocation;
    }

    // License (Base64 string)
    public String getLicense() {
        return license;
    }
    public void setLicense(String license) {
        this.license = license;
    }

    // Police Verification (Base64 string)
    public String getPoliceVerification() {
        return policeVerification;
    }
    public void setPoliceVerification(String policeVerification) {
        this.policeVerification = policeVerification;
    }

    // Repair Types (comma-separated string)
    public String getRepairTypes() {
        return repairTypes;
    }
    public void setRepairTypes(String repairTypes) {
        this.repairTypes = repairTypes;
    }

    // Computed distance (not stored in the database)
    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
}
