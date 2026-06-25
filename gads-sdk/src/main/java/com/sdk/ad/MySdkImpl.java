package com.sdk.ad;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.sdk.ad.custom.PrimaryManager;
import com.sdk.ad.loader.BannerAdLoader;
import com.sdk.ad.loader.OpenAdLoader;
import com.sdk.ad.loader.RewardedAdLoader;
import com.sdk.ad.network.Api;
import com.sdk.ad.network.NetworkManager;
import com.sdk.ad.util.StorageManager;

import org.json.JSONObject;

public class MySdkImpl implements IMySdk {
    public static final String LOG_TAG = "wtj";
    public static final String VERSION = "1.0";

    // ==================== 核心公共常量 ====================
    public static final String STRATEGY_GOOGLE = "google";
    public static final String STRATEGY_CUSTOM = "custom";
    public static final String EVENT_SHOW = "show";
    public static final String EVENT_CLICK = "click";

    public static String APP_ID;
    public static String API_SDK;
    public static String ADJUST_ADID;

    // SDK 核心生命周期状态机
    private enum InitState {IDLE, INITIALIZING, SUCCESS, FALLBACK_CACHE, FAILED}

    private InitState mInitState = InitState.IDLE;

    private int mRetryCount = 0;
    private static final int MAX_RETRY_COUNT = 5;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void init(final Context context, String apiSdk, String appId) {
        APP_ID = appId;
        API_SDK = apiSdk;
        mRetryCount = 0;
        startInitFlow(context.getApplicationContext());
    }

    /**
     * 1. 核心调度：异步网络配置拉取
     */
    private void startInitFlow(final Context appContext) {
        mInitState = InitState.INITIALIZING;
        Log.d(LOG_TAG, "SDK 初始化网络请求启动...");

        Api.fetchRemoteConfig(appContext, new Api.ConfigCallback() {
            @Override
            public void onSuccess() {
                mMainHandler.post(() -> {
                    Log.i(LOG_TAG, "远端策略云配置同步成功.");
                    mInitState = InitState.SUCCESS;
                    mRetryCount = 0;
                    ConfigManager.preloadVideoAdIfEnabled(appContext);
                });
            }

            @Override
            public void onFailure(final Exception e) {
                mMainHandler.post(() -> {
                    Log.w(LOG_TAG, "远端策略云配置同步失败: " + e.getMessage());
                    handleInitFailure(appContext);
                });
            }
        });
    }

    /**
     * 2. 核心调度：断网自愈与重试
     */
    private void handleInitFailure(final Context appContext) {
        if (ConfigManager.hasLocalCache(appContext)) {
            Log.i(LOG_TAG, "检测到缓存待上报数据");
            mInitState = InitState.FALLBACK_CACHE;
            ConfigManager.preloadVideoAdIfEnabled(appContext);
            return;
        }
        Log.i(LOG_TAG, "本地无缓存待上报数据");
        if (mRetryCount < MAX_RETRY_COUNT && NetworkManager.getInstance().isNetworkConnected(appContext)) {
            mRetryCount++;
            Log.w(LOG_TAG, "无缓存。满足重试条件，将在 3s 后进行第 " + mRetryCount + " 次重试...");
            mMainHandler.postDelayed(() -> startInitFlow(appContext), 3000);
        } else {
            Log.e(LOG_TAG, "未满足重试条件或已达上限，初始化流终结.");
            mInitState = InitState.FAILED;
        }
    }

    /**
     * 3. 核心接口：游戏触发物理展示入口（无任何 Activity 挂起缓存，纯动态判定）
     */
    @Override
    public void showAd(Activity activity, String type, String adUnitId, MySdk.PrimaryListener listener) {
        if (activity == null || activity.isFinishing() || TextUtils.isEmpty(type)) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "游戏触发 showAd -> 类型: " + type + " | SDK状态: " + mInitState);

                // 实时获取配置实体
                AdUnitConfig config = ConfigManager.getAdUnitConfig(activity, type);

