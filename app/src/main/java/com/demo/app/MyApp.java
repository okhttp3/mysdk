package com.demo.app;

import android.app.Application;

import com.sdk.ad.MySdk;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // 服务器域名，APP_ID
        MySdk.init(this, "https://ad.a11game-test.com", "test123456789aab");

    }
}
