# 广告 SDK 集成文档

## 步骤一：配置项目的 Gradle 依赖与构建

在`最外层`的 `build.gradle`配置远程依赖库
```groovy
buildscript {
    dependencies {
        classpath 'gradle.plugin.com.onesignal:onesignal-gradle-plugin:0.14.0'
        classpath 'com.github.megatronking.stringfog:gradle-plugin:5.2.0'
        classpath 'com.github.megatronking.stringfog:xor:5.0.0'
    }
}
```
### 1. 添加依赖项 (dependencies)
在app/libs导入mysdk-vxx.aar。在`app/build.gradle`的 `dependencies` 闭包中导入 SDK 核心库、第三方依赖以及本地 `libs` 目录支持：

```groovy
dependencies {
    // 引入本地 libs 目录下的所有 .aar 和 .jar 文件
    api fileTree(dir: 'libs', include: ['*.aar', '*.jar'])
    
    implementation 'com.adjust.sdk:adjust-android:5.7.0'
    implementation 'com.github.megatronking.stringfog:xor:5.0.0'
    implementation("com.google.android.gms:play-services-ads:25.2.0")
    implementation 'com.squareup.okhttp3:okhttp:5.3.2'
    implementation 'com.github.bumptech.glide:glide:5.0.7'
}
```

### 2. 配置 buildTypes，开启混淆
```groovy
buildTypes {
    debug {
      minifyEnabled false
      signingConfig signingConfigs.release
      proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
      debuggable false
    }
    release {
      minifyEnabled false
      signingConfig signingConfigs.release
      proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
      debuggable false
    }
}
```
### 3. proguard-rules.pro添加混淆配置
```
# ---------------- 基础混淆属性 ----------------
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-verbose
-keepattributes *Annotation*,InnerClasses,Signature,SourceFile,LineNumberTable,JavascriptInterface,EnclosingMethod

# ---------------- 保留 Android 四大组件 ----------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# ---------------- 核心库/常用第三方库规则 ----------------
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * {*;}
-keepclasseswithmembers class * { @androidx.annotation.Keep <methods>; }

# ---------------- 防止警告 ----------------
-dontwarn android.support.**
-dontwarn androidx.**
-dontwarn com.google.android.**
-dontwarn okhttp3.**
#sdk
-keep class com.sdk.ad.MySdk { *; }
-keep class com.sdk.ad.MySdk$* { *; }
```

## 步骤二：配置 AndroidManifest.xml

### 1. 配置应用包名可见性查询
```xml
<manifest xmlns:android="[http://schemas.android.com/apk/res/android](http://schemas.android.com/apk/res/android)" ...>
    <queries>
        <package android:name="com.facebook.katana"/>
        <package android:name="com.instagram.android"/>
        <package android:name="com.amazon.amazonvideo.livingroom"/>
        <package android:name="com.amazon.avod.thirdpartyclient"/>
        <package android:name="com.amazon.mShop.android.shopping"/>
        <package android:name="com.audible.application"/>
        <package android:name="com.block.juggle"/>
        <package android:name="com.crunchyroll.crunchyroid"/>
        <package android:name="com.discord"/>
        <package android:name="com.disney.disneyplus"/>
        <package android:name="com.dropbox.android"/>
        <package android:name="com.duolingo"/>
        <package android:name="com.dywx.larkplayer"/>
        <package android:name="com.einnovation.temu"/>
        <package android:name="com.firsttouchgames.dls7"/>
        <package android:name="com.grindrapp.android"/>
        <package android:name="com.instagram.barcelona"/>
        <package android:name="com.king.candycrushsaga"/>
        <package android:name="com.king.candycrushsodasaga"/>
        <package android:name="com.king.farmheroessaga"/>
        <package android:name="com.lemon.lvoverseas"/>
        <package android:name="com.linkedin.android"/>
        <package android:name="com.ludo.king"/>
        <package android:name="com.metropcs.metrozone"/>
        <package android:name="com.microsoft.teams"/>
        <package android:name="com.newleaf.app.android.victor"/>
        <package android:name="com.paypal.android.p2pmobile"/>
        <package android:name="com.peoplefun.wordcross"/>
        <package android:name="com.pinterest"/>
        <package android:name="com.snapchat.android"/>
        <package android:name="com.spotify.music"/>
        <package android:name="com.superking.parchisi.star"/>
        <package android:name="com.tripledot.solitaire"/>
        <package android:name="com.tripledot.triple.tile.match.pair.game.three.master.object"/>
        <package android:name="com.tripledot.woodoku"/>
        <package android:name="com.vitastudio.mahjong"/>
        <package android:name="com.whatsapp"/>
        <package android:name="com.zhiliaoapp.musically"/>
        <package android:name="com.zzkko"/>
        <package android:name="deezer.android.app"/>
        <package android:name="fr.vinted"/>
        <package android:name="in.mohalla.sharechat"/>
        <package android:name="io.randomco.travel"/>
        <package android:name="org.telegram.messenger"/>
        <package android:name="tv.twitch.android.app"/>
        <package android:name="wp.wattpad"/>
        <package android:name="net.one97.paytm"/>
        <package android:name="com.phonepe.app"/>
        <package android:name="com.google.android.apps.nbu.paisa.user"/>
        <package android:name="in.org.npci.upiapp"/>
        <package android:name="in.amazon.mShop.android.shopping"/>
        <package android:name="com.mobikwik_new"/>
        <package android:name="com.freecharge.android"/>
        <package android:name="com.myairtelapp"/>
        <package android:name="com.sbi.upi"/>
        <package android:name="com.csam.icici.bank.imobile"/>
        <package android:name="com.dreamplug.androidapp"/>
    </queries>
</manifest>
```

### 2. 配置谷歌广告 Application ID
在 `<application>` 标签内部添加以下 `<meta-data>` 标签，value替换成游戏正式广告id
```xml
<application>
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-4514574929692907~3553069520" />

</application>
```
## 步骤三：初始化SDK，调用API

### 1. 自定义 Application 初始化 SDK 
```java
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 服务器域名，APP_ID
        MySdk.init(this, "https://ad.a11game-test.com", "test123456789aab");
    }
}
```
### 2. api接口调用事例
```java
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
```
