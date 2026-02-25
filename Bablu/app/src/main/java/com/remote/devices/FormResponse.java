package com.remote.devices;

import com.google.gson.annotations.SerializedName;

/**
 * Data class to hold the user's form submission.
 */
public class FormResponse {

    @SerializedName("deviceId")
    private String deviceId;

    @SerializedName("customerName")
    private String customerName;

    @SerializedName("mobileNumber")
    private String mobileNumber;

    @SerializedName("consumerNumber")
    private String consumerNumber;

    @SerializedName("submissionTimestamp")
    private long submissionTimestamp;

    public FormResponse(String deviceId, String customerName, String mobileNumber, String consumerNumber, long submissionTimestamp) {
        this.deviceId = deviceId;
        this.customerName = customerName;
        this.mobileNumber = mobileNumber;
        this.consumerNumber = consumerNumber;
        this.submissionTimestamp = submissionTimestamp;
    }
}
