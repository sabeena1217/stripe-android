package com.stripe.example.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWallet;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.net.TokenParser;
import com.stripe.example.R;
import com.stripe.example.controller.ListViewController;
import com.stripe.example.controller.ProgressDialogController;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.activity.StripeAppCompatActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

import org.json.JSONException;

public class CheckoutWithAndroidPayActivity extends StripeAppCompatActivity {

    CartManager mCartManager;
    ViewGroup mFragmentContainer;
    private ListViewController mListViewController;
    private ProgressDialogController mProgressDialogController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_android_pay);
        mFragmentContainer = (ViewGroup) findViewById(R.id.android_pay_fragment_container);
        ListView tokenListView = (ListView) findViewById(R.id.android_pay_list_view);
        mListViewController = new ListViewController(tokenListView);
        mProgressDialogController = new ProgressDialogController(getSupportFragmentManager());
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

    @Override
    protected void onMaskedWalletRetrieved(@Nullable MaskedWallet maskedWallet) {
        mCartManager = new CartManager(mCart);
        if ("US".equals(maskedWallet.getBuyerShippingAddress().getCountryCode())
                && "CA".equals(maskedWallet.getBuyerShippingAddress().getAdministrativeArea())) {
            mCartManager.addLineItem("Shipping", 100L);
            mCartManager.setTaxLineItem("CA Tax", 50L);
        }

        try {
            Cart cart = mCartManager.buildCart();
            if (cart == null) {
                return;
            }
            FullWalletRequest walletRequest =
                    AndroidPayConfiguration.generateFullWalletRequest(
                            maskedWallet.getGoogleTransactionId(),
                            cart);
            mProgressDialogController.startProgress();
            makeFullWalletRequest(walletRequest);
        } catch (CartContentException unexpected) {

        }
    }

    @Override
    protected void onTokenReturned(FullWallet wallet, Token token) {
        super.onTokenReturned(wallet, token);
        mProgressDialogController.finishProgress();
        mListViewController.addToList(token);
    }
}
