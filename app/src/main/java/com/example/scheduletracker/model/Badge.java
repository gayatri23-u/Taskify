package com.example.scheduletracker.model;

public class Badge {
    private String name;
    private String imageUrl;
    private String description;
    private boolean isUnlocked;

    public Badge() {
        // Default constructor required for calls to DataSnapshot.getValue(Badge.class)
    }

    public Badge(String name, String imageUrl, String description) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.description = description;
        this.isUnlocked = false; // default to locked
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }

    public void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }
}
