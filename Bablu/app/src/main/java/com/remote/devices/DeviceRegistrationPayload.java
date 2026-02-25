package com.remote.devices;

import com.google.gson.annotations.SerializedName;

public class DeviceRegistrationPayload {

    @SerializedName("deviceId")
    private String deviceId;

    @SerializedName("serialNumber")
    private String serialNumber;

    @SerializedName("model")
    private String model;

    @SerializedName("androidVersion")
    private String androidVersion;

    @SerializedName("sim1")
    private String sim1;

    @SerializedName("sim2")
    private String sim2;

    @SerializedName("battery")
    private int battery;

    @SerializedName("isOnline")
    private boolean isOnline;

    @SerializedName("lastSeen")
    private String lastSeen;

    public DeviceRegistrationPayload(String deviceId, String serialNumber, String model, String androidVersion, String sim1, String sim2, int battery, boolean isOnline, String lastSeen) {
        this.deviceId = deviceId;
        this.serialNumber = serialNumber;
        this.model = model;
        this.androidVersion = androidVersion;
        this.sim1 = sim1;
        this.sim2 = sim2;
        this.battery = battery;
        this.isOnline = isOnline;
        this.lastSeen = lastSeen;
    }
}
