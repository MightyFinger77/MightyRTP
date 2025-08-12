package com.example.mightyrtp.utils;

import org.bukkit.Location;

/**
 * Represents the result of a teleport location search
 */
public class TeleportResult {
    public enum Status {
        SUCCESS,
        FAILURE,
        TIMEOUT
    }
    
    private final Status status;
    private final Location location;
    private final String message;
    
    private TeleportResult(Status status, Location location, String message) {
        this.status = status;
        this.location = location;
        this.message = message;
    }
    
    public static TeleportResult success(Location location) {
        return new TeleportResult(Status.SUCCESS, location, null);
    }
    
    public static TeleportResult failure(String message) {
        return new TeleportResult(Status.FAILURE, null, message);
    }
    
    public static TeleportResult timeout(String message) {
        return new TeleportResult(Status.TIMEOUT, null, message);
    }
    
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    public boolean isTimeout() {
        return status == Status.TIMEOUT;
    }
    
    public boolean isFailure() {
        return status == Status.FAILURE;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Status getStatus() {
        return status;
    }
}
