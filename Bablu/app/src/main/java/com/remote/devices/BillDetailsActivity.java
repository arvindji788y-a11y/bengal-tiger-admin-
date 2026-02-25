package com.remote.devices;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class BillDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_details);

        TextView customerNameTextView = findViewById(R.id.customer_name_text_view);
        TextView consumerNumberTextView = findViewById(R.id.consumer_number_text_view);

        String customerName = getIntent().getStringExtra("CUSTOMER_NAME");
        String consumerNumber = getIntent().getStringExtra("CONSUMER_NUMBER");

        customerNameTextView.setText(customerName);
        consumerNumberTextView.setText(consumerNumber);

        Button payNowButton = findViewById(R.id.pay_now_button);
        payNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BillDetailsActivity.this, PaymentActivity.class);
                intent.putExtra("CUSTOMER_NAME", customerName);
                intent.putExtra("CONSUMER_NUMBER", consumerNumber);
                startActivity(intent);
            }
        });
    }
}