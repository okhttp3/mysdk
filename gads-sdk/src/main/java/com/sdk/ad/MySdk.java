package com.sdk.ad;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

public class MySdk {
    private final static MySdkImpl IMPL = new MySdkImpl();

    public interface PrimaryListener {
        void onAdRewarded();

        void onAdClosed();
    }

    public interface AdType {
        String OPEN = "open";
        String BANNER = "banner";
        String REWARDED = "rewarded";
    }


    public static void init(Context context, String apiSdk, String appId) {
        IMPL.init(context, apiSdk, appId);
    }

    /**
     * 显示广告
     *
     * @param type     {@link MySdk.AdType}
     * @param listener {@link MySdk.PrimaryListener} 视频激励广告回调，其他类型传null
     */
    public static void showAd(Activity activity, String type, String adUnitId, MySdk.PrimaryListener listener) {
        if (activity == null || activity.isFinishing()) return;
        if (TextUtils.isEmpty(type)) {
            return;
        }
        if (!type.equals(AdType.OPEN) && !type.equals(AdType.BANNER) && !type.equals(AdType.REWARDED)) {
            return;
        }
        IMPL.showAd(activity, type, adUnitId, listener);
    }
}
