package com.sdk.ad.loader;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.sdk.ad.MySdkImpl;
import com.sdk.ad.network.Api;

import static com.sdk.ad.MySdkImpl.LOG_TAG;

public class OpenAdLoader {

    public static void loadAndShow(Activity activity, String type, String adUnitId) {
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(activity, adUnitId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                Log.d(LOG_TAG, "App Open ad loaded. Showing now.");
                // 绑定全屏生命周期监听，确保只在发生时精准上报
                appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        Api.reportAdEvent(activity, type, MySdkImpl.EVENT_SHOW, MySdkImpl.STRATEGY_GOOGLE);
                    }

                    @Override
                    public void onAdClicked() {
                        Api.reportAdEvent(activity, type, MySdkImpl.EVENT_CLICK, MySdkImpl.STRATEGY_GOOGLE);
                    }
                });
                appOpenAd.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(LOG_TAG, "App Open ad loading failed: " + loadAdError.getMessage());
            }
        });
    }
}
