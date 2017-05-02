package com.stripe.wrap.pay.activity;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.utils.PaymentUtils;

public abstract class StripeAppCompatActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public static final String EXTRA_ACCOUNT_NAME = "extra_account_name";
    public static final String EXTRA_CART = "extra_cart";

    // Request code to use when creating the Masked Wallet.
    public static final int REQUEST_CODE_MASKED_WALLET = 2002;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    protected String mAccountName;
    protected AndroidPayConfiguration mAndroidPayConfiguration;
    protected Cart mCart;
    protected GoogleApiClient mGoogleApiClient;
    protected String mGoogleTransactionId;
    protected SupportWalletFragment mSupportWalletFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        if (getIntent().hasExtra(EXTRA_CART)) {
            mCart = getIntent().getParcelableExtra(EXTRA_CART);
        }

        if (getIntent().hasExtra(EXTRA_ACCOUNT_NAME)) {
            mAccountName = getIntent().getStringExtra(EXTRA_ACCOUNT_NAME);
        }

        mAndroidPayConfiguration = AndroidPayConfiguration.getInstance();
        mGoogleApiClient = buildGoogleApiClient();

        onBeforeAndroidPayAvailable();
        verifyAndPrepareAndroidPayControls(mGoogleApiClient,
                PaymentUtils.getStripeIsReadyToPayRequest());
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
        // retrieve the error code, if available
        int errorCode = -1;
        if (data != null) {
            errorCode = data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1);
        }
        switch (requestCode) {
            case REQUEST_RESOLVE_ERROR:
                mResolvingError = false;
                if (resultCode != RESULT_OK) {
                    return;
                }
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
                break;
            case REQUEST_CODE_MASKED_WALLET:
                switch (resultCode) {
                    case RESULT_OK:
                        if (data != null) {
                            MaskedWallet maskedWallet =
                                    data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);

                            // call to get the Google transaction id
                            mGoogleTransactionId = maskedWallet.getGoogleTransactionId();
                            // TODO: Make the full wallet request.
                            // Using the masked wallet, we can now update the cart with accurate
                            // shipping payment information and make a full wallet request.
                        }
                        break;
                    case RESULT_CANCELED:
                        break;
                    default:
                        handleError(errorCode);
                        break;
                }
                break;

            case WalletConstants.RESULT_ERROR:
                handleError(errorCode);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    protected void verifyAndPrepareAndroidPayControls(
            @NonNull GoogleApiClient googleApiClient,
            @NonNull IsReadyToPayRequest isReadyToPayRequest) {
        Wallet.Payments.isReadyToPay(googleApiClient, isReadyToPayRequest)
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

    /**
     * Builds the {@link GoogleApiClient} used in this Activity. Override
     * if you'd like to change the default GoogleApiClient.
     *
     * @return a {@link GoogleApiClient} used to interact with the Wallet API
     */
    @NonNull
    protected GoogleApiClient buildGoogleApiClient() {
        return new GoogleApiClient.Builder(this)
                .addApi(Wallet.API, new Wallet.WalletOptions.Builder()
                        .setEnvironment(getWalletEnvironment())
                        .setTheme(getWalletTheme())
                        .build())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    /**
     * Creates the {@link WalletFragmentStyle} for this Activity. Override to change
     * the appearance of the Wallet Fragment itself. The results of this method
     * are used to build the {@link WalletFragmentOptions}.
     *
     * @return a {@link WalletFragmentStyle} used to display Android Pay options to the user
     */
    @NonNull
    protected WalletFragmentStyle getWalletFragmentStyle() {
        return new WalletFragmentStyle()
                .setBuyButtonText(WalletFragmentStyle.BuyButtonText.BUY_WITH)
                .setBuyButtonAppearance(WalletFragmentStyle.BuyButtonAppearance.ANDROID_PAY_DARK)
                .setBuyButtonWidth(WalletFragmentStyle.Dimension.MATCH_PARENT);
    }

    @NonNull
    protected WalletFragmentOptions getWalletFragmentOptions() {
        return WalletFragmentOptions.newBuilder()
                .setEnvironment(getWalletEnvironment())
                .setFragmentStyle(getWalletFragmentStyle())
                .setTheme(getWalletTheme())
                .setMode(WalletFragmentMode.BUY_BUTTON)
                .build();
    }

    protected void createAndAddWalletFragment() {
        mSupportWalletFragment = SupportWalletFragment.newInstance(getWalletFragmentOptions());

        MaskedWalletRequest maskedWalletRequest =
                mAndroidPayConfiguration.generateMaskedWalletRequest(mCart);
        WalletFragmentInitParams.Builder startParamsBuilder =
                WalletFragmentInitParams.newBuilder()
                        .setMaskedWalletRequest(maskedWalletRequest)
                        .setMaskedWalletRequestCode(REQUEST_CODE_MASKED_WALLET);

        if (!TextUtils.isEmpty(mAccountName)) {
            startParamsBuilder.setAccountName(mAccountName);
        }

        mSupportWalletFragment.initialize(startParamsBuilder.build());
        addWalletFragment(mSupportWalletFragment);
    }

    protected void handleError(int errorCode) {

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

    /**
     * Handles the error conditions for connection issues with the {@link GoogleApiClient}.
     * Deliberately mimics the behavior of enableAutoManage.
     *
     * @param connectionResult a {@link ConnectionResult} failure in the {@link GoogleApiClient}
     */
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

    protected abstract void addWalletFragment(@NonNull SupportWalletFragment walletFragment);
    protected abstract void onAndroidPayAvailable();
    protected abstract void onAndroidPayNotAvailable();

    /*------ Optional Overrides ------*/

    protected void onBeforeAndroidPayAvailable() {
        // This is a good place to display a spinner if you anticipate network delays
        // initializing the Google API client.
    }

    protected void onAfterAndroidPayCheckComplete() {
        // If a spinner was showing, remove it in this method.
    }

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
