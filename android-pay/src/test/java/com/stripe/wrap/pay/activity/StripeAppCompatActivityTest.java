package com.stripe.wrap.pay.activity;

import android.content.Intent;

import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.BuildConfig;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.testharness.StripeAndroidPayTestActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link StripeAppCompatActivity}Note that we have to test against SDK 22
 * because of a <a href="https://github.com/robolectric/robolectric/issues/1932">known issue</a> in
 * Robolectric.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 22)
public class StripeAppCompatActivityTest {

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";

    @Mock StripeAndroidPayTestActivity.AndroidPayAvailabilityChooser mAndroidPayAvailabilityChooser;
    @Mock GoogleApiClient mGoogleApiClient;
    @Mock StripeAndroidPayTestActivity.GoogleApiClientMockBuilder mGoogleApiClientMockBuilder;
    @Mock StripeAndroidPayTestActivity.StripeAppCompatActivityListener mListener;
    ActivityController<StripeAndroidPayTestActivity> mActivityController;
    CartManager mCartManager;
    Cart mCart;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(mGoogleApiClientMockBuilder.getMockGoogleApiClient()).thenReturn(mGoogleApiClient);
        mCartManager = new CartManager();
        mCartManager.addLineItem("First item", 100L);
        mCartManager.addLineItem("Second item", 200L);

        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.getInstance();
        androidPayConfiguration.setPublicApiKey(FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
        when(mAndroidPayAvailabilityChooser.doesAndroidPayCheckSucceed()).thenReturn(
                new BooleanResult(new Status(CommonStatusCodes.SUCCESS), true));

        try {
            mCart = mCartManager.buildCart();
            Intent intent = new Intent(RuntimeEnvironment.application,
                    StripeAndroidPayTestActivity.class)
                    .putExtra(StripeAppCompatActivity.EXTRA_CART, mCart);
            mActivityController = Robolectric.buildActivity(StripeAndroidPayTestActivity.class, intent);
            mActivityController.get().setStripeAppCompatActivityListener(mListener);
            mActivityController.get().setGoogleApiClientMockBuilder(mGoogleApiClientMockBuilder);
            mActivityController.get()
                    .setAndroidPayAvailabilityChooser(mAndroidPayAvailabilityChooser);
        } catch (CartContentException unexpected) {
            fail("Error setting up tests");
        }
    }

    @Test
    public void onCreate_listenerHitsExpectedMethods() {
        mActivityController.create().start();
        verify(mListener).onBeforeAndroidPayAvailable();
        verify(mListener).getWalletEnvironment(WalletConstants.ENVIRONMENT_TEST);
        verify(mListener).getWalletTheme(WalletConstants.THEME_LIGHT);
    }

}
