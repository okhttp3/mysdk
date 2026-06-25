package com.sdk.ad.loader;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.sdk.ad.MySdk;
import com.sdk.ad.MySdkImpl;
import com.sdk.ad.network.Api;

import static com.sdk.ad.MySdkImpl.LOG_TAG;

public class OpenAdLoader {
    public static void loadAndShow(Activity activity, String type, String adUnitId, MySdk.PrimaryListener listener) {
        // 1. 在开始加载前，向 Activity 顶层注入一个纯白色的遮罩层
        FrameLayout rootView = activity.findViewById(android.R.id.content);
        View whiteLoadingView = new View(activity);
        whiteLoadingView.setBackgroundColor(Color.WHITE);
        // 拦截点击事件，防止在加载期间用户误触底部的游戏UI
        whiteLoadingView.setClickable(true);
        rootView.addView(whiteLoadingView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        AdRequest request = new AdRequest.Builder().build();

        AppOpenAd.load(activity, adUnitId, request, new AppOpenAd.AppOpenAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull AppOpenAd appOpenAd) {
                Log.d(LOG_TAG, "App Open ad loaded. Showing now.");

                appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        // 2. 广告真正全屏展示出来时，移除白屏过渡页（此时移除最平滑，无缝衔接广告页面）
                        rootView.removeView(whiteLoadingView);
                        Api.reportAdEvent(activity, type, 0, MySdkImpl.EVENT_SHOW, MySdkImpl.STRATEGY_GOOGLE);
                    }

                    @Override
                    public void onAdClicked() {
                        Api.reportAdEvent(activity, type, 0, MySdkImpl.EVENT_CLICK, MySdkImpl.STRATEGY_GOOGLE);
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d(LOG_TAG, "App Open ad dismissed.");
                        if (listener != null) {
                            listener.onAdClosed();
                        }
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        Log.e(LOG_TAG, "App Open ad failed to show: " + adError.getMessage());
                        // 3. 广告展示失败时，移除白屏并恢复游戏
                        rootView.removeView(whiteLoadingView);
                        if (listener != null) {
                            listener.onAdClosed();
                        }
                    }
                });

                appOpenAd.show(activity);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(LOG_TAG, "App Open ad loading failed: " + loadAdError.getMessage());
                // 4. 广告加载失败（如网络错误）时，移除白屏并恢复游戏
                rootView.removeView(whiteLoadingView);
                if (listener != null) {
                    listener.onAdClosed();
                }
            }
        });
    }
}
