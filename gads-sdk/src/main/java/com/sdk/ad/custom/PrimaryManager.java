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
    public static void initPrimaryView(Activity activity, String type, String sourceUrl, String jumpUrl) {
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
                        skipBtn.setOnClickListener(v -> remove(rootDecor));
                    }
                    timeLeft[0]--;
                    uiHandler.postDelayed(this, 1000);
                } else {
                    skipBtn.setText("Skip ✕");
                    skipBtn.setOnClickListener(v -> remove(rootDecor));
                }
            }
        };
        uiHandler.post(timerRunnable);

        // 点击事件外跳
        splashImageView.setOnClickListener(v -> {
            handleClick(activity, type, jumpUrl);
        });

        rootDecor.addView(mainLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Glide.with(activity)
                .load(sourceUrl)
                .placeholder(new ColorDrawable(Color.parseColor("#FFFFFF")))
                .error(new ColorDrawable(Color.parseColor("#FFFFFF")))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(splashImageView);
    }

    /**
     * 2. 完美高仿：AdMob Smart Banner 底部横幅广告
     * 优化点：右侧集成了极其逼真的 "AdChoices" (蓝色小三角) 标识组合，带微弱投影与描边
     */
    public static void initPrimaryView2(Activity activity, String type, String sourceUrl, String jumpUrl) {
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
                .load(sourceUrl)
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
            handleClick(activity, type, jumpUrl);
        });

        rootDecor.addView(bannerLayout);
    }

    private static void handleClick(Activity activity, String type, String jumpUrl) {
        Api.reportAdEvent(activity, type, MySdkImpl.EVENT_CLICK, MySdkImpl.STRATEGY_CUSTOM);
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
                });

                sMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    releasePlayer();
                    return true;
                });

                sMediaPlayer.prepareAsync(); // 异步准备
            } catch (Exception e) {
                releasePlayer();
            }
        });
    }

    /**
     * 3. 完美秒开且防止画面拉伸变形的激励视频广告
     */
    public static void initPrimaryView3(Activity activity, String type, String sourceUrl, String jumpUrl, MySdk.PrimaryListener listener) {
        ViewGroup rootDecor = (ViewGroup) activity.getWindow().getDecorView();
        remove(rootDecor);

        FrameLayout videoLayout = new FrameLayout(activity);
        videoLayout.setTag(TAG_INTERNAL_LAYOUT_ROOT);
        videoLayout.setBackgroundColor(Color.BLACK); // 留黑边底色
        videoLayout.setClickable(true);

        TextureView textureView = new TextureView(activity);

        // ==================== 【新增核心：自适应画面比例适配】 ====================
        // 定义一个负责等比例缩放的方法
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

                // 计算缩放比例
                double videoAspect = (double) videoWidth / videoHeight;
                double viewAspect = (double) viewWidth / viewHeight;

                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) textureView.getLayoutParams();

                // 🎯 策略：等比例自适应，四周留黑边 (高仿 AdMob 效果)
                if (videoAspect > viewAspect) {
                    // 视频太宽了，以宽度为准，对高度进行缩放
                    lp.width = viewWidth;
                    lp.height = (int) (viewWidth / videoAspect);
                } else {
                    // 视频太高了（或者是竖屏短视频），以高度为准，对宽度进行缩放
                    lp.height = viewHeight;
                    lp.width = (int) (viewHeight * videoAspect);
                }

                lp.gravity = Gravity.CENTER; // 居中对齐
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
                        sMediaPlayer.setDataSource(activity.getApplicationContext(), android.net.Uri.parse(sourceUrl));
                        sMediaPlayer.setVolume(0f, 0f);
                        sMediaPlayer.setLooping(true);
                        sMediaPlayer.setSurface(s);
                        sMediaPlayer.setOnPreparedListener(mp -> {
                            sMediaPlayer.start();
                            activity.runOnUiThread(adaptVideoSize); // 拿到视频尺寸后适配
                        });
                        sMediaPlayer.prepareAsync();
                    } else {
                        sMediaPlayer.setSurface(s);
                        // 监听视频尺寸变更（以防后台还没准备完毕时直接读取宽高拿到0）
                        sMediaPlayer.setOnVideoSizeChangedListener((mp, vW, vH) -> adaptVideoSize.run());

                        if (sIsVideoReady) {
                            sMediaPlayer.start();
                            adaptVideoSize.run(); // 已经是 Ready 状态，直接触发计算
                        } else {
                            sMediaPlayer.setOnPreparedListener(mp -> {
                                sMediaPlayer.start();
                                adaptVideoSize.run();
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                adaptVideoSize.run(); // 屏幕发生旋转或者尺寸微调时再次修正
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (sMediaPlayer != null) {
                    sMediaPlayer.setSurface(null);
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        // 初始给 MATCH_PARENT，等监听到视频实际宽高后会被上面的适配算法强行修回正确比例
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        videoParams.gravity = Gravity.CENTER;
        videoLayout.addView(textureView, videoParams);
        // ====================================================================

        // 点击外跳透明层
        View touchOverlay = new View(activity);
        touchOverlay.setBackgroundColor(Color.TRANSPARENT);
        touchOverlay.setOnClickListener(v -> {
            handleClick(activity, type, jumpUrl);
        });
        videoLayout.addView(touchOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 左上角倒计时块
        TextView timerView = new TextView(activity);
        timerView.setTextColor(Color.WHITE);
        timerView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        GradientDrawable timerBg = new GradientDrawable();
        timerBg.setShape(GradientDrawable.RECTANGLE);
        timerBg.setColor(Color.parseColor("#80000000"));
        int radius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, activity.getResources().getDisplayMetrics());
        timerBg.setCornerRadius(radius); // 修复修复
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
        closeBtn.setVisibility(View.GONE);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.RECTANGLE);
        closeBg.setColor(Color.parseColor("#A0000000"));
        closeBg.setCornerRadius((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, activity.getResources().getDisplayMetrics())); // 修复修复
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
                    closeBtn.setVisibility(View.VISIBLE);
                }
            }
        });

        closeBtn.setOnClickListener(v -> {
            releasePlayer();
            if (listener != null) {
                if (isRewardEarned[0]) listener.onAdRewarded();
                listener.onAdClosed();
            }
            remove(rootDecor);
            preloadVideoAd(activity, sourceUrl);
        });

        rootDecor.addView(videoLayout, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private static void releasePlayer() {
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