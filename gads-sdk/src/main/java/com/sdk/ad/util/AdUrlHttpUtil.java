package com.sdk.ad.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

public class AdUrlHttpUtil {

    /**
     * 安全跳转外部浏览器打开广告链接
     *
     * @param context 建议传当前 Activity 的 Context
     * @param url     完整的跳转链接 (必须以 http:// 或 https:// 开头)
     */
    public static void openBrowser(Context context, String url) {
        if (context == null || url == null || TextUtils.isEmpty(url)) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // 规范化 URL 格式，确保带有协议头，防止拼写错误引发解析异常
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }
}