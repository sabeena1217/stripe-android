package com.stripe.wrap.pay.testharness;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.stripe.wrap.pay.activity.StripeAppCompatActivity;

public class StripeAndroidPayTestActivity extends StripeAppCompatActivity {

    AndroidPayAvailabilityChooser mAndroidPayAvailabilityChooser;
    GoogleApiClientMockBuilder mGoogleApiClientMockBuilder;
    StripeAppCompatActivityListener mListener;

    public void setStripeAppCompatActivityListener(StripeAppCompatActivityListener listener) {
        mListener = listener;
    }

    public void setGoogleApiClientMockBuilder(GoogleApiClientMockBuilder builder) {
        mGoogleApiClientMockBuilder = builder;
    }

    public void setAndroidPayAvailabilityChooser(AndroidPayAvailabilityChooser chooser) {
        mAndroidPayAvailabilityChooser = chooser;
    }

    @NonNull
    @Override
    protected GoogleApiClient buildGoogleApiClient() {
        return mGoogleApiClientMockBuilder.getMockGoogleApiClient();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mListener != null) {
            mListener.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected int getWalletEnvironment() {
        int walletEnvironment = super.getWalletEnvironment();
        if (mListener != null) {
            mListener.getWalletEnvironment(walletEnvironment);
        }
        return walletEnvironment;
    }

    @Override
    protected int getWalletTheme() {
        int walletTheme = super.getWalletTheme();
        if (mListener  != null) {
            mListener.getWalletTheme(walletTheme);
        }
        return walletTheme;
    }

    @Override
    protected void addWalletFragment(@NonNull SupportWalletFragment walletFragment) {
        if (mListener != null) {
            mListener.addWalletFragment(walletFragment);
        }
    }

    @Override
    protected void onBeforeAndroidPayAvailable() {
        super.onBeforeAndroidPayAvailable();
        if (mListener != null) {
            mListener.onBeforeAndroidPayAvailable();
        }
    }

    @Override
    protected void onAfterAndroidPayCheckComplete() {
        super.onAfterAndroidPayCheckComplete();
        if (mListener != null) {
            mListener.onAfterAndroidPayCheckComplete();
        }
    }

    @Override
    protected void onAndroidPayAvailable() {
        if (mListener != null) {
            mListener.onAndroidPayAvaliable();
        }
    }

    @Override
    protected void onAndroidPayNotAvailable() {
        if (mListener != null) {
            mListener.onAndroidPayNotAvailable();
        }
    }

    @Override
    protected void onMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) {

    }

    @Override
    protected void onFullWalletRetrieved(@NonNull FullWallet fullWallet) {

    }

    @Override
    protected void verifyAndPrepareAndroidPayControls(
            @NonNull GoogleApiClient googleApiClient,
            @NonNull IsReadyToPayRequest isReadyToPayRequest) {
        if (mListener != null && mAndroidPayAvailabilityChooser != null) {
            mListener.verifyAndPrepareAndroidPayControls(isReadyToPayRequest);
            // Simulate the check completing
            onAfterAndroidPayCheckComplete();
            BooleanResult result = mAndroidPayAvailabilityChooser.doesAndroidPayCheckSucceed();
            if (result.getStatus().isSuccess() && result.getValue()) {
                createAndAddWalletFragment();
            } else {
                onAndroidPayNotAvailable();
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        super.onConnectionFailed(connectionResult);
        if (mListener != null) {
            mListener.onConnectionFailed(connectionResult);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        super.onConnectionSuspended(i);
        if (mListener != null) {
            mListener.onConnectionSuspended(i);
        }
    }

    public interface StripeAppCompatActivityListener {
        void addWalletFragment(SupportWalletFragment walletFragment);
        void onActivityResult(int requestCode, int resultCode, Intent data);
        void onAfterAndroidPayCheckComplete();
        void onAndroidPayAvaliable();
        void onAndroidPayNotAvailable();
        void onBeforeAndroidPayAvailable();
        void onConnectionFailed(@NonNull ConnectionResult connectionResult);
        void onConnectionSuspended(int i);
        void getWalletEnvironment(int walletEnvironMent);
        void getWalletTheme(int walletTheme);
        void verifyAndPrepareAndroidPayControls(@NonNull IsReadyToPayRequest payRequest);
    }

    public interface GoogleApiClientMockBuilder {
        GoogleApiClient getMockGoogleApiClient();
    }

    public interface AndroidPayAvailabilityChooser {
        BooleanResult doesAndroidPayCheckSucceed();
    }
}
