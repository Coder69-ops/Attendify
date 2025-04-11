package com.example.attendify.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.Exclude;

public class Office implements Parcelable {
    private String id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private double radius;
    private String entryTime;
    private int startHour;
    private int startMinute;
    private int endHour;
    private int endMinute;
    private int checkInRadius;
    private int stability;

    // Required empty constructor for Firestore
    public Office() {
    }

    public Office(String id, String name, double latitude, double longitude, double radius, String entryTime) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.entryTime = entryTime;
    }

    protected Office(Parcel in) {
        id = in.readString();
        name = in.readString();
        address = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        radius = in.readDouble();
        entryTime = in.readString();
        startHour = in.readInt();
        startMinute = in.readInt();
        endHour = in.readInt();
        endMinute = in.readInt();
        checkInRadius = in.readInt();
        stability = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(address);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(radius);
        dest.writeString(entryTime);
        dest.writeInt(startHour);
        dest.writeInt(startMinute);
        dest.writeInt(endHour);
        dest.writeInt(endMinute);
        dest.writeInt(checkInRadius);
        dest.writeInt(stability);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Office> CREATOR = new Creator<Office>() {
        @Override
        public Office createFromParcel(Parcel in) {
            return new Office(in);
        }

        @Override
        public Office[] newArray(int size) {
            return new Office[size];
        }
    };

    @Exclude
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(String entryTime) {
        this.entryTime = entryTime;
    }

    public int getStartHour() {
        return startHour;
    }

    public void setStartHour(int startHour) {
        this.startHour = startHour;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(int startMinute) {
        this.startMinute = startMinute;
    }

    public int getEndHour() {
        return endHour;
    }

    public void setEndHour(int endHour) {
        this.endHour = endHour;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public void setEndMinute(int endMinute) {
        this.endMinute = endMinute;
    }

    public int getStability() {
        return stability;
    }

    public void setStability(int stability) {
        this.stability = stability;
    }

    public int getCheckInRadius() {
        return checkInRadius;
    }

    public void setCheckInRadius(int checkInRadius) {
        this.checkInRadius = checkInRadius;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Office office = (Office) o;
        return Double.compare(office.latitude, latitude) == 0 &&
                Double.compare(office.longitude, longitude) == 0 &&
                Double.compare(office.radius, radius) == 0 &&
                startHour == office.startHour &&
                startMinute == office.startMinute &&
                endHour == office.endHour &&
                endMinute == office.endMinute &&
                checkInRadius == office.checkInRadius &&
                (id == null ? office.id == null : id.equals(office.id)) &&
                (name == null ? office.name == null : name.equals(office.name)) &&
                (address == null ? office.address == null : address.equals(office.address)) &&
                (entryTime == null ? office.entryTime == null : entryTime.equals(office.entryTime));
    }
}