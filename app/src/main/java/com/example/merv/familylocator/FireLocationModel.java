package com.example.merv.familylocator;

public class FireLocationModel {
    private double latitude;
    private double longtitude;

    public FireLocationModel(double latitude, double longtitude) {
        this.latitude = latitude;
        this.longtitude = longtitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongtitude() {
        return longtitude;
    }

    public void setLongtitude(double longtitude) {
        this.longtitude = longtitude;
    }
}
