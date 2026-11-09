package com.video.downloader.downloadvideo.videodownloader.presentation.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.ads.AdsManager
import com.video.downloader.downloadvideo.videodownloader.core.ads.RemoteConfigs
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseFragment
import com.video.downloader.downloadvideo.videodownloader.core.utils.OrganicGuard
import com.video.downloader.downloadvideo.videodownloader.core.utils.showSafely
import com.video.downloader.downloadvideo.videodownloader.databinding.FragmentHomeBinding
import com.video.downloader.downloadvideo.videodownloader.presentation.browser.URLDownloaderActivity
import com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog.YoutubeNotSupportedBottomSheet
import com.video.downloader.downloadvideo.videodownloader.presentation.downloaddetail.DownloadDetailActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    private val viewModel: HomeViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHomeBinding {
        return FragmentHomeBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        loadNativeHomeAd()

        // Feature buttons on Home trigger inter_home before navigating
        binding.layoutPasteInput.setOnClickListener {
            showInterHomeThen { ctx -> openUrlDownloader("", ctx) }
        }

        binding.btnPasteOrGo.setOnClickListener {
            showInterHomeThen { ctx -> openUrlDownloader("", ctx) }
        }

        binding.btnSiteFacebook.setOnClickListener {
            showInterHomeThen { ctx -> openDownloadDetail("Facebook", ctx) }
        }

        binding.btnSitePinterest.setOnClickListener {
            showInterHomeThen { ctx -> openDownloadDetail("Pinterest", ctx) }
        }

        binding.btnSiteInstagram.setOnClickListener {
            showInterHomeThen { ctx -> openDownloadDetail("Instagram", ctx) }
        }

        binding.btnSiteTiktok.setOnClickListener {
            showInterHomeThen { ctx -> openDownloadDetail("TikTok", ctx) }
        }

        binding.btnSiteTwitter.setOnClickListener {
            showInterHomeThen { ctx -> openDownloadDetail("Twitter", ctx) }
        }
    }

    private fun loadNativeHomeAd() {
        val act = activity ?: return
        val b = bindingOrNull ?: return
        AdsManager.loadAndShowNative(
            activity = act,
            container = b.flNativeHome,
            placementKey = "native_home",
            adUnitId = getString(R.string.native_home),
            isEnabled = RemoteConfigs.native_home,
            layoutRes = R.layout.layout_native_small_home
        )
    }

    private fun showInterHomeThen(action: (Context) -> Unit) {
        val act = activity ?: run {
            context?.let { action(it) }
            return
        }
        AdsManager.showInterHome(act) {
            val safeCtx = context ?: act.takeIf { !it.isFinishing && !it.isDestroyed } ?: return@showInterHome
            action(safeCtx)
        }
    }

    private fun openDownloadDetail(platform: String, ctx: Context? = context) {
        val safeContext = ctx ?: context ?: activity ?: return
        val intent = Intent(safeContext, DownloadDetailActivity::class.java).apply {
            putExtra(DownloadDetailActivity.EXTRA_PLATFORM, platform)
        }
        safeContext.startActivity(intent)
    }

    private fun openUrlDownloader(url: String, ctx: Context? = context) {
        val safeContext = ctx ?: context ?: activity ?: return
        if (url.isNotEmpty() && OrganicGuard.isSensitiveContent(url, safeContext)) {
            Toast.makeText(safeContext, R.string.restricted_website_content, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(safeContext, URLDownloaderActivity::class.java).apply {
            putExtra(URLDownloaderActivity.EXTRA_URL, url)
        }
        safeContext.startActivity(intent)
    }

    override fun observeData() {
        collectLifecycleFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is HomeEffect.NavigateToBrowser -> {
                    openUrlDownloader(effect.url)
                }
                is HomeEffect.ShowYoutubeBlockedDialog -> {
                    if (isAdded) {
                        val sheet = YoutubeNotSupportedBottomSheet()
                        sheet.showSafely(childFragmentManager, "YoutubeNotSupported")
                    }
                }
                is HomeEffect.ShowToast -> {
                    context?.let {
                        Toast.makeText(it, effect.messageResId, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
