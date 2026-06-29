package com.sdk.ad.network;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.sdk.ad.MySdkImpl;
import com.sdk.ad.util.StorageManager;
import com.sdk.ad.util.XXTeaUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class OfflineReportQueue {
    private static OfflineReportQueue instance;
    private static final Object LOCK = new Object();
    private static final String SP_NAME = "sdk_ad_offline_storage";
    private static final String KEY_QUEUE = "offline_event_tracks";

    private boolean isLooping = false;

    private OfflineReportQueue() {
    }

    public static synchronized OfflineReportQueue getInstance() {
        if (instance == null) {
            instance = new OfflineReportQueue();
        }
        return instance;
    }

    /**
     * 外部提交流：联网正常直接发，失败/弱网秒级落盘并唤醒定时器
     */
    public void submitEvent(Context context, JSONObject eventPayload) {
        NetworkManager net = NetworkManager.getInstance();
        if (!net.isNetworkConnected(context)) {
            saveToDisk(context, eventPayload);
            checkAndStartQueue(context);
            return;
        }

        executeHttpCall(context, eventPayload, false);
    }

    /**
     * 启动自愈轮询器：满足SDK初始化成功后或断网重连后主动触发
     */
    public void checkAndStartQueue(Context context) {
        synchronized (LOCK) {
            if (isLooping) return;
            int pendingCount = getLocalQueueLength(context);
            if (pendingCount > 0) {
                isLooping = true;
//                Log.d(LOG_TAG, "Offline storage items detected [" + pendingCount + "]. Activating 10s polling engine...");
                scheduleLoopSlice(context.getApplicationContext());
            }
        }
    }

    private void scheduleLoopSlice(Context context) {
        // 复用底层唯一的固定后台单线程池进行休眠，避免常驻后台带来的额外系统开销与泄漏
        NetworkManager.getInstance().getExecutor().execute(() -> {
            try {
                Thread.sleep(10000); // 严格挂起 10 秒不停
            } catch (InterruptedException ignored) {
            }

            synchronized (LOCK) {
                try {
                    android.content.SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                    JSONArray array = new JSONArray(sp.getString(KEY_QUEUE, "[]"));

                    if (array.length() == 0) {
                        isLooping = false;
                        return; // 数据队列消费完，彻底关闭该轮询器句柄
                    }

                    // 始终取出第一个元素尝试通过网络冲洗
                    JSONObject topItem = array.optJSONObject(0);
                    if (topItem != null && NetworkManager.getInstance().isNetworkConnected(context)) {
                        executeHttpCall(context, topItem, true);
                    }
                } catch (Exception e) {
                }

                // 只要状态未被注销，自适应开启下一个 10 秒递归分片
                if (isLooping) {
                    scheduleLoopSlice(context);
                }
            }
        });
    }

    private void executeHttpCall(Context context, JSONObject payload, boolean isFromLoop) {
        if (payload == null) {
            return;
        }
        try {
            // optString 在字段不存在、为 null 或为空时，默认会返回空字符串 ""
            String openUdid = payload.optString("open_udid", "").trim();
            if (TextUtils.isEmpty(openUdid)) {
                String cachedUdid = StorageManager.getOpenUdid(context);
                payload.put("open_udid", cachedUdid);
            }

            String encryptedData = XXTeaUtil.encryptToBase64String(payload.toString(), MySdkImpl.APP_ID);
            String targetUrl = MySdkImpl.API_SDK + "/v1/track/report";

            NetworkManager.getInstance().postEncryptedJson(targetUrl, MySdkImpl.APP_ID, encryptedData, new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    if (!isFromLoop) saveToDisk(context, payload);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    try (Response resp = response) {
                        if (resp.isSuccessful()) {
                            if (isFromLoop) {
                                removeQueueHead(context); // 确认发送成功，剔除本地队列头部元素
                            }
                        } else {
                            if (!isFromLoop) saveToDisk(context, payload);
                        }
                    }
                }
            });
        } catch (Exception e) {
            if (!isFromLoop) saveToDisk(context, payload);
        }
    }

    private void saveToDisk(Context context, JSONObject item) {
        synchronized (LOCK) {
            try {
                android.content.SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                JSONArray array = new JSONArray(sp.getString(KEY_QUEUE, "[]"));

                // 内存和磁盘空间控制优化：最大允许本地积压 200 条，超出则顶替最老旧的脏数据，防止容器越界
                if (array.length() >= 200) {
                    array.remove(0);
                }
                array.put(item);
                sp.edit().putString(KEY_QUEUE, array.toString()).apply();
            } catch (Exception ignored) {
            }
        }
    }

    private void removeQueueHead(Context context) {
        synchronized (LOCK) {
            try {
                android.content.SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
                JSONArray array = new JSONArray(sp.getString(KEY_QUEUE, "[]"));
                if (array.length() > 0) {
                    array.remove(0);
                    sp.edit().putString(KEY_QUEUE, array.toString()).apply();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private int getLocalQueueLength(Context context) {
        try {
            android.content.SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            return new JSONArray(sp.getString(KEY_QUEUE, "[]")).length();
        } catch (Exception e) {
            return 0;
        }
    }
}