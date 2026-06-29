package com.sdk.ad.network;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sdk.ad.MySdkImpl;
import com.sdk.ad.util.DeviceUtils;
import com.sdk.ad.util.StorageManager;
import com.sdk.ad.util.XXTeaUtil;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class Api {
    // 1. 新增一个内部通知接口
    public interface ConfigCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    /**
     * 接口一：初始化拉取云端策略配置
     */
    public static void fetchRemoteConfig(Context context, final ConfigCallback callback) {
        // 1. 初始化成功后，首先立刻检测并激活本地可能残留的离线代上报事件队列进行自愈消费
        OfflineReportQueue.getInstance().checkAndStartQueue(context);

        DeviceProfileCollector.collectAsync(context, rootProfile -> {
            try {
                String plainJson = rootProfile.toString();
                String encryptedData = XXTeaUtil.encryptToBase64String(plainJson, MySdkImpl.APP_ID);
                String targetUrl = MySdkImpl.API_SDK + "/v1/device/init";

                NetworkManager.getInstance().postEncryptedJson(targetUrl, MySdkImpl.APP_ID, encryptedData, new Callback() {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        try (Response resp = response) {
                            if (!resp.isSuccessful()) {
                                return;
                            }
                            String encryptedResponseBody = resp.body().string().trim();

                            JSONObject responseJson = new JSONObject(encryptedResponseBody);
                            if (responseJson.optInt("code", -1) == 200) {
                                String data = responseJson.optString("data");
                                if (!TextUtils.isEmpty(data)) {
                                    String decryptedJsonStr = XXTeaUtil.decryptFromBase64String(data, MySdkImpl.APP_ID);

                                    JSONObject dataNode = new JSONObject(decryptedJsonStr);
                                    String openUdid = dataNode.optString("open_udid", "");

                                    JSONObject adDelivery = dataNode.optJSONObject("ad_delivery");

                                    if (!TextUtils.isEmpty(openUdid)) {
                                        // 缓存策略数据
                                        String rewardedData = "";
                                        if (adDelivery != null) {
                                            rewardedData = adDelivery.toString();
                                        }
                                        StorageManager.saveConfig(context, openUdid, rewardedData);
                                        if (callback != null) {
                                            callback.onSuccess();
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            if (callback != null) {
                                callback.onFailure(ex);
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        if (callback != null) {
                            callback.onFailure(e);
                        }
                    }
                });
            } catch (Exception e) {
//                Log.e(LOG_TAG, "Encrypt or envelope failed during initialization.", e);
            }
        });
    }

    /**
     * 接口二：全平台通用的展示/点击事件埋点数据上报
     */
    public static void reportAdEvent(Context context, String adType, int adId, String event, String strategy) {
        NetworkManager.getInstance().getExecutor().execute(() -> {
            try {
                JSONObject trackItem = new JSONObject();
                trackItem.put("app_id", MySdkImpl.APP_ID);
                trackItem.put("package_name", context.getPackageName());
                trackItem.put("open_udid", StorageManager.getOpenUdid(context));
                trackItem.put("gaid", DeviceUtils.getGaid(context));
                trackItem.put("ad_type", adType);
                trackItem.put("ad_id", adId);
                trackItem.put("event", event);
                trackItem.put("strategy", strategy);
                trackItem.put("timestamp", System.currentTimeMillis());

                OfflineReportQueue.getInstance().submitEvent(context, trackItem);
            } catch (Exception e) {
            }
        });
    }


}