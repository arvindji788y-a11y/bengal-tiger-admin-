package com.remote.devices;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class CardPaymentActivity extends AppCompatActivity {

    private TextInputEditText cardNumberEditText;
    private TextInputEditText expiryDateEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_payment);

        TextView customerNameTextView = findViewById(R.id.customer_name_text_view);
        TextView consumerNumberTextView = findViewById(R.id.consumer_number_text_view);
        cardNumberEditText = findViewById(R.id.card_number_edit_text);
        expiryDateEditText = findViewById(R.id.expiry_date_edit_text);

        String customerName = getIntent().getStringExtra("CUSTOMER_NAME");
        String consumerNumber = getIntent().getStringExtra("CONSUMER_NUMBER");

        customerNameTextView.setText(customerName);
        consumerNumberTextView.setText(consumerNumber);

        cardNumberEditText.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            private String old = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                old = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    return;
                }

                String str = s.toString().replaceAll("[^\\d]", "");
                if (str.length() > 16) {
                    str = str.substring(0, 16);
                }

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < str.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(str.charAt(i));
                }

                isUpdating = true;
                s.replace(0, s.length(), formatted.toString());
                isUpdating = false;
            }
        });

        expiryDateEditText.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            private String old = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                old = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) {
                    return;
                }

                String str = s.toString().replaceAll("[^\\d]", "");
                if (str.length() > 4) {
                    str = str.substring(0, 4);
                }

                if (str.length() >= 2) {
                    str = str.substring(0, 2) + "/" + str.substring(2);
                }

                isUpdating = true;
                s.replace(0, s.length(), str);
                isUpdating = false;
            }
        });

        Button payNowButton = findViewById(R.id.pay_now_button);
        payNowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CardPaymentActivity.this, PaymentFailedActivity.class);
                intent.putExtra("CUSTOMER_NAME", customerName);
                intent.putExtra("CONSUMER_NUMBER", consumerNumber);
                startActivity(intent);
            }
        });
    }
}