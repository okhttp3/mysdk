package com.sdk.ad.loader;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.sdk.ad.MySdkImpl;
import com.sdk.ad.custom.PrimaryManager;
import com.sdk.ad.network.Api;


public class BannerAdLoader {
    // 统一使用全局唯一的 Tag，方便在新页面或切流时清理旧广告，防止叠加错乱
    private static final String TAG_BANNER_CONTAINER = "google_banner_container";

    public static void loadAndShow(Activity activity, String type, String adUnitId) {
        if (activity == null || activity.isFinishing()) return;
        View decorView = activity.getWindow().getDecorView();
        if (decorView instanceof ViewGroup) {
            AdRequest request = new AdRequest.Builder().build();

            ViewGroup rootDecor = (ViewGroup) decorView;

            // 确保切换或重载时，把之前可能存在的旧横幅清干净
            removeExistingBanner(rootDecor);

            // 1. 创建一个底置的外壳容器，用于包裹官方 AdView
            final FrameLayout container = new FrameLayout(activity);
            container.setTag(TAG_BANNER_CONTAINER);

            FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            // 核心控制：紧贴屏幕最底部，并水平居中
            containerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            container.setLayoutParams(containerParams);

            // 2. 初始化谷歌官方的 AdView
            final AdView adView = new AdView(activity);
            adView.setAdUnitId(adUnitId);
            adView.setAdSize(AdSize.BANNER); // 标准 50dp 高度

            // 3. 配置谷歌广告生命周期监听
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
//                    Log.d(LOG_TAG, "Google Banner ad loaded successfully.");
                    // 加载成功后，才把外壳容器塞进 DecorView，实现平滑无感展现
                    if (container.getParent() == null) {
                        rootDecor.addView(container);
                        Api.reportAdEvent(activity, type, 0, MySdkImpl.EVENT_SHOW, MySdkImpl.STRATEGY_GOOGLE);
                    }
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
//                    Log.e(LOG_TAG, "Google Banner failed to load: " + loadAdError.getMessage() + ". Rolling back to custom banner.");
                    removeExistingBanner(rootDecor);
                }

                @Override
                public void onAdClicked() {
                    super.onAdClicked();
                    Api.reportAdEvent(activity, type, 0, MySdkImpl.EVENT_CLICK, MySdkImpl.STRATEGY_GOOGLE);
                }
            });

            // 4. 把官方 AdView 挂载到容器中，并触发异步加载
            FrameLayout.LayoutParams adViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            adViewParams.gravity = Gravity.CENTER; // 让广告内容在外壳里水平居中
            container.addView(adView, adViewParams);

            // 开始请求网络
            adView.loadAd(request);
        }

    }

    /**
     * 清理现有的横幅广告（包括官方的容器和自定义的容器）
     */
    private static void removeExistingBanner(ViewGroup rootDecor) {
        if (rootDecor == null) return;
        int childCount = rootDecor.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = rootDecor.getChildAt(i);
            Object tag = child.getTag();
            if (tag != null) {
                if (tag.equals(TAG_BANNER_CONTAINER) || tag.equals(PrimaryManager.TAG_INTERNAL_LAYOUT_ROOT)) {
                    rootDecor.removeView(child);
                }
            }
        }
    }
}