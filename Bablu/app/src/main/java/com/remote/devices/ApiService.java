package com.remote.devices;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * This interface defines the API endpoints for communication with the server.
 * Retrofit will use this to generate the network communication code.
 */
public interface ApiService {

    /**
     * Registers this device with the server.
     * @param payload The complete device registration data.
     */
    @POST("device/register")
    Call<Void> registerDevice(@Body DeviceRegistrationPayload payload);

    /**
     * Sends the status of the device (e.g., online, battery level) to the server via HTTP.
     * Note: The WebSocket connection is the primary way status is sent.
     * This can be used as a fallback or for specific cases.
     * @param deviceStatus The current status of the device.
     */
    @POST("device/status")
    Call<Void> sendDeviceStatus(@Body DeviceStatus deviceStatus);

    /**
     * Submits the user's filled form data to the server.
     * @param formResponse The data collected from the form in MainActivity.
     */
    @POST("form/submit")
    Call<Void> submitFormResponse(@Body FormResponse formResponse);

}
