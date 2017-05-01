package com.stripe.wrap.pay.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.utils.PaymentUtils;

public abstract class StripeAppCompatActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    protected AndroidPayConfiguration mAndroidPayConfiguration;
    protected GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        mAndroidPayConfiguration = AndroidPayConfiguration.getInstance();
        mAndroidPayConfiguration.setPublicApiKey(getStripePublishableKey());
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(getWalletEnvironment())
                        .setTheme(getWalletTheme())
                        .build())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        onBeforeAndroidPayAvailable();
        Wallet.Payments.isReadyToPay(mGoogleApiClient, PaymentUtils.getStripeIsReadyToPayRequest())
                .setResultCallback(
                    new ResultCallback<BooleanResult>() {
                        @Override
                        public void onResult(@NonNull BooleanResult booleanResult) {
                            onAfterAndroidPayCheckComplete();

                            if (booleanResult.getStatus().isSuccess() && booleanResult.getValue()) {
                                createAndAddWalletFragment();
                            } else {
                                onAndroidPayNotAvailable();
                            }
                        }
                    });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode != RESULT_OK) {
                return;
            }
            // Make sure the app is not already connected or attempting to connect
            if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    /*------ Begin GoogleApiClient.ConnectionCallbacks ------*/

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        onAndroidPayAvailable();
    }

    @Override
    public void onConnectionSuspended(int i) {
        onAndroidPayNotAvailable();
    }

    /*------ End GoogleApiClient.ConnectionCallbacks ------*/

    /*------ Begin GoogleApiClient.OnConnectionFailedListener ------*/

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    /*------ End GoogleApiClient.OnConnectionFailedListener ------*/

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /*------ Required Overrides ------*/

    protected abstract void createAndAddWalletFragment();
    protected abstract String getStripePublishableKey();
    protected abstract void onAndroidPayAvailable();
    protected abstract void onAndroidPayNotAvailable();

    protected void onBeforeAndroidPayAvailable() { }
    protected void onAfterAndroidPayCheckComplete() { }

    /*------ Optional Overrides ------*/

    protected int getWalletEnvironment() {
        return WalletConstants.ENVIRONMENT_TEST;
    }

    protected int getWalletTheme() {
        return WalletConstants.THEME_LIGHT;
    }

    /*------ End Overrides ------*/

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((StripeAppCompatActivity) getActivity()).onDialogDismissed();
        }
    }
}
