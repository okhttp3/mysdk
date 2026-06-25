package com.sdk.ad.custom;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.sdk.ad.MySdk;
import com.sdk.ad.MySdkImpl;
import com.sdk.ad.network.Api;
import com.sdk.ad.util.AdUrlHttpUtil;

import static com.sdk.ad.MySdkImpl.LOG_TAG;

public class PrimaryManager {
    public static final String TAG_INTERNAL_LAYOUT_ROOT = "tag_internal_layout_root";
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    /**
     * Helper: 快捷将 dp 转换为 px
     */
    private static int dpToPx(Activity activity, int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, activity.getResources().getDisplayMetrics());
    }

    /**
     * Helper: 生成高仿谷歌风格的半透明/实色圆角背景
     */
    private static GradientDrawable createRoundDrawable(String colorHex, int radiusDp, Activity activity) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.parseColor(colorHex));
        drawable.setCornerRadius(dpToPx(activity, radiusDp));
        return drawable;
    }

    /**
     * 1. 完美高仿：AdMob App Open / Splash 全屏开屏广告
     * 优化点：右上角高仿谷歌标准的胶囊型倒计时跳过按钮，左上角精细化 Ad 标签，底部打底白色安全区
     */
    public static void initPrimaryView(Activity activity, String type, MySdkImpl.AdUnitConfig config, MySdk.PrimaryListener listener) {
        ViewGroup rootDecor = (ViewGroup) activity.getWindow().getDecorView();
        remove(rootDecor);

        FrameLayout mainLayout = new FrameLayout(activity);
        mainLayout.setTag(TAG_INTERNAL_LAYOUT_ROOT);
        mainLayout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        mainLayout.setClickable(true);
        mainLayout.setFocusable(true);

        // 广告主图
        ImageView splashImageView = new ImageView(activity);
        splashImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        // 给底部留出 60dp 的标准 AdMob 假打底色块空间，防止遮挡全面屏手势
        imgParams.bottomMargin = dpToPx(activity, 60);
        mainLayout.addView(splashImageView, imgParams);

        // 左上角：精细化 "Ad" 标签 (高仿 AdMob 浅灰色微圆角小标)
        TextView adTag = new TextView(activity);
        adTag.setText("Ad");
        adTag.setTextColor(Color.parseColor("#FFFFFF"));
        adTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        adTag.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        adTag.setGravity(Gravity.CENTER);
        adTag.setPadding(dpToPx(activity, 5), dpToPx(activity, 1), dpToPx(activity, 5), dpToPx(activity, 1));
        adTag.setBackground(createRoundDrawable("#A0000000", 3, activity)); // 半透明灰黑，确保各种背景都能看清

        FrameLayout.LayoutParams tagParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tagParams.gravity = Gravity.TOP | Gravity.LEFT;
        tagParams.setMargins(dpToPx(activity, 16), dpToPx(activity, 16), 0, 0);
        mainLayout.addView(adTag, tagParams);

        // 右上角：高仿谷歌标准的“胶囊型”跳过按钮
        TextView skipBtn = new TextView(activity);
        skipBtn.setTextColor(Color.WHITE);
        skipBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        skipBtn.setGravity(Gravity.CENTER);
        skipBtn.setPadding(dpToPx(activity, 14), dpToPx(activity, 6), dpToPx(activity, 14), dpToPx(activity, 6));
        skipBtn.setBackground(createRoundDrawable("#80000000", 16, activity)); // 标准胶囊圆角

        FrameLayout.LayoutParams skipParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        skipParams.gravity = Gravity.TOP | Gravity.RIGHT;
        skipParams.setMargins(0, dpToPx(activity, 16), dpToPx(activity, 16), 0);
        mainLayout.addView(skipBtn, skipParams);

        // 假装底部有技术提供商标识，大幅提升逼真度
        TextView providerTag = new TextView(activity);
        providerTag.setText("Powered by Google AdMob");
        providerTag.setTextColor(Color.parseColor("#999999"));
        providerTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        FrameLayout.LayoutParams providerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        providerParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        providerParams.bottomMargin = dpToPx(activity, 20);
        mainLayout.addView(providerTag, providerParams);

        // 倒计时核心逻辑 (前 2 秒强留不可跳过，符合真实体验；2秒后可随时点击跳过)
        final int[] timeLeft = {5};
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) return;
                if (mainLayout.getParent() == null) return;

                if (timeLeft[0] > 0) {
                    skipBtn.setText("Skip in " + timeLeft[0] + "s");
                    // 谷歌规范：前几秒是不允许点击的
                    if (timeLeft[0] <= 3) {
                        skipBtn.setText("Skip ✕");
                        skipBtn.setOnClickListener(v -> closeOpen(listener, rootDecor));
                    }
                    timeLeft[0]--;
                    uiHandler.postDelayed(this, 1000);
                } else {
                    skipBtn.setText("Skip ✕");
                    skipBtn.setOnClickListener(v -> closeOpen(listener, rootDecor));
                }
            }
        };
        uiHandler.post(timerRunnable);

        // 点击事件外跳
        splashImageView.setOnClickListener(v -> {
            handleClick(activity, type, config, config.jumpUrl);
        });

        rootDecor.addView(mainLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Glide.with(activity)
                .load(config.sourceUrl)
                .placeholder(new ColorDrawable(Color.parseColor("#FFFFFF")))
                .error(new ColorDrawable(Color.parseColor("#FFFFFF")))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(splashImageView);
    }

    private static void closeOpen(MySdk.PrimaryListener listener, ViewGroup rootDecor) {
        if (listener != null) {
            listener.onAdClosed();
        }
        remove(rootDecor);
    }

    /**
     * 2. 完美高仿：AdMob Smart Banner 底部横幅广告
     * 优化点：右侧集成了极其逼真的 "AdChoices" (蓝色小三角) 标识组合，带微弱投影与描边
     */
    public static void initPrimaryView2(Activity activity, String type, MySdkImpl.AdUnitConfig config) {
        ViewGroup rootDecor = (ViewGroup) activity.getWindow().getDecorView();
        remove(rootDecor);

        int bannerHeightPx = dpToPx(activity, 50);

        FrameLayout bannerLayout = new FrameLayout(activity);
        bannerLayout.setTag(TAG_INTERNAL_LAYOUT_ROOT);
        bannerLayout.setBackgroundColor(Color.parseColor("#F1F1F1")); // 谷歌标准浅灰打底

        FrameLayout.LayoutParams baseParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, bannerHeightPx);
        baseParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        bannerLayout.setLayoutParams(baseParams);

        // 广告大图
        ImageView adImageView = new ImageView(activity);
        adImageView.setScaleType(ImageView.ScaleType.FIT_XY);

        Glide.with(activity)
                .load(config.sourceUrl)
                .placeholder(new ColorDrawable(Color.parseColor("#E0E0E0")))
                .error(new ColorDrawable(Color.parseColor("#E0E0E0")))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(adImageView);

        FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        bannerLayout.addView(adImageView, imgParams);

        // ==================== 【视觉精髓：右上方 AdChoices 悬浮控制台】 ====================
        LinearLayout adChoicesInfo = new LinearLayout(activity);
        adChoicesInfo.setOrientation(LinearLayout.HORIZONTAL);
        adChoicesInfo.setGravity(Gravity.CENTER_VERTICAL);
        adChoicesInfo.setPadding(dpToPx(activity, 4), dpToPx(activity, 1), dpToPx(activity, 4), dpToPx(activity, 1));
        // 右上角微小半圆角打底
        adChoicesInfo.setBackground(createRoundDrawable("#B0FFFFFF", 4, activity));

        // "Ad" 字样
        TextView miniAdTag = new TextView(activity);
        miniAdTag.setText("Ad");
        miniAdTag.setTextColor(Color.parseColor("#666666"));
        miniAdTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9);
        miniAdTag.setTypeface(Typeface.DEFAULT_BOLD);
        adChoicesInfo.addView(miniAdTag);

        // 谷歌著名的蓝色小三角图标 (这里用 Unicode 字符 ▷ 代替，配合浅蓝色假装是 AdChoices 标志)
        TextView choiceIcon = new TextView(activity);
        choiceIcon.setText(" ▷");
        choiceIcon.setTextColor(Color.parseColor("#0073E6")); // 谷歌蓝
        choiceIcon.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
        adChoicesInfo.addView(choiceIcon);

        // ✕ 关闭按钮
        TextView closeBtn = new TextView(activity);
        closeBtn.setText(" ✕");
        closeBtn.setTextColor(Color.parseColor("#666666"));
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        closeBtn.setPadding(dpToPx(activity, 4), 0, dpToPx(activity, 2), 0);
        adChoicesInfo.addView(closeBtn);

        FrameLayout.LayoutParams infoParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.gravity = Gravity.TOP | Gravity.RIGHT;
        bannerLayout.addView(adChoicesInfo, infoParams);
        // ===================================================================================

        // 点击事件隔离
        closeBtn.setOnClickListener(v -> remove(rootDecor));
        adImageView.setOnClickListener(v -> {
            handleClick(activity, type, config, config.jumpUrl);
        });

        rootDecor.addView(bannerLayout);
    }

    private static void handleClick(Activity activity, String type, MySdkImpl.AdUnitConfig config, String jumpUrl) {
        Api.reportAdEvent(activity, type, config.adId, MySdkImpl.EVENT_CLICK, MySdkImpl.STRATEGY_CUSTOM);
        AdUrlHttpUtil.openBrowser(activity, jumpUrl);
    }

    // ==================== 【终极重构：解耦底层播放器】 ====================
    private static MediaPlayer sMediaPlayer = null;
    private static boolean sIsVideoReady = false;

    /**
     * 在初始化或后台无感“真·预加载”
     */
    public static void preloadVideoAd(Context context, String sourceUrl) {
        // 已有缓存，跳过
        if (sMediaPlayer != null) return;
        uiHandler.post(() -> {
            try {
                sMediaPlayer = new MediaPlayer();
                sIsVideoReady = false;

                // 1. 设置数据源（异步非阻塞网络握手）
                sMediaPlayer.setDataSource(context.getApplicationContext(), android.net.Uri.parse(sourceUrl));
                sMediaPlayer.setVolume(0f, 0f); // 默认静音
                sMediaPlayer.setLooping(true);

                sMediaPlayer.setOnPreparedListener(mp -> {
                    sIsVideoReady = true; // 真正缓冲完毕，后台处于就绪/暂停状态
                    Log.e(LOG_TAG, "视频激励广告，预加载缓冲完毕，后台处于就绪/暂停状态");
                });

                sMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(LOG_TAG, "视频激励广告，预加载缓冲错误");
                    releasePlayer();
                    return true;
                });

                sMediaPlayer.prepareAsync(); // 异步准备
            } catch (Exception e) {
                Log.e(LOG_TAG, "视频激励广告，预加载缓冲报错：" + e.toString());
                releasePlayer();
            }
        });
    }

    /**
     * 3. 完美秒开且防止画面拉伸变形的激励视频广告 (内存常驻复用版)
     * 修改点：关闭按钮默认可见，允许随时关闭；满足15秒条件才发放奖励
     */
    public static void initPrimaryView3(Activity activity, String type, MySdkImpl.AdUnitConfig config, MySdk.PrimaryListener listener) {
        ViewGroup rootDecor = (ViewGroup) activity.getWindow().getDecorView();
        remove(rootDecor);

        FrameLayout videoLayout = new FrameLayout(activity);
        videoLayout.setTag(TAG_INTERNAL_LAYOUT_ROOT);
        videoLayout.setBackgroundColor(Color.BLACK);
        videoLayout.setClickable(true);

        TextureView textureView = new TextureView(activity);

        // 自适应画面比例适配逻辑
        final Runnable adaptVideoSize = () -> {
            if (sMediaPlayer == null || textureView.getWidth() == 0 || textureView.getHeight() == 0)
                return;
            try {
                int videoWidth = sMediaPlayer.getVideoWidth();
                int videoHeight = sMediaPlayer.getVideoHeight();
                if (videoWidth == 0 || videoHeight == 0) return;

                int viewWidth = videoLayout.getWidth();
                int viewHeight = videoLayout.getHeight();
                if (viewWidth == 0 || viewHeight == 0) {
                    viewWidth = activity.getResources().getDisplayMetrics().widthPixels;
                    viewHeight = activity.getResources().getDisplayMetrics().heightPixels;
                }

                double videoAspect = (double) videoWidth / videoHeight;
                double viewAspect = (double) viewWidth / viewHeight;
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) textureView.getLayoutParams();

                if (videoAspect > viewAspect) {
                    lp.width = viewWidth;
                    lp.height = (int) (viewWidth / videoAspect);
                } else {
                    lp.height = viewHeight;
                    lp.width = (int) (viewHeight * videoAspect);
                }
                lp.gravity = Gravity.CENTER;
                textureView.setLayoutParams(lp);
            } catch (Exception ignored) {
            }
        };

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                android.view.Surface s = new android.view.Surface(surface);
                try {
                    if (sMediaPlayer == null) {
                        sMediaPlayer = new MediaPlayer();
                        sMediaPlayer.setDataSource(activity.getApplicationContext(), android.net.Uri.parse(config.sourceUrl));
                        sMediaPlayer.setVolume(0f, 0f);
                        sMediaPlayer.setLooping(true);
                        sMediaPlayer.setSurface(s);
                        sMediaPlayer.setOnPreparedListener(mp -> {
                            sIsVideoReady = true;
                            sMediaPlayer.start();
                            activity.runOnUiThread(adaptVideoSize);
                        });
                        sMediaPlayer.prepareAsync();
                    } else {
                        // 针对常驻复用的 MediaPlayer
                        sMediaPlayer.setSurface(s);
                        sMediaPlayer.setOnVideoSizeChangedListener((mp, vW, vH) -> adaptVideoSize.run());

                        // 设置定位完成监听，确保第一帧在新的 Surface 上渲染完毕后再开播
                        sMediaPlayer.setOnSeekCompleteListener(mp -> {
                            sMediaPlayer.start();
                            adaptVideoSize.run();
                        });

                        // 无论是播完的还是播到一半关闭的，统一在这里画面就绪后回滚到 0 帧
                        sMediaPlayer.seekTo(0);
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "绑定Surface或播放失败: " + e.getMessage());
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                adaptVideoSize.run();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (sMediaPlayer != null) {
                    try {
                        sMediaPlayer.setSurface(null);
                    } catch (Exception ignored) {
                    }
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        videoParams.gravity = Gravity.CENTER;
        videoLayout.addView(textureView, videoParams);

        // 透明点击外跳层
        View touchOverlay = new View(activity);
        touchOverlay.setBackgroundColor(Color.TRANSPARENT);
        touchOverlay.setOnClickListener(v -> handleClick(activity, type, config, config.jumpUrl));
        videoLayout.addView(touchOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 左上角倒计时
        TextView timerView = new TextView(activity);
        timerView.setTextColor(Color.WHITE);
        timerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        GradientDrawable timerBg = new GradientDrawable();
        timerBg.setShape(GradientDrawable.RECTANGLE);
        timerBg.setColor(Color.parseColor("#80000000"));
        int radius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, activity.getResources().getDisplayMetrics());
        timerBg.setCornerRadius(radius);
        timerView.setBackground(timerBg);
        int paddingHor = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, activity.getResources().getDisplayMetrics());
        int paddingVer = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, activity.getResources().getDisplayMetrics());
        timerView.setPadding(paddingHor, paddingVer, paddingHor, paddingVer);
        FrameLayout.LayoutParams timerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timerParams.gravity = Gravity.TOP | Gravity.LEFT;
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, activity.getResources().getDisplayMetrics());
        timerParams.setMargins(margin, margin, 0, 0);
        videoLayout.addView(timerView, timerParams);

        // 右上角关闭按钮
        TextView closeBtn = new TextView(activity);
        closeBtn.setText("✕");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        closeBtn.setGravity(Gravity.CENTER);
        // 【修改点】让关闭按钮从一开始就直接可见
        closeBtn.setVisibility(View.VISIBLE);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.RECTANGLE);
        closeBg.setColor(Color.parseColor("#A0000000"));
        closeBg.setCornerRadius((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, activity.getResources().getDisplayMetrics()));
        closeBtn.setBackground(closeBg);
        int btnSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, activity.getResources().getDisplayMetrics());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(btnSize, btnSize);
        closeParams.gravity = Gravity.TOP | Gravity.RIGHT;
        closeParams.setMargins(0, margin, margin, 0);
        videoLayout.addView(closeBtn, closeParams);

        final int[] secondsRemaining = {15};
        final boolean[] isRewardEarned = {false};

        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                if (activity.isFinishing()) return;
                if (videoLayout.getParent() == null) return;

                if (secondsRemaining[0] > 0) {
                    timerView.setText("Reward in " + secondsRemaining[0] + "s");
                    secondsRemaining[0]--;
                    uiHandler.postDelayed(this, 1000);
                } else {
                    isRewardEarned[0] = true;
                    timerView.setText("Reward granted ✓");
                }
            }
        });

        closeBtn.setOnClickListener(v -> {
            remove(rootDecor);
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (sMediaPlayer != null) {
                            sMediaPlayer.pause(); // 只暂停，不在这里 seekTo(0)
                            Log.e(LOG_TAG, "视频激励广告已关闭，保留MediaPlayer实例并暂停");
                        }
                    } catch (Exception exception) {
                        Log.e(LOG_TAG, "视频激励广告关闭暂停报错：" + exception);
                    }

                    if (listener != null) {
                        if (isRewardEarned[0]) {
                            listener.onAdRewarded();
                        } else {
                            listener.onAdClosed();
                        }
                    }
                }
            });
        });

        rootDecor.addView(videoLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static void releasePlayer() {
        Log.e(LOG_TAG, "视频激励广告释放资源releasePlayer");

        try {
            if (sMediaPlayer != null) {
                sMediaPlayer.stop();
                sMediaPlayer.release();
            }
        } catch (Exception ignored) {
        }
        sMediaPlayer = null;
        sIsVideoReady = false;
    }

    private static void remove(ViewGroup rootDecor) {
        int childCount = rootDecor.getChildCount();
        for (int i = childCount - 1; i >= 0; i--) {
            View child = rootDecor.getChildAt(i);
            if (TAG_INTERNAL_LAYOUT_ROOT.equals(child.getTag())) {
                rootDecor.removeView(child);
            }
        }
    }
}