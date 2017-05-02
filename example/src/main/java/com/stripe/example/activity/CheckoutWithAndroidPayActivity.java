package com.stripe.example.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.stripe.example.R;
import com.stripe.wrap.pay.activity.StripeAppCompatActivity;

public class CheckoutWithAndroidPayActivity extends StripeAppCompatActivity {

    ViewGroup mFragmentContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_android_pay);
        mFragmentContainer = (ViewGroup) findViewById(R.id.android_pay_fragment_container);
    }

    @Override
    protected void onAndroidPayAvailable() {
        mFragmentContainer.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onAndroidPayNotAvailable() {
        mFragmentContainer.setVisibility(View.GONE);
    }

    @Override
    protected void addWalletFragment(@NonNull SupportWalletFragment walletFragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.android_pay_fragment_container, walletFragment).commit();
    }
}
