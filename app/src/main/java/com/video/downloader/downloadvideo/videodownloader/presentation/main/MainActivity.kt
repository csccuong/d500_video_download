package com.video.downloader.downloadvideo.videodownloader.presentation.main

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.activity.addCallback
import androidx.core.widget.ImageViewCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.ads.AdsManager
import com.video.downloader.downloadvideo.videodownloader.core.ads.RemoteConfigs
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseActivity
import com.video.downloader.downloadvideo.videodownloader.core.utils.showSafely
import com.video.downloader.downloadvideo.videodownloader.databinding.ActivityMainBinding
import com.video.downloader.downloadvideo.videodownloader.presentation.dialog.PopupConfirmQuitDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    companion object {
        const val EXTRA_SELECT_TAB = "extra_select_tab"
        private const val BANNER_RELOAD_INTERVAL_MS = 15_000L
        private val COLOR_NAV_SELECTED = Color.parseColor("#8359FF")
        private val COLOR_NAV_UNSELECTED = Color.parseColor("#9FA8BC")
    }

    private lateinit var navController: NavController

    // Handler for 15s collapsible banner auto-reload
    private val bannerReloadHandler = Handler(Looper.getMainLooper())
    private val bannerReloadRunnable = object : Runnable {
        override fun run() {
            loadCollapsibleBanner()
            bannerReloadHandler.postDelayed(this, BANNER_RELOAD_INTERVAL_MS)
        }
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(inflater)
    }

    override fun initViews() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        setupCustomBottomNav()

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home -> {
                    binding.layoutMainHeader.visibility = View.GONE
                    binding.layoutCustomBottomNav.visibility = View.VISIBLE
                    updateBottomNavUI(R.id.navigation_home)
                }
                R.id.navigation_history -> {
                    binding.layoutMainHeader.visibility = View.VISIBLE
                    binding.layoutHomeTitle.visibility = View.GONE
                    binding.tvDownloadTitle.visibility = View.VISIBLE
                    binding.tvSettingTitle.visibility = View.GONE
                    binding.layoutCustomBottomNav.visibility = View.VISIBLE
                    updateBottomNavUI(R.id.navigation_history)
                }
                R.id.navigation_setting -> {
                    binding.layoutMainHeader.visibility = View.VISIBLE
                    binding.layoutHomeTitle.visibility = View.GONE
                    binding.tvDownloadTitle.visibility = View.GONE
                    binding.tvSettingTitle.visibility = View.VISIBLE
                    binding.layoutCustomBottomNav.visibility = View.VISIBLE
                    updateBottomNavUI(R.id.navigation_setting)
                }
                else -> {
                    binding.layoutMainHeader.visibility = View.GONE
                    binding.layoutCustomBottomNav.visibility = View.GONE
                }
            }
        }

        handleIntent(intent)

        onBackPressedDispatcher.addCallback(this) {
            val currentDest = navController.currentDestination?.id
            if (currentDest == R.id.navigation_home || currentDest == null) {
                showConfirmQuitDialog()
            } else {
                if (!navController.popBackStack()) {
                    showConfirmQuitDialog()
                }
            }
        }
    }

    private fun loadCollapsibleBanner() {
        if (isFinishing || isDestroyed) return
        val isEnabled = RemoteConfigs.banner_collap_home
        if (!isEnabled) {
            binding.flBannerContainer.visibility = View.GONE
            return
        }
        AdsManager.showCollapsibleBanner(
            activity = this,
            container = binding.flBannerContainer,
            adUnitId = getString(R.string.banner_collap_home),
            isEnabled = isEnabled,
            onLoaded = {
                if (!isFinishing && !isDestroyed) {
                    binding.flBannerContainer.visibility = View.VISIBLE
                }
            },
            onFailed = {
                if (!isFinishing && !isDestroyed) {
                    binding.flBannerContainer.visibility = View.GONE
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        val isEnabled = RemoteConfigs.banner_collap_home
        if (isEnabled) {
            bannerReloadHandler.removeCallbacks(bannerReloadRunnable)
            bannerReloadHandler.post(bannerReloadRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        bannerReloadHandler.removeCallbacks(bannerReloadRunnable)
    }

    override fun onDestroy() {
        bannerReloadHandler.removeCallbacks(bannerReloadRunnable)
        AdsManager.destroyBanner(this)
        super.onDestroy()
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun setupCustomBottomNav() {
        binding.btnNavHome.setOnClickListener {
            val currentDestId = navController.currentDestination?.id
            if (currentDestId != R.id.navigation_home) {
                navController.navigate(R.id.navigation_home)
            }
        }

        binding.btnNavDownload.setOnClickListener {
            val currentDestId = navController.currentDestination?.id
            if (currentDestId == R.id.navigation_home) {
                // Switching from Home to Download tab -> show inter_home ad
                AdsManager.showInterHome(this) {
                    if (!isFinishing && !isDestroyed) {
                        navController.navigate(R.id.navigation_history)
                    }
                }
            } else if (currentDestId != R.id.navigation_history) {
                navController.navigate(R.id.navigation_history)
            }
        }

        binding.btnNavSetting.setOnClickListener {
            val currentDestId = navController.currentDestination?.id
            if (currentDestId != R.id.navigation_setting) {
                navController.navigate(R.id.navigation_setting)
            }
        }
    }

    private fun updateBottomNavUI(destinationId: Int) {
        val isHome = destinationId == R.id.navigation_home
        binding.ivNavHome.setImageResource(if (isHome) R.drawable.tab_home_selected else R.drawable.tab_home)
        ImageViewCompat.setImageTintList(
            binding.ivNavHome,
            ColorStateList.valueOf(if (isHome) COLOR_NAV_SELECTED else COLOR_NAV_UNSELECTED)
        )
        binding.tvNavHome.setTextColor(if (isHome) COLOR_NAV_SELECTED else COLOR_NAV_UNSELECTED)

        val isDownload = destinationId == R.id.navigation_history
        binding.ivNavDownload.setImageResource(if (isDownload) R.drawable.tab_download_selected else R.drawable.tab_download)
        ImageViewCompat.setImageTintList(
            binding.ivNavDownload,
            ColorStateList.valueOf(if (isDownload) COLOR_NAV_SELECTED else COLOR_NAV_UNSELECTED)
        )
        binding.tvNavDownload.setTextColor(if (isDownload) COLOR_NAV_SELECTED else COLOR_NAV_UNSELECTED)

        val isSetting = destinationId == R.id.navigation_setting
        binding.ivNavSetting.setImageResource(if (isSetting) R.drawable.ic_tab_setting_selected else R.drawable.ic_tab_setting)
        ImageViewCompat.setImageTintList(
            binding.ivNavSetting,
            ColorStateList.valueOf(if (isSetting) COLOR_NAV_SELECTED else COLOR_NAV_UNSELECTED)
        )
        binding.tvNavSetting.setTextColor(if (isSetting) COLOR_NAV_SELECTED else COLOR_NAV_UNSELECTED)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val tab = intent?.getIntExtra(EXTRA_SELECT_TAB, -1) ?: -1
        if (tab == 1) {
            navController.navigate(R.id.navigation_history)
        } else if (tab == 2) {
            navController.navigate(R.id.navigation_setting)
        }
    }

    private fun showConfirmQuitDialog() {
        if (isFinishing || isDestroyed) return
        val dialog = PopupConfirmQuitDialog {
            finish()
        }
        dialog.showSafely(supportFragmentManager, "PopupConfirmQuitDialog")
    }

    fun navigateToBrowser(url: String) {
        val bundle = Bundle().apply {
            putString("target_url", url)
        }
        navController.navigate(R.id.navigation_browser, bundle)
    }
}
