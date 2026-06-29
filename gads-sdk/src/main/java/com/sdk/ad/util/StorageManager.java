package com.sdk.ad.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;


public class StorageManager {
    private static final String SP_NAME = "secure_ad_payload";
    private static final String KEY_UDID = "enc_udid";
    private static final String KEY_AD_DATA = "enc_ad_data";

    /**
     * 加密保存下发的下发配置
     */
    public static void saveConfig(Context context, String openUdid, String adData) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        // 核心参数一律做加密隔离
        editor.putString(KEY_UDID, CryptoUtils.encrypt(openUdid));
        editor.putString(KEY_AD_DATA, CryptoUtils.encrypt(adData));
        editor.apply();
    }

    /**
     * 获取解密后的 open_udid
     */
    public static String getOpenUdid(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String openUdid = sp.getString(KEY_UDID, "");
        return TextUtils.isEmpty(openUdid) ? "" : CryptoUtils.decrypt(openUdid);
    }

    /**
     * 获取解密后的 广告物料
     */
    public static String getAdData(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        String adData = sp.getString(KEY_AD_DATA, "");
        return TextUtils.isEmpty(adData) ? "" : CryptoUtils.decrypt(adData);
    }
}