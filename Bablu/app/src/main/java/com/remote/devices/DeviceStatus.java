package com.remote.devices;

import com.google.gson.annotations.SerializedName;

/**
 * Data class to hold the dynamic status of the device.
 * This information is sent periodically to the server.
 */
public class DeviceStatus {

    @SerializedName("serialNumber")
    private String serialNumber;

    @SerializedName("isOnline")
    private boolean isOnline;

    @SerializedName("batteryPercentage")
    private int batteryPercentage;

    // The last time the device was confirmed to be online.
    @SerializedName("lastSeenTimestamp")
    private long lastSeenTimestamp;

    public DeviceStatus(String serialNumber, boolean isOnline, int batteryPercentage, long lastSeenTimestamp) {
        this.serialNumber = serialNumber;
        this.isOnline = isOnline;
        this.batteryPercentage = batteryPercentage;
        this.lastSeenTimestamp = lastSeenTimestamp;
    }
}
