package com.sdk.ad.loader;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.sdk.ad.MySdk;
import com.sdk.ad.MySdkImpl;
import com.sdk.ad.network.Api;

public class RewardedAdLoader {

    public static void loadAndShow(Activity activity, String type, String adUnitId, MySdk.PrimaryListener listener) {
        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(activity, adUnitId, request, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                // 用于标记本轮播放用户最终是否获得了奖励（默认为 false）
                final boolean[] userEarnedReward = {false};
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdShowedFullScreenContent() {
                        Api.reportAdEvent(activity, type, MySdkImpl.EVENT_SHOW, MySdkImpl.STRATEGY_GOOGLE);
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        Api.reportAdEvent(activity, type, MySdkImpl.EVENT_CLICK, MySdkImpl.STRATEGY_GOOGLE);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                        if (listener != null) {
                            listener.onAdClosed();
                        }
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        if (listener != null) {
                            if (userEarnedReward[0]) {
                                listener.onAdRewarded();
                            } else {
                                listener.onAdClosed();
                            }
                        }
                    }
                });

                rewardedAd.show(activity, new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        userEarnedReward[0] = true;
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                if (listener != null) {
                    listener.onAdClosed();
                }
            }
        });

    }
}
