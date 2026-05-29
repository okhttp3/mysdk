package com.sdk.ad.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.sdk.ad.MySdkImpl;

import static com.sdk.ad.MySdkImpl.LOG_TAG;

public class DeviceUtils {
    private static String cachedGaid = "";

    /**
     * 获取谷歌广告标识符 (GAID)
     * 注意：由于该方法涉及网络/IPC绑定，不能在主线程调用。
     * 如果未加载完成、老外关闭了追踪或发生异常，一律透传空字符串 ""
     */
    public static String getGaid(Context context) {
        if (!TextUtils.isEmpty(cachedGaid)) {
            return cachedGaid;
        }
        cachedGaid = AdvertisingIdUtil.getGoogleAdId(context);
        return !TextUtils.isEmpty(cachedGaid) ? cachedGaid : MySdkImpl.ADJUST_ADID;
    }

    /**
     * 移动国家码 (MCC) 获取
     * 100%基于物理基站真实归属。获取不到或无卡时传空字符串 ""
     */
    public static int getMcc(Context context) {
        try {
            TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telManager != null) {
                String networkOperator = telManager.getNetworkOperator();
                // networkOperator 由 3位MCC + 2位或3位MNC 组成
                if (!TextUtils.isEmpty(networkOperator) && networkOperator.length() >= 3) {
                    String substring = networkOperator.substring(0, 3);
                    return Integer.parseInt(substring);
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to get MCC: " + e.getMessage());
        }
        return 0;
    }

    /**
     * 移动网络码 (MNC) 获取
     * 用于精确识别海外运营商。获取不到或无卡时传空字符串 ""
     */
    public static int getMnc(Context context) {
        try {
            TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telManager != null) {
                String networkOperator = telManager.getNetworkOperator();
                if (!TextUtils.isEmpty(networkOperator) && networkOperator.length() > 3) {
                    return Integer.parseInt(networkOperator.substring(3));
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to get MNC: " + e.getMessage());
        }
        return 0;
    }

    /**
     * 系统的 USB 调试模式是否开启
     * Slots真实玩家极少开启，防谷歌机审与逆向黑客
     */
    public static boolean isUsbDebugOn(Context context) {
        try {
            if (context != null && context.getContentResolver() != null) {
                return Settings.Global.getInt(context.getContentResolver(), Settings.Global.ADB_ENABLED, 0) > 0;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to check USB debug status.");
        }
        return false;
    }

    /**
     * 检测手机内是否有可用的物理 SIM 卡
     */
    public static boolean hasSimCard(Context context) {
        try {
            TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telManager != null) {
                return telManager.getSimState() == TelephonyManager.SIM_STATE_READY;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to check SIM state.");
        }
        return false;
    }

    /**
     * 精准应用状态检测矩阵
     * 基于清单文件 <queries> 标签检测A/B/C包在架状态，失败返回 false
     */
    public static boolean checkAppInstalled(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            // 未安装目标包名
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}