package com.sdk.ad.network;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;

import com.sdk.ad.MySdkImpl;
import com.sdk.ad.util.DeviceUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.TimeZone;

public class DeviceProfileCollector {

    public interface CollectorCallback {
        void onCollected(JSONObject rootProfile);
    }

    private static final String[] TARGET_PACKAGES = {
            "com.amazon.amazonvideo.livingroom",
            "com.amazon.avod.thirdpartyclient",
            "com.amazon.mShop.android.shopping",
            "com.audible.application",
            "com.block.juggle",
            "com.crunchyroll.crunchyroid",
            "com.csam.icici.bank.imobile",
            "com.discord",
            "com.disney.disneyplus",
            "com.dreamplug.androidapp",
            "com.dropbox.android",
            "com.duolingo",
            "com.dywx.larkplayer",
            "com.einnovation.temu",
            "com.facebook.katana",
            "com.firsttouchgames.dls7",
            "com.freecharge.android",
            "com.google.android.apps.nbu.paisa.user",
            "com.grindrapp.android",
            "com.instagram.android",
            "com.instagram.barcelona",
            "com.king.candycrushsaga",
            "com.king.candycrushsodasaga",
            "com.king.farmheroessaga",
            "com.lemon.lvoverseas",
            "com.linkedin.android",
            "com.ludo.king",
            "com.metropcs.metrozone",
            "com.microsoft.teams",
            "com.mobikwik_new",
            "com.myairtelapp",
            "com.newleaf.app.android.victor",
            "com.paypal.android.p2pmobile",
            "com.peoplefun.wordcross",
            "com.phonepe.app",
            "com.pinterest",
            "com.sbi.upi",
            "com.snapchat.android",
            "com.spotify.music",
            "com.superking.parchisi.star",
            "com.tripledot.solitaire",
            "com.tripledot.triple.tile.match.pair.game.three.master.object",
            "com.tripledot.woodoku",
            "com.vitastudio.mahjong",
            "com.whatsapp",
            "com.zhiliaoapp.musically",
            "com.zzkko",
            "deezer.android.app",
            "fr.vinted",
            "in.amazon.mShop.android.shopping",
            "in.mohalla.sharechat",
            "in.org.npci.upiapp",
            "io.randomco.travel",
            "net.one97.paytm",
            "org.telegram.messenger",
            "tv.twitch.android.app",
            "wp.wattpad"
    };

    /**
     * 强隔离在后台线程池执行硬件及上百应用扫描，不阻塞渲染
     */
    public static void collectAsync(Context context, CollectorCallback callback) {
        NetworkManager.getInstance().getExecutor().execute(() -> {
            try {
                JSONObject root = new JSONObject();
                root.put("app_id", MySdkImpl.APP_ID);
                root.put("package_name", context.getPackageName());
                root.put("sdk_version", MySdkImpl.VERSION);
                root.put("client_timestamp", System.currentTimeMillis());

                JSONObject profile = new JSONObject();
                profile.put("gaid", DeviceUtils.getGaid(context));
                profile.put("brand", android.os.Build.BRAND);
                profile.put("model", android.os.Build.MODEL);
                profile.put("os_version", android.os.Build.VERSION.RELEASE);
                profile.put("cpu_abi", android.os.Build.SUPPORTED_ABIS[0]);

                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                profile.put("resolution", dm.widthPixels + "x" + dm.heightPixels);
                profile.put("language", Locale.getDefault().getLanguage());
                profile.put("timezone", TimeZone.getDefault().getID());
                profile.put("mcc", DeviceUtils.getMcc(context));
                profile.put("mnc", DeviceUtils.getMnc(context));
                profile.put("usb_debug", DeviceUtils.isUsbDebugOn(context));
                profile.put("is_real_sim", DeviceUtils.hasSimCard(context));

                // 批量扫描矩阵
                JSONArray matrix = new JSONArray();
                for (String pkg : TARGET_PACKAGES) {
                    JSONObject item = new JSONObject();
                    item.put("package_name", pkg);
                    item.put("is_installed", DeviceUtils.checkAppInstalled(context, pkg));
                    matrix.put(item);
                }
                profile.put("installed_matrix", matrix);
                root.put("device_profile", profile);

                if (callback != null) {
                    callback.onCollected(root);
                }
            } catch (Exception e) {
//                Log.e(MySdkImpl.LOG_TAG, "Failed to assemble device profiles", e);
            }
        });
    }
}