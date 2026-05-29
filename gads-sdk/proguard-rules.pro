-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-dontoptimize
-dontshrink
-allowaccessmodification
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes JavascriptInterface
-keepattributes LineNumberTable
-keepattributes Signature
-keepattributes SourceFile

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
-dontnote com.android.vending.licensing.ILicensingService

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-ignorewarnings
-keep class com.google.**{*;}


-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.**{*;}
-keep class kotlinx.**{*;}
-keeppackagenames kotlin.**,kotlinx.**,gnu.**,com.android.**,android.**,androidx.**,com.google.**

-keep class sun.misc.Unsafe { *; }
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    void set*(***);
    *** get*();
}

-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keep class **.R$* {
    public static <fields>;
}

-keep @android.support.annotation.Keep class *
-keep @android.support.annotation.Keep interface *

-keepclassmembers class * {
    @android.support.annotation.Keep <methods>;
}

-keepclassmembers class * {
    @android.support.annotation.Keep <fields>;
}

-keepclassmembers interface * {
    @android.support.annotation.Keep <methods>;
}

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}


#######################################################################
-keep class lfsldetrgiok.Xhczswkkzp { *; }
-keep class lfsldetrgiok.C7yusgr8nv { *; }

-dontwarn com.didi.virtualapk.**
-dontwarn android.**
-keep class android.** { *; }

# 指定重新打包,所有包重命名
-flattenpackagehierarchy

# 所有包重命名
-flattenpackagehierarchy
# 为每个类成员生成唯一的名称，而不考虑它们的语义。这将导致类成员（如方法和字段）的名称被更改。
-useuniqueclassmembernames
# 混淆后类名都小写
-dontusemixedcaseclassnames
# 指定不去忽略非公共的库的类(即混淆第三方, 第三方库可能自己混淆了 , 可在后面配置某些第三方库不混淆)
-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共的库的类的成员
-dontskipnonpubliclibraryclassmembers

-dontwarn com.didi.virtualapk.**
-dontwarn android.**
-keep class android.** { *; }
# 保护代码中的Annotation不被混淆
-keepattributes *Annotation*,InnerClasses
# 避免混淆泛型
-keepattributes Signature
#抛出异常时保留源文件和代码行号
-keepattributes SourceFile,LineNumberTable,Deprecated
# ------------------------------- 基本指令区 -------------------------------

# ------------------------------- 默认保留区 -------------------------------

# 保留四大组件
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
# 保留就保证layout中定义的onClick方法不影响
-keepclassmembers class * extends android.app.Activity{
    public void *(android.view.View);
}
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.Application

# 保留类名和native成员方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留自定义控件
-keep public class * extends android.view.View{
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 这指定了继承Parcelable/Serizalizable的类的如下成员不被移除混淆
-keep public class * implements java.io.Serializable {*;}
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# 对R文件下的所有类及其方法 , 都不能被混淆
-keep class **.R$* {
    *;
}

# 官方
-keepclassmembers class **.R$* {
    public static <fields>;
}

-dontwarn android.support.**
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * {*;}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <fields>;
}
-keepclasseswithmembers class * {
    @androidx.annotation.Keep <init>(...);
}

# ------------------------------- 默认保留区 end-------------------------------

# webview相关
-dontwarn android.webkit**
-keepclassmembers class * extends android.webkit.WebView {
    public *;
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView,java.lang.String,android.graphics.Bitmap);
    public boolean *(android.webkit.WebView,java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public void *(android.webkit.WebView,java.lang.String);
}
# 与JS交互
-keepattributes SetJavaScriptEnabled
-keepattributes JavascriptInterface
# 保留与JS交互接口
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-dontwarn org.apache.**
-dontwarn org.codehaus.**
-dontwarn java.nio.**
-dontwarn java.lang.invoke.**
-dontwarn rx.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn com.google.**

# okhttp3
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# glide
-keep class com.aw.bumptech.glide.** {*;}

# Adjust
-keep public class com.adjust.sdk.**{ *; }
-keep public class com.android.installreferrer.**{ *; }
-keep class com.adjust.sdk.**{ *; }
-keep class com.google.android.gms.common.ConnectionResult {
    int SUCCESS;
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {
    com.google.android.gms.ads.identifier.AdvertisingIdClient$Info getAdvertisingIdInfo(android.content.Context);
}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {
    java.lang.String getId();
    boolean isLimitAdTrackingEnabled();
}
-keep public class com.android.installreferrer.**{ *; }
#Appsflyer
-keep class com.appsflyer.** { *; }
#sdk
-keep class com.sdk.ad.MySdk { *; }
-keep class com.sdk.ad.MySdk$* { *; }