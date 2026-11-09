# ============================================================
# ProGuard / R8 Rules for D54 Video Downloader
# com.video.downloader.downloadvideo.videodownloader
# ============================================================

# ---------------------- General Optimization & Debug ----------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes Exceptions

# ---------------------- Kotlin & Coroutines ----------------------
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.jvm.internal.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------------------- App Domain, Models & Data ----------------------
-keep class com.video.downloader.downloadvideo.videodownloader.domain.model.** { *; }
-keep class com.video.downloader.downloadvideo.videodownloader.data.model.** { *; }
-keep class com.video.downloader.downloadvideo.videodownloader.data.local.** { *; }
-keep class com.video.downloader.downloadvideo.videodownloader.data.network.** { *; }
-keep class com.video.downloader.downloadvideo.videodownloader.core.ads.RemoteConfigs { *; }
-keepclassmembers class com.video.downloader.downloadvideo.videodownloader.core.ads.RemoteConfigs { *; }

# ---------------------- Room Database ----------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}
-dontwarn androidx.room.paging.**

# ---------------------- Hilt & Dagger ----------------------
-dontwarn dagger.internal.codegen.**
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* *;
    @dagger.* *;
}
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class dagger.hilt.** { *; }

# ---------------------- Retrofit 2, OkHttp & Gson ----------------------
# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.Expose <fields>;
}

# ---------------------- Glide ----------------------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}
-dontwarn com.bumptech.glide.**

# ---------------------- Media3 (ExoPlayer) ----------------------
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.ui.** { *; }

# ---------------------- Jsoup ----------------------
-keep public class org.jsoup.** { public *; }
-dontwarn org.jsoup.**

# ---------------------- Lottie & UI Helpers ----------------------
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**
-keep class com.intuit.sdp.** { *; }
-keep class com.intuit.ssp.** { *; }
-keep class com.facebook.shimmer.** { *; }

# ---------------------- Firebase & Google Play Services ----------------------
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep public class * extends java.lang.Exception

# ---------------------- CSC App Library & Configs ----------------------
-keep class com.cscapp.** { *; }
-keep class com.cscmobi.libraryads.** { *; }
-keep interface com.cscmobi.libraryads.** { *; }
-keepclassmembers class * implements com.cscmobi.libraryads.commons.remote.CSCKonfigModel { *; }
-dontwarn com.cscapp.**
-dontwarn com.cscmobi.libraryads.**

# ---------------------- Adjust SDK ----------------------
-keep class com.adjust.sdk.** { *; }
-dontwarn com.adjust.sdk.**

# ---------------------- Ads Mediation Adapters ----------------------
# Meta / Facebook Audience Network
-keep class com.google.ads.mediation.facebook.** { *; }
-keep class com.facebook.ads.** { *; }
-keep class com.facebook.internal.** { *; }
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.internal.**

# AppLovin Mediation
-keep class com.google.ads.mediation.applovin.** { *; }
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**

# InMobi Mediation
-keep class com.google.ads.mediation.inmobi.** { *; }
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**
-dontwarn com.squareup.picasso.**

# Pangle SDK
-keep class com.bytedance.sdk.** { *; }
-keep interface com.bytedance.sdk.** { *; }
-keep class com.bytedance.adsdk.** { *; }
-keep class com.bytedance.mobsec.** { *; }
-keep class com.bytedance.applog.** { *; }
-keep class com.apm.insight.** { *; }
-keep class com.bykv.vk.** { *; }
-keep interface com.bykv.vk.** { *; }
-keep class com.com.bytedance.overseas.sdk.** { *; }
-keep class com.iab.omid.library.bytedance2.** { *; }
-keep class com.pgl.ssdk.** { *; }
-keep class com.tiktok.** { *; }
-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.bykv.vk.openvk.** { *; }
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Pangle JNI & Native Security (Crucial for Bidding Token Generation)
-keep class ms.bd.o.Pgl.** { *; }
-keep class ms.bd.c.** { *; }
-keep class ms.bd.o.** { *; }
-keep class com.pgl.sys.ces.** { *; }

# Google AdMob Mediation Adapter for Pangle
-keep class com.google.ads.mediation.pangle.** { *; }

# Mintegral SDK
-keep class com.mbridge.** { *; }
-dontwarn com.mbridge.**
-keep interface com.mbridge.** { *; }
-keep class * extends com.mbridge.msdk.out.BaseExtraInterfaceForHandler { *; }
-keep class com.google.ads.mediation.mintegral.** { *; }

# Unity Ads Mediation
-keep class com.google.ads.mediation.unity.** { *; }
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.ads.**
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.services.**

# Pubscale Adapter
-keep class com.pubscale.** { *; }
-dontwarn com.pubscale.**

# ---------------------- Architecture / Lifecycle / ViewBinding ----------------------
-keep public class * extends androidx.fragment.app.Fragment {
    public <init>();
}
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }

-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(...);
}

-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ---------------------- Parcelable & Serializable ----------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------------------- Enums ----------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------------------- WebViews & Native Methods ----------------------
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class * extends android.webkit.WebViewClient { *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { *; }

-keepclasseswithmembernames class * {
    native <methods>;
}
