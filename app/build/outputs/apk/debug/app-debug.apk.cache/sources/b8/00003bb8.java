package com.sdk.ad;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

/* compiled from: r8-map-id-36867c58d7dafba1b8bf21bd6f0d43755c2b5fc35117f0ce7dc2bb1c4c3fa089 */
/* loaded from: classes2.dex */
public class MySdk {
    private static final MySdkImpl IMPL = new MySdkImpl();

    /* compiled from: r8-map-id-36867c58d7dafba1b8bf21bd6f0d43755c2b5fc35117f0ce7dc2bb1c4c3fa089 */
    /* loaded from: classes2.dex */
    public interface AdType {
        public static final String OPEN = a.a.a("fd54+A==\n", "Eq4dltQyxjc=\n");
        public static final String BANNER = a.a.a("5LGdf6we\n", "htDzEclsP3s=\n");
        public static final String REWARDED = a.a.a("2+o3Y9c29Y0=\n", "qY9AAqVSkOk=\n");
    }

    /* compiled from: r8-map-id-36867c58d7dafba1b8bf21bd6f0d43755c2b5fc35117f0ce7dc2bb1c4c3fa089 */
    /* loaded from: classes2.dex */
    public interface PrimaryListener {
        void onAdClosed();

        void onAdRewarded();
    }

    public static void init(Context context, String str, String str2) {
        IMPL.a(context, str, str2);
    }

    public static void showAd(Activity activity, String str, String str2, PrimaryListener primaryListener) {
        if (activity == null || activity.isFinishing() || TextUtils.isEmpty(str)) {
            return;
        }
        if (str.equals(a.a.a("0gyecw==\n", "vXz7HVboo3M=\n")) || str.equals(a.a.a("NkIETjz0\n", "VCNqIFmGJOU=\n")) || str.equals(a.a.a("1kpyCVnfMtY=\n", "pC8FaCu7V7I=\n"))) {
            IMPL.a(activity, str, str2, primaryListener);
        }
    }
}