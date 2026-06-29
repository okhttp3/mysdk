package com.sdk.ad.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;


public class NetworkManager {
    private static NetworkManager instance;
    private final OkHttpClient httpClient;
    private final ExecutorService singleThreadExecutor;

    private NetworkManager() {
        httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        // 全局共享单线程池，严格控制后台并发、避免高频分配和释放线程带来的内存抖动
        singleThreadExecutor = Executors.newSingleThreadExecutor();
    }

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public ExecutorService getExecutor() {
        return singleThreadExecutor;
    }

    /**
     * 核心高层请求器：携带强校验 ?app_id= 路径参数，发送加密报文
     */
    public void postEncryptedJson(String baseUrl, String appId, String encryptedPayload, Callback callback) {
        try {
            // 不管是 GET 还是 POST，Url 路径上强制追加 ?app_id=xxx 参数
            String finalUrl = baseUrl + "?app_id=" + appId;

            RequestBody body = RequestBody.create(encryptedPayload, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(finalUrl)
                    .post(body)
                    .build();
            httpClient.newCall(request).enqueue(callback);
        } catch (Exception e) {
//            Log.e(LOG_TAG, "Failed to execute encrypted post for url: " + baseUrl, e);
        }
    }

    public boolean isNetworkConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}