package com.example.esim;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "esim_profiles")
public class EsimProfile {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name; // Optional name for the profile
    public String activationCode;
    public String matchingId;

    public EsimProfile(String name, String activationCode, String matchingId) {
        this.name = name;
        this.activationCode = activationCode;
        this.matchingId = matchingId;
    }

    // Getters and setters if needed, but for simplicity, public fields
}