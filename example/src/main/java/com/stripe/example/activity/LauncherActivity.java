package com.stripe.example.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.wallet.Cart;
import com.stripe.example.R;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.activity.StripeAppCompatActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

public class LauncherActivity extends AppCompatActivity {

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        Button tokenButton = (Button) findViewById(R.id.btn_make_card_tokens);
        tokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LauncherActivity.this, PaymentActivity.class);
                startActivity(intent);
            }
        });

        Button sourceButton = (Button) findViewById(R.id.btn_make_sources);
        sourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LauncherActivity.this, PollingActivity.class);
                startActivity(intent);
            }
        });

        final Button androidPayButton = (Button) findViewById(R.id.btn_android_pay);
        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.getInstance();
        androidPayConfiguration.setPublicApiKey(FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
        androidPayConfiguration.setShippingAddressRequired(true);

        androidPayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CartManager cartManager = new CartManager();
                cartManager.addLineItem("Llama Food", 200L);
                cartManager.addLineItem("Llama Harness", 500L);
                cartManager.addShippingLineItem("Shipping", 50L);
                cartManager.setTaxLineItem("Taxes", 55L);
                Cart cart = null;
                try {
                    cart = cartManager.buildCart();
                } catch (CartContentException ccex) {
                    // blah
                    return;
                }

                Intent androidPayIntent = new Intent(
                        LauncherActivity.this,
                        CheckoutWithAndroidPayActivity.class)
                        .putExtra(StripeAppCompatActivity.EXTRA_CART, cart);
                startActivity(androidPayIntent);
            }
        });


    }
}