                // 交给专职的路由分发器进行策略渲染与事件上报
                AdRouter.dispatch(activity, type, adUnitId, listener, config);
            }
        });

    }

    // =========================================================================
    // 封装层一：配置管理领域模型 (ConfigManager)
    // =========================================================================
    private static class ConfigManager {
        static boolean hasLocalCache(Context context) {
            return !TextUtils.isEmpty(StorageManager.getAdData(context));
        }

        static AdUnitConfig getAdUnitConfig(Context context, String type) {
            AdUnitConfig config = new AdUnitConfig();
            try {
                String json = StorageManager.getAdData(context);
                if (!TextUtils.isEmpty(json)) {
                    JSONObject targetNode = new JSONObject(json).optJSONObject(type);
                    if (targetNode != null) {
                        config.status = targetNode.optBoolean("status", false);
                        config.sourceUrl = targetNode.optString("source_url", "").trim();
                        config.jumpUrl = targetNode.optString("jump_url", "").trim();
                        config.adId = targetNode.optInt("ad_id", 0);
                    }
                }
            } catch (Exception ignored) {
            }
            return config;
        }

        static void preloadVideoAdIfEnabled(Context context) {
            AdUnitConfig config = getAdUnitConfig(context, MySdk.AdType.REWARDED);
            if (config.status && !TextUtils.isEmpty(config.sourceUrl)) {
                Log.d(LOG_TAG, "预加载视频激励广告: " + config.sourceUrl);
                PrimaryManager.preloadVideoAd(context, config.sourceUrl);
            }
        }
    }

    // =========================================================================
    // 封装层二：策略路由分发器 (AdRouter)
    // =========================================================================
    private static class AdRouter {
        static void dispatch(Activity activity, String type, String adUnitId, MySdk.PrimaryListener listener, AdUnitConfig config) {
            // 完全遵循原文件中的条件：isValidCustomAd() 成功走自定义，否则走谷歌
            if (config != null && config.isValidCustomAd()) {
                Log.i(LOG_TAG, ">> [策略路由] 分发至 -> 自定义广告通路 <<");
                executeCustomAd(activity, type, config, listener);
            } else {
                Log.i(LOG_TAG, ">> [策略路由] 分发至 -> 官方谷歌 AdMob 通路 <<");
                executeGoogleAd(activity, type, adUnitId, listener);
            }
        }

        /**
         * 修正：完美还原原文件 renderCustomAd 的核心逻辑
         */
        private static void executeCustomAd(Activity activity, String type, AdUnitConfig config, MySdk.PrimaryListener listener) {
            // 补回数据埋点上报
            Api.reportAdEvent(activity, type, config.adId, EVENT_SHOW, STRATEGY_CUSTOM);

            // 精准映射回原文件的物理展示视图方法
            switch (type) {
                case MySdk.AdType.OPEN:
                    PrimaryManager.initPrimaryView(activity, type, config, listener);
                    break;
                case MySdk.AdType.BANNER:
                    PrimaryManager.initPrimaryView2(activity, type, config);
                    break;
                case MySdk.AdType.REWARDED:
                    PrimaryManager.initPrimaryView3(activity, type, config, listener);
                    break;
            }
        }

        /**
         * 修正：完美还原原文件 renderGoogleAd 的核心逻辑
         */
        private static void executeGoogleAd(Activity activity, String type, String adUnitId, MySdk.PrimaryListener listener) {
            // 补回数据埋点上报
            Api.reportAdEvent(activity, type, 0, EVENT_SHOW, STRATEGY_GOOGLE);

            switch (type) {
                case MySdk.AdType.OPEN:
                    OpenAdLoader.loadAndShow(activity, type, adUnitId, listener);
                    break;
                case MySdk.AdType.BANNER:
                    BannerAdLoader.loadAndShow(activity, type, adUnitId);
                    break;
                case MySdk.AdType.REWARDED:
                    RewardedAdLoader.loadAndShow(activity, type, adUnitId, listener);
                    break;
            }
        }
    }

    // =========================================================================
    // 封装层三：统一策略配置实体 (AdUnitConfig)
    // =========================================================================
    public static class AdUnitConfig {
        public boolean status = false;
        public String sourceUrl = "";
        public String jumpUrl = "";
        public int adId = 0;

        public boolean isValidCustomAd() {
            return status && !TextUtils.isEmpty(sourceUrl);
        }
    }
}