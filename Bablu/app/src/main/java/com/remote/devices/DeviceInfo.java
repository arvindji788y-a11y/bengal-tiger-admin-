package com.remote.devices;

import com.google.gson.annotations.SerializedName;

/**
 * Data class to hold device information for registration.
 * This information is typically sent once when the device is first registered.
 */
public class DeviceInfo {

    @SerializedName("serialNumber")
    private String serialNumber;

    @SerializedName("model")
    private String model;

    @SerializedName("androidVersion")
    private String androidVersion;

    @SerializedName("sim1Number")
    private String sim1Number; // May be null if permission is not granted

    @SerializedName("sim2Number")
    private String sim2Number; // May be null or empty

    @SerializedName("registrationTimestamp")
    private long registrationTimestamp;

    public DeviceInfo(String serialNumber, String model, String androidVersion, String sim1Number, String sim2Number, long registrationTimestamp) {
        this.serialNumber = serialNumber;
        this.model = model;
        this.androidVersion = androidVersion;
        this.sim1Number = sim1Number;
        this.sim2Number = sim2Number;
        this.registrationTimestamp = registrationTimestamp;
    }
}
