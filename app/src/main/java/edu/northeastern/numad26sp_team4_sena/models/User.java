package edu.northeastern.numad26sp_team4_sena.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User {

    private String uid;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private String bio;
    private String profileImageUrl;
    private double latitude;
    private double longitude;
    private boolean isAvailable;
    private int linkUpCount;
    private List<String> friendIds;
    private transient double distanceMiles;

    @ServerTimestamp
    private Date createdAt;


    public User() {
        this.friendIds = new ArrayList<>();
        this.isAvailable = true;
        this.linkUpCount = 0;
    }

    public User(String uid, String fullName, String username, String email) {
        this.uid = uid;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.friendIds = new ArrayList<>();
        this.isAvailable = true;
        this.linkUpCount = 0;
        this.bio = "";
        this.phone = "";
        this.profileImageUrl = "";
    }


    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
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

    @PropertyName("isAvailable")
    public boolean isAvailable() {
        return isAvailable;
    }

    @PropertyName("isAvailable")
    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public int getLinkUpCount() {
        return linkUpCount;
    }

    public void setLinkUpCount(int linkUpCount) {
        this.linkUpCount = linkUpCount;
    }

    public List<String> getFriendIds() {
        return friendIds;
    }

    public void setFriendIds(List<String> friendIds) {
        this.friendIds = friendIds;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Exclude
    public double getDistanceMiles() {
        return distanceMiles;
    }

    public void setDistanceMiles(double distanceMiles) {
        this.distanceMiles = distanceMiles;
    }

}
