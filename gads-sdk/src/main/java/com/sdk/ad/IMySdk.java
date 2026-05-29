package com.sdk.ad;

import android.app.Activity;
import android.content.Context;


public interface IMySdk {
    void init(Context context, String apiSdk, String appId);

    void showAd(Activity activity, String type, String adUnitId, MySdk.PrimaryListener listener);
}
