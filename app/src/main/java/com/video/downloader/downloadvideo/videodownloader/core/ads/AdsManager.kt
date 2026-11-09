package com.video.downloader.downloadvideo.videodownloader.core.ads

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.cscmobi.libraryads.ads.banner_ads.CSCBanner
import com.cscmobi.libraryads.ads.inter_ads.CSCInter
import com.cscmobi.libraryads.ads.native_ads.CSCNativeManager
import com.cscmobi.libraryads.ads.native_ads.callback.INativeAdCallback
import com.cscmobi.libraryads.ads.open_ads.CSCOpenAds
import com.video.downloader.downloadvideo.videodownloader.R

object AdsManager {
    private const val TAG = "AdsManager"

    private var openAdsInstance: CSCOpenAds? = null

    fun registerOpenAds(openAds: CSCOpenAds?) {
        this.openAdsInstance = openAds
    }

    fun disableAOA() {
        try {
            openAdsInstance?.disableShowAOA()
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling AOA", e)
        }
    }

    fun enableAOA() {
        try {
            openAdsInstance?.enableShowAOA()
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling AOA", e)
        }
    }

    /**
     * Show Banner in MainActivity or any screen.
     * If isCollapsible is false, requests standard BANNER_ADAPTIVE.
     * If isCollapsible is true, requests BANNER_COLLAPSIBLE_BOTTOM.
     */
    fun showBanner(
        activity: Activity,
        container: ViewGroup?,
        adUnitId: String = activity.getString(R.string.banner_collap_home),
        isEnabled: Boolean = RemoteConfigs.banner_collap_home,
        isCollapsible: Boolean = true,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (container == null) return
        if (!isEnabled || adUnitId.isBlank()) {
            container.visibility = View.GONE
            onFailed?.invoke()
            return
        }

        try {
            Log.d(TAG, "showBanner: unitId=$adUnitId, isEnabled=$isEnabled, isCollapsible=$isCollapsible")
            val frameLayout = if (container is FrameLayout) {
                container
            } else {
                val existing = container.findViewById<FrameLayout>(R.id.flBannerContainer)
                if (existing != null) {
                    existing
                } else {
                    val fl = FrameLayout(activity).apply { id = R.id.flBannerContainer }
                    container.removeAllViews()
                    container.addView(fl, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    fl
                }
            }
            container.visibility = View.VISIBLE
            val bannerType = if (isCollapsible) {
                CSCBanner.TypeAds.BANNER_COLLAPSIBLE_BOTTOM
            } else {
                CSCBanner.TypeAds.BANNER_ADAPTIVE
            }
            CSCBanner.requestBanner(
                activity = activity,
                id = adUnitId,
                typeAds = bannerType,
                adFrame = frameLayout,
                canShowAd = isEnabled,
                onResult = { adView ->
                    activity.runOnUiThread {
                        if (adView != null) {
                            Log.d(TAG, "Banner loaded successfully")
                            container.visibility = View.VISIBLE
                            onLoaded?.invoke()
                        } else {
                            Log.e(TAG, "Banner failed to load (adView is null)")
                            container.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }
                },
                onShown = {
                    Log.d(TAG, "Banner impression recorded")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting banner", e)
            container.visibility = View.GONE
            onFailed?.invoke()
        }
    }

    /**
     * Show Collapsible Bottom Banner in MainActivity or any screen
     */
    fun showCollapsibleBanner(
        activity: Activity,
        container: ViewGroup?,
        adUnitId: String = activity.getString(R.string.banner_collap_home),
        isEnabled: Boolean = RemoteConfigs.banner_collap_home,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        showBanner(
            activity = activity,
            container = container,
            adUnitId = adUnitId,
            isEnabled = isEnabled,
            isCollapsible = true,
            onLoaded = onLoaded,
            onFailed = onFailed
        )
    }

    fun destroyBanner(activity: Activity) {
        try {
            CSCBanner.destroyBannerAds(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying banner ad", e)
        }
    }

    /**
     * Load & Show Native Ad inside a container (FrameLayout or ViewGroup)
     */
    fun loadAndShowNative(
        activity: Activity,
        container: ViewGroup?,
        placementKey: String,
        adUnitId: String,
        layoutRes: Int = R.layout.layout_native_media,
        isEnabled: Boolean = true,
        isShowLoading: Boolean = true,
        onLoaded: (() -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (container == null) return
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            container.visibility = View.GONE
            onFailed?.invoke()
            return
        }

        try {
            val frameLayout: FrameLayout = if (container is FrameLayout) {
                container
            } else {
                val existing = container.findViewById<FrameLayout>(R.id.flNativeContainer)
                if (existing != null) {
                    existing
                } else {
                    val fl = FrameLayout(activity).apply { id = R.id.flNativeContainer }
                    container.removeAllViews()
                    container.addView(fl, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    fl
                }
            }

            val effectiveKey = if (placementKey.isNotBlank()) {
                if (placementKey.contains(activity.javaClass.simpleName)) placementKey else "${placementKey}_${activity.javaClass.simpleName}"
            } else {
                "native_${activity.javaClass.simpleName}"
            }

            Log.d(TAG, "loadAndShowNative: key=$effectiveKey, unitId=$adUnitId, isEnabled=$isEnabled")
            container.visibility = View.VISIBLE
            CSCNativeManager.loadAndShowNative(
                adFrame = frameLayout,
                adName = effectiveKey,
                adId = adUnitId,
                adLayout = layoutRes,
                isPreload = false,
                canShowAd = isEnabled,
                callback = object : INativeAdCallback {
                    override fun onAdLoaded(adName: String) {
                        Log.d(TAG, "Native onAdLoaded: $adName")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.VISIBLE
                            onLoaded?.invoke()
                        }
                    }

                    override fun onAdFailedToLoad(adName: String, errorMessage: String) {
                        Log.e(TAG, "Native onAdFailedToLoad: $adName, error=$errorMessage")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }

                    override fun onAdShowFailed(adName: String, reason: String) {
                        Log.e(TAG, "Native onAdShowFailed: $adName, reason=$reason")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.GONE
                            onFailed?.invoke()
                        }
                    }

                    override fun onAdDismissed(adName: String) {
                        Log.d(TAG, "Native onAdDismissed: $adName")
                        activity.runOnUiThread {
                            if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                            container.visibility = View.GONE
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading native ad: $placementKey", e)
            container.visibility = View.GONE
            onFailed?.invoke()
        }
    }

    /**
     * Show Interstitial Ad with safety fallback so navigation never hangs
     */
    fun showInter(
        activity: Activity,
        adUnitId: String,
        isEnabled: Boolean = true,
        onNext: () -> Unit
    ) {
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            onNext()
            return
        }

        var executed = false
        val safeNext: () -> Unit = {
            if (!executed) {
                executed = true
                try {
                    com.cscmobi.libraryads.ads.utils.StatusShowAd.isInterstitialShown = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error resetting inter status", e)
                }
                activity.runOnUiThread { onNext() }
            }
        }

        try {
            CSCInter.showInter(
                activity = activity,
                adName = adUnitId,
                nextAction = { _: Boolean -> safeNext() },
                onShown = null,
                preload = true,
                canShowAd = isEnabled
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error showing inter ad", e)
            safeNext()
        }
    }

    /**
     * Load & Show Interstitial Ad with timeout
     */
    fun loadAndShowInter(
        activity: Activity,
        adUnitId: String,
        isEnabled: Boolean = true,
        timeout: Long = 6000L,
        onNext: () -> Unit
    ) {
        if (!isEnabled || adUnitId.isBlank() || activity.isFinishing || activity.isDestroyed) {
            onNext()
            return
        }

        var executed = false
        val safeNext: () -> Unit = {
            if (!executed) {
                executed = true
                com.video.downloader.downloadvideo.videodownloader.presentation.dialogs.AdLoadingDialog.dismiss()
                try {
                    com.cscmobi.libraryads.ads.utils.StatusShowAd.isInterstitialShown = false
                } catch (e: Exception) {
                    Log.e(TAG, "Error resetting inter status", e)
                }
                activity.runOnUiThread { onNext() }
            }
        }

        try {
            com.video.downloader.downloadvideo.videodownloader.presentation.dialogs.AdLoadingDialog.show(activity)

            CSCInter.loadAndShowInter(
                activity = activity,
                adId = adUnitId,
                timeDelay = 0L,
                timeOut = timeout,
                nextAction = { _: Boolean ->
                    com.video.downloader.downloadvideo.videodownloader.presentation.dialogs.AdLoadingDialog.dismiss()
                    safeNext()
                },
                onShown = {
                    com.video.downloader.downloadvideo.videodownloader.presentation.dialogs.AdLoadingDialog.dismiss()
                },
                canShowId = isEnabled,
                onLoadFailed = {
                    com.video.downloader.downloadvideo.videodownloader.presentation.dialogs.AdLoadingDialog.dismiss()
                    safeNext()
                },
                showLoading = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loadAndShowInter", e)
            com.video.downloader.downloadvideo.videodownloader.presentation.dialogs.AdLoadingDialog.dismiss()
            safeNext()
        }
    }

    fun showInterHome(activity: Activity, onNext: () -> Unit) {
        loadAndShowInter(
            activity = activity,
            adUnitId = activity.getString(R.string.inter_home),
            isEnabled = RemoteConfigs.inter_home,
            timeout = 5000L,
            onNext = onNext
        )
    }

    fun showInterPlay(activity: Activity, onNext: () -> Unit) {
        loadAndShowInter(
            activity = activity,
            adUnitId = activity.getString(R.string.inter_play),
            isEnabled = RemoteConfigs.inter_play,
            timeout = 5000L,
            onNext = onNext
        )
    }
}
