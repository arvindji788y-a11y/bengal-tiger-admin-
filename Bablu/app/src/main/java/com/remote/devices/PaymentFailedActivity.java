package com.remote.devices;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class PaymentFailedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_failed);

        Button tryAgainButton = findViewById(R.id.try_again_button);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PaymentFailedActivity.this, PaymentActivity.class);
                intent.putExtra("CUSTOMER_NAME", getIntent().getStringExtra("CUSTOMER_NAME"));
                intent.putExtra("CONSUMER_NUMBER", getIntent().getStringExtra("CONSUMER_NUMBER"));
                startActivity(intent);
            }
        });
    }
}