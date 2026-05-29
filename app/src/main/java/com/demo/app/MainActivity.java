package com.demo.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.a11.ad.test.a.R;
import com.sdk.ad.MySdk;

public class MainActivity extends Activity {

//    private static final String OPEN_AD_UNIT_ID = "ca-app-pub-4514574929692907/7015699592";
//    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-4514574929692907/7015699592";
//    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-4514574929692907/7015699592";

    // 谷歌测试广告位id
    private static final String OPEN_AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 开屏广告
        MySdk.showAd(MainActivity.this, MySdk.AdType.OPEN, OPEN_AD_UNIT_ID, null);

        // 开屏广告
        findViewById(R.id.btn_show_open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MySdk.showAd(MainActivity.this, MySdk.AdType.OPEN, OPEN_AD_UNIT_ID, null);
            }
        });

        // 底部广告
        findViewById(R.id.btn_show_banner).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MySdk.showAd(MainActivity.this, MySdk.AdType.BANNER, BANNER_AD_UNIT_ID, null);
            }
        });

        // 视频激励广告
        findViewById(R.id.btn_show_rewarded).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MySdk.showAd(MainActivity.this, MySdk.AdType.REWARDED, REWARDED_AD_UNIT_ID, new MySdk.PrimaryListener() {
                    @Override
                    public void onAdRewarded() {
                        Log.e("wtj", "关闭且发放奖励");
                    }

                    @Override
                    public void onAdClosed() {
                        Log.e("wtj", "直接关闭");
                    }
                });
            }
        });

    }
}
