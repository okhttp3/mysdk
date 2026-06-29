package com.sdk.ad.util;

import android.content.Context;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.sdk.ad.MySdkImpl;


public class AdvertisingIdUtil {

    /**
     * 使用官方 Play Services 客户端获取 GAID（必须在子线程调用）
     */
    public static String getGoogleAdId(Context context) {
        try {
            // 官方一句话搞定，内部自动完成跨进程绑定与数据读取
            AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
            // 如果用户在系统设置里关闭了广告追踪（Limit Ad Tracking），可以视情况选择是否过滤
            // boolean isLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled();

            String gaid = adInfo.getId();
            return gaid != null ? gaid : "";
        } catch (Exception e) {
            // 内部会自动捕获：GooglePlayServicesNotAvailableException (无谷歌服务)
            // 和 GooglePlayServicesRepairableException (谷歌服务需要修复/升级)
        }
        return "";
    }
}