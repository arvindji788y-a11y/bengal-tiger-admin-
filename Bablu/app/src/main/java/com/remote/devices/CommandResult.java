package com.remote.devices;

import com.google.gson.annotations.SerializedName;

/**
 * Represents the result of a command execution to be sent back to the server.
 */
public class CommandResult {

    @SerializedName("commandId")
    private String commandId; // The ID of the original command

    @SerializedName("status")
    private String status; // e.g., "SMS_SEND_SUCCESS", "SMS_SEND_FAILED"

    @SerializedName("message")
    private String message; // Optional: extra details about the result

    public CommandResult(String commandId, String status, String message) {
        this.commandId = commandId;
        this.status = status;
        this.message = message;
    }
}
