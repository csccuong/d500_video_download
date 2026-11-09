package com.video.downloader.downloadvideo.videodownloader

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cscmobi.libraryads.CSCApplication
import com.cscmobi.libraryads.commons.sharepreference.CSCSPF
import com.cscmobi.libraryads.data.AdsLanguageConfig
import com.cscmobi.libraryads.data.AdsOBConfig
import com.cscmobi.libraryads.data.AdsSplashConfig
import com.cscmobi.libraryads.data.LanguageConfig
import com.cscmobi.libraryads.data.LanguageSetting
import com.cscmobi.libraryads.data.OBConfig
import com.cscmobi.libraryads.data.OnActivityCallBack
import com.cscmobi.libraryads.data.SplashConfig
import com.cscmobi.libraryads.data.UiLanguageConfig
import com.cscmobi.libraryads.data.UiOBConfig
import com.cscmobi.libraryads.data.UiSplashConfig
import com.video.downloader.downloadvideo.videodownloader.core.ads.AdsManager
import com.video.downloader.downloadvideo.videodownloader.core.ads.RemoteConfigs
import com.video.downloader.downloadvideo.videodownloader.core.utils.EnumSelectLanguage
import com.video.downloader.downloadvideo.videodownloader.core.utils.SystemUtil
import com.video.downloader.downloadvideo.videodownloader.presentation.main.MainActivity
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DownloaderApp : Application() {

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(SystemUtil.wrapContext(base))
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        SystemUtil.setLocale(this)
    }

    override fun onCreate() {
        super.onCreate()
        SystemUtil.setLocale(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName = getProcessName()
            if (packageName != processName) {
                try {
                    android.webkit.WebView.setDataDirectorySuffix(processName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        initFO()
        setupFullScreenLifecycle()
    }

    private fun initFO() {
        val cscLibrary = CSCApplication(this, RemoteConfigs, getString(R.string.app_id), BuildConfig.DEBUG)

        cscLibrary.initSdk(
            adjustAppToken = "pndme5cex4ao",
            gsmAppId = "6aeeab50c3a0fbea0a8d2716",
            splashConfig = SplashConfig(
                uiSplashConfig = UiSplashConfig(
                    resLayout = R.layout.activity_splash,
                    showFOForever = false,
                    homeActivity = MainActivity::class.java,
                    timeout = 45_000
                ),
                adsSplashConfig = AdsSplashConfig(
                    bannerId = getString(R.string.banner_splash),
                    interHighId = getString(R.string.inter_splash_high),
                    interAllId = getString(R.string.inter_splash),
                    nativeFullLayout = R.layout.layout_native_full,
                    admobAOAId = getString(R.string.resume_open_app),
                    isCheckOrganicUser = true
                )
            ),
            languageConfig = LanguageConfig(
                uiLanguageConfig = UiLanguageConfig(
                    resLayout = R.layout.activity_language_app,
                    itemLangDefault = R.layout.item_select_language_default,
                    itemLangSelected = R.layout.item_select_language_selected,
                    listLanguage = EnumSelectLanguage.toLanguageModelList(),
                    languageSetting = object : LanguageSetting {
                        override fun onDone(activity: Activity) {
                            val code = CSCSPF(activity).language_code_selected
                            val previousCode = SystemUtil.getPreLanguage(activity)
                            val finalCode = if (!code.isNullOrBlank()) code else previousCode
                            android.util.Log.d("LanguageDebug", "App languageSetting.onDone: code=$code, finalCode=$finalCode")
                            SystemUtil.setPreLanguage(activity, finalCode)
                            SystemUtil.changeLang(finalCode, activity)
                            if (!previousCode.equals(finalCode, ignoreCase = true)) {
                                val mainIntent = Intent(activity, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                activity.startActivity(mainIntent)
                                val settingIntent = Intent(activity, com.video.downloader.downloadvideo.videodownloader.presentation.settings.SettingActivity::class.java)
                                activity.startActivity(settingIntent)
                            }
                            activity.finish()
                        }
                    }
                ),
                adsLanguageConfig = AdsLanguageConfig(
                    nativeLangHighId = getString(R.string.native_language_high),
                    nativeLangId = getString(R.string.native_language),
                    nativeLangClickHighId = getString(R.string.native_language_high_click),
                    nativeLangClickId = getString(R.string.native_language_click),
                    layoutNative = R.layout.layout_native_media,
                    layoutNativeClick = R.layout.layout_native_media_click
                )
            ),
            obConfig = OBConfig(
                uiOBConfig = UiOBConfig(
                    resFragmentOB1 = R.layout.fragment_intro1,
                    resFragmentOB2 = R.layout.fragment_intro2,
                    resFragmentOB3 = R.layout.fragment_intro3,
                    resFragmentOB4 = R.layout.fragment_intro4,
                    activityCallback = object : OnActivityCallBack {
                        override fun onNextActivity(activity: Activity, inSession2: Boolean) {
                            val savedLang = CSCSPF(activity).language_code_selected
                            if (!savedLang.isNullOrBlank()) {
                                SystemUtil.changeLang(savedLang, activity)
                            }
                            val intent = Intent(activity, com.video.downloader.downloadvideo.videodownloader.presentation.question.QuestionActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            activity.startActivity(intent)
                        }
                    },
                    nextOBActivity = com.video.downloader.downloadvideo.videodownloader.presentation.question.QuestionActivity::class.java
                ),
                adsOBConfig = AdsOBConfig(
                    nativeOB1Id = getString(R.string.native_onboarding_1),
                    nativeOB4Id = getString(R.string.native_onboarding_4),
                    nativeOBFull12Id = getString(R.string.native_onboarding_full_1),
                    nativeOBFull34Id = getString(R.string.native_onboarding_full_2),
                    layoutNativeOB1 = R.layout.layout_native_media,
                    layoutNativeOB4 = R.layout.layout_native_media,
                    layoutNativeFullOB = R.layout.admob_layout_native_full,
                    isLoadNativeOBInLanguage = true
                )
            )
        )

        try {
            val field = CSCApplication::class.java.getDeclaredField("cscOpenAds").apply { isAccessible = true }
            AdsManager.registerOpenAds(field.get(cscLibrary) as? com.cscmobi.libraryads.ads.open_ads.CSCOpenAds)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isAppManagedActivity(activity: Activity): Boolean {
        val className = activity.javaClass.name
        return className.startsWith(packageName) ||
            className.startsWith("com.video.downloader.") ||
            className.startsWith("com.cscmobi.") ||
            className.startsWith("com.cscapp.")
    }

    private fun setupFullScreenLifecycle() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (!isAppManagedActivity(activity)) return
                try {
                    activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                applyFullScreen(activity)

                activity.window?.decorView?.let { decor ->
                    decor.post {
                        applyFullScreen(activity)
                        decor.viewTreeObserver?.addOnWindowFocusChangeListener { hasFocus ->
                            if (hasFocus) {
                                applyFullScreen(activity)
                            }
                        }
                    }
                }
            }

            override fun onActivityStarted(activity: Activity) {
                if (!isAppManagedActivity(activity)) return
                applyFullScreen(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                if (!isAppManagedActivity(activity)) return
                applyFullScreen(activity)
                activity.window?.decorView?.post {
                    applyFullScreen(activity)
                }
            }

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {}

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    companion object {
        fun applyFullScreen(activity: Activity?) {
            if (activity == null || activity.isFinishing || activity.isDestroyed) return
            applyFullScreen(activity.window)
        }

        fun applyFullScreen(window: Window?) {
            if (window == null) return
            try {
                WindowCompat.setDecorFitsSystemWindows(window, false)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    window.attributes.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }

                window.statusBarColor = Color.TRANSPARENT
                window.navigationBarColor = Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                    window.isStatusBarContrastEnforced = false
                }

                val decorView = window.decorView
                val insetsController = WindowCompat.getInsetsController(window, decorView)
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
