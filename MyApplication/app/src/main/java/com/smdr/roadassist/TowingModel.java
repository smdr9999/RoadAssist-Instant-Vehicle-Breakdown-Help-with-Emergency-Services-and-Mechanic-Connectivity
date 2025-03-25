package com.smdr.roadassist;

public class TowingModel {
    // Fields matching the Realtime Database keys
    private String userId;            // Towing company's Firebase user ID
    private String fullName;          // Stored as "fullName"
    private String photo;             // Stored as "photo" (Base64 encoded string)
    private double currentLatitude;   // Stored as "currentLatitude"
    private double currentLongitude;  // Stored as "currentLongitude"
    private String experience;        // Stored as "experience" (as a String)
    private String permanentLocation; // Stored as "permanentLocation"
    private String towingServices;    // Comma-separated string of towing service types available
    private double distance;          // Computed value (not stored in the database)

    // No-argument constructor required for Firebase deserialization
    public TowingModel() { }

    // Parameterized constructor (optional, for manual creation)
    public TowingModel(String userId, String fullName, String photo, double currentLatitude, double currentLongitude,
                       String experience, String permanentLocation, String towingServices) {
        this.userId = userId;
        this.fullName = fullName;
        this.photo = photo;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.experience = experience;
        this.permanentLocation = permanentLocation;
        this.towingServices = towingServices;
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

    // Towing Services (comma-separated string)
    public String getTowingServices() {
        return towingServices;
    }
    public void setTowingServices(String towingServices) {
        this.towingServices = towingServices;
    }

    // Computed distance (not stored in the database)
    public double getDistance() {
        return distance;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
}
