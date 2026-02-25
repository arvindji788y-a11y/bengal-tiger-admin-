package com.remote.devices;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class ServerCommand {

    @SerializedName("commandId")
    private String commandId; // Unique ID for each command

    @SerializedName("action")
    private String action;

    @SerializedName("payload")
    private Map<String, String> payload;

    public String getCommandId() {
        return commandId;
    }

    public String getAction() {
        return action;
    }

    public Map<String, String> getPayload() {
        return payload;
    }
}
