package com.video.downloader.downloadvideo.videodownloader.presentation.downloaddetail

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.ads.AdsManager
import com.video.downloader.downloadvideo.videodownloader.core.ads.RemoteConfigs
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseActivity
import com.video.downloader.downloadvideo.videodownloader.core.utils.DetectedVideo
import com.video.downloader.downloadvideo.videodownloader.core.utils.MediaStoreHelper
import com.video.downloader.downloadvideo.videodownloader.core.utils.OrganicGuard
import com.video.downloader.downloadvideo.videodownloader.core.utils.VideoSniffer
import com.video.downloader.downloadvideo.videodownloader.core.utils.YoutubeGuard
import com.video.downloader.downloadvideo.videodownloader.core.utils.showSafely
import com.video.downloader.downloadvideo.videodownloader.data.service.DownloadForegroundService
import com.video.downloader.downloadvideo.videodownloader.databinding.ActivityDownloadDetailBinding
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus
import com.video.downloader.downloadvideo.videodownloader.domain.usecase.ExtractVideoUseCase
import com.video.downloader.downloadvideo.videodownloader.domain.usecase.StartDownloadUseCase
import com.video.downloader.downloadvideo.videodownloader.presentation.browser.URLDownloaderActivity
import com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog.VideoFormatsBottomSheet
import com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog.YoutubeNotSupportedBottomSheet
import com.video.downloader.downloadvideo.videodownloader.presentation.dialog.PopupDeleteDialog
import com.video.downloader.downloadvideo.videodownloader.presentation.dialog.PopupRenameDialog
import com.video.downloader.downloadvideo.videodownloader.presentation.history.HistoryAdapter
import com.video.downloader.downloadvideo.videodownloader.presentation.history.HistoryEffect
import com.video.downloader.downloadvideo.videodownloader.presentation.history.HistoryIntent
import com.video.downloader.downloadvideo.videodownloader.presentation.history.HistoryTab
import com.video.downloader.downloadvideo.videodownloader.presentation.history.HistoryViewModel
import com.video.downloader.downloadvideo.videodownloader.presentation.howtouse.HowToUseActivity
import com.video.downloader.downloadvideo.videodownloader.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DownloadDetailActivity : BaseActivity<ActivityDownloadDetailBinding>() {

    companion object {
        const val EXTRA_PLATFORM = "extra_platform"
        const val EXTRA_INITIAL_URL = "extra_initial_url"
    }

    @Inject
    lateinit var startDownloadUseCase: StartDownloadUseCase

    @Inject
    lateinit var extractVideoUseCase: ExtractVideoUseCase

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter
    private var platform: String = "Facebook"
    private var currentTabIndex: Int = 0

    override fun inflateBinding(inflater: LayoutInflater): ActivityDownloadDetailBinding {
        return ActivityDownloadDetailBinding.inflate(inflater)
    }

    override fun initViews() {
        platform = intent.getStringExtra(EXTRA_PLATFORM) ?: "Facebook"
        val displayPlatform = if (platform.equals("twitter", ignoreCase = true)) "X" else platform
        binding.tabPlatform.text = displayPlatform

        binding.btnDetailBack.setOnClickListener { finish() }

        // Setup Guide & Open Browser for all platforms (Facebook, Instagram, TikTok, X, Pinterest, etc.)
        binding.layoutStoryGuide.visibility = View.VISIBLE
        val storyImageRes = when (platform.lowercase()) {
            "facebook" -> R.drawable.social_facebook
            "instagram" -> R.drawable.social_instagram
            "pinterest" -> R.drawable.social_pinterest
            "tiktok" -> R.drawable.social_tiktok
            "twitter", "x" -> R.drawable.social_twiter
            else -> R.drawable.social_facebook
        }
        binding.ivStoryGuide.setImageResource(storyImageRes)
        binding.tvStoryGuide.text = getString(
            R.string.story_guide_format,
            displayPlatform
        )
        binding.btnOpenBrowser.text = "Open $displayPlatform"

        binding.btnDetailHelp.setOnClickListener {
            val intent = Intent(this, HowToUseActivity::class.java).apply {
                putExtra(HowToUseActivity.EXTRA_PLATFORM, platform)
            }
            startActivity(intent)
        }

        binding.btnDetailPaste.setOnClickListener {
            val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clipData = clipboard?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val pasteText = clipData.getItemAt(0).coerceToText(this)?.toString()?.trim().orEmpty()
                if (pasteText.isNotEmpty()) {
                    binding.etDetailUrlInput.setText(pasteText)
                    binding.etDetailUrlInput.setSelection(pasteText.length)
                    Toast.makeText(this, R.string.pasted_from_clipboard, Toast.LENGTH_SHORT).show()
                }
            }
        }


        binding.btnOpenBrowser.setOnClickListener {
            val url = when (platform.lowercase()) {
                "facebook" -> "https://www.facebook.com/reel/"
                "instagram" -> "https://www.instagram.com/reels/"
                "twitter", "x" -> "https://x.com/home"
                "pinterest" -> "https://www.pinterest.com/pin/1075938167239875363/"
                "tiktok" -> "https://www.tiktok.com/foryou?lang=en"
                else -> "https://www.google.com"
            }
            val intent = Intent(this, URLDownloaderActivity::class.java).apply {
                putExtra(URLDownloaderActivity.EXTRA_URL, url)
                putExtra(URLDownloaderActivity.EXTRA_TYPE, platform.lowercase())
            }
            startActivity(intent)
        }

        binding.btnDetailDownload.setOnClickListener {
            val inputUrl = binding.etDetailUrlInput.text?.toString()?.trim().orEmpty()
            if (inputUrl == "test") {
                val dummyVideos = listOf(
                    DetectedVideo(url = "https://example.com/1.mp4", title = "Facebook_Video_1080p", size = "18.5 MB", streamType = "MP4 Stream | 1080p"),
                    DetectedVideo(url = "https://example.com/2.mp4", title = "Facebook_Video_720p", size = "10.2 MB", streamType = "MP4 Stream | 720p"),
                    DetectedVideo(url = "https://example.com/3.mp3", title = "Facebook_Audio_MP3", size = "3.4 MB", streamType = "MP3 Audio | 128kbps")
                )
                VideoFormatsBottomSheet(dummyVideos) {}.showSafely(supportFragmentManager, "TestSheet")
                return@setOnClickListener
            }
            if (inputUrl.isEmpty()) {
                Toast.makeText(this, R.string.please_enter_url, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (YoutubeGuard.isYoutubeUrl(inputUrl)) {
                YoutubeNotSupportedBottomSheet().showSafely(supportFragmentManager, "YoutubeBlocked")
                return@setOnClickListener
            }

            if (OrganicGuard.isSensitiveContent(inputUrl, this)) {
                Toast.makeText(this, R.string.restricted_website_content, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loadingDialog = com.video.downloader.downloadvideo.videodownloader.presentation.dialog.LoadingDialog(this).apply {
                setMessage(getString(R.string.download_starting))
                show()
            }

            lifecycleScope.launch {
                val isTiktok = platform.equals("tiktok", ignoreCase = true) || inputUrl.contains("tiktok.com", ignoreCase = true)
                val result = extractVideoUseCase(inputUrl, isTiktok = isTiktok)
                try {
                    loadingDialog.dismiss()
                } catch (_: Exception) {}

                result.onSuccess { downloadModel ->
                    if (!downloadModel.formats.isNullOrEmpty()) {
                        val defaultTitle = downloadModel.title.orEmpty().ifEmpty { "${platform}_Video_${System.currentTimeMillis()}" }
                        val detectedList = com.video.downloader.downloadvideo.videodownloader.core.utils.FormatHelper.processAndSortFormats(
                            downloadModel,
                            defaultTitle
                        )

                        val sheet = VideoFormatsBottomSheet(detectedList) { selectedVideos ->
                            val headers = hashMapOf(
                                "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
                            )
                            when (platform.lowercase()) {
                                "facebook" -> headers["Referer"] = "https://www.facebook.com/"
                                "instagram" -> headers["Referer"] = "https://www.instagram.com/"
                                "tiktok" -> headers["Referer"] = "https://www.tiktok.com/"
                                "twitter", "x" -> headers["Referer"] = "https://x.com/"
                                "pinterest" -> headers["Referer"] = "https://www.pinterest.com/"
                            }

                            for (video in selectedVideos) {
                                lifecycleScope.launch {
                                    val downloadId = startDownloadUseCase(
                                        url = video.url,
                                        title = video.title,
                                        fileSize = video.sizeBytes,
                                        thumbnailUrl = downloadModel.thumbnail
                                    )
                                    DownloadForegroundService.startDownload(
                                        this@DownloadDetailActivity,
                                        downloadId,
                                        video.url,
                                        video.title,
                                        headers = headers
                                    )
                                }
                            }
                            binding.etDetailUrlInput.setText("")
                            Toast.makeText(this@DownloadDetailActivity, R.string.download_started, Toast.LENGTH_SHORT).show()
                            selectTab(1)
                        }
                        if (!isFinishing && !isDestroyed) {
                            sheet.showSafely(supportFragmentManager, "VideoFormatsBottomSheet")
                        }
                    } else {
                        fallbackToBrowser(inputUrl)
                    }
                }.onFailure {
                    // Fallback to URLDownloaderActivity with inputUrl so user can download via WebView sniffer
                    fallbackToBrowser(inputUrl)
                }
            }
        }

        // Setup History list
        adapter = HistoryAdapter(
            onItemClick = { item ->
                if (item.status == DownloadStatus.COMPLETED) {
                    AdsManager.showInterPlay(this) {
                        viewModel.sendIntent(HistoryIntent.PlayItem(item))
                    }
                }
            },
            onMenuClick = { item, anchorView ->
                showItemMenu(item, anchorView)
            }
        )

        binding.rvDetailDownloads.layoutManager = LinearLayoutManager(this)
        binding.rvDetailDownloads.adapter = adapter

        // Setup Tabs
        binding.tabPlatform.setOnClickListener { selectTab(0) }
        binding.tabProgress.setOnClickListener { selectTab(1) }
        binding.tabDownloaded.setOnClickListener { selectTab(2) }

        selectTab(0)

        val initialUrl = intent.getStringExtra(EXTRA_INITIAL_URL).orEmpty()
        if (initialUrl.isNotEmpty()) {
            binding.etDetailUrlInput.setText(initialUrl)
            binding.btnDetailDownload.performClick()
        }
    }

    override fun observeData() {
        collectLifecycleFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is HistoryEffect.OpenWith -> {
                    openMyFile(effect.contentUri)
                }
                is HistoryEffect.ShareItem -> {
                    try {
                        val shareUri = MediaStoreHelper.resolveShareableUri(this, effect.contentUri)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/*"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
                    } catch (_: Exception) {
                        Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show()
                    }
                }
                is HistoryEffect.ShowToast -> {
                    Toast.makeText(this, effect.messageResId, Toast.LENGTH_SHORT).show()
                }
            }
        }

        collectLifecycleFlow(viewModel.uiState) { state ->
            adapter.submitList(state.downloads)
            if (state.downloads.isEmpty()) {
                binding.layoutDetailEmpty.visibility = View.VISIBLE
                binding.rvDetailDownloads.visibility = View.GONE
            } else {
                binding.layoutDetailEmpty.visibility = View.GONE
                binding.rvDetailDownloads.visibility = View.VISIBLE
            }
        }

        collectLifecycleFlow(com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEventBus.events) { event ->
            when (event) {
                is com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEvent.Completed -> {
                    try {
                        com.video.downloader.downloadvideo.videodownloader.presentation.dialog.PopupDownloadSuccessDialog(this, event.title).show()
                    } catch (_: Exception) {}
                    selectTab(2)
                }
                is com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEvent.Failed -> {
                    Toast.makeText(this, getString(R.string.download_failed, event.error), Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private fun selectTab(index: Int) {
        currentTabIndex = index
        when (index) {
            0 -> {
                binding.tabPlatform.setBackgroundResource(R.drawable.bg_segmented_tab_selected)
                binding.tabPlatform.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tabProgress.setBackgroundResource(R.color.transparent)
                binding.tabProgress.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tabDownloaded.setBackgroundResource(R.color.transparent)
                binding.tabDownloaded.setTextColor(ContextCompat.getColor(this, R.color.white))

                binding.layoutPlatformTabContent.visibility = View.VISIBLE
                binding.layoutListTabContent.visibility = View.GONE
            }
            1 -> {
                adapter.currentTab = HistoryTab.PROGRESS
                binding.tabProgress.setBackgroundResource(R.drawable.bg_segmented_tab_selected)
                binding.tabProgress.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tabPlatform.setBackgroundResource(R.color.transparent)
                binding.tabPlatform.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tabDownloaded.setBackgroundResource(R.color.transparent)
                binding.tabDownloaded.setTextColor(ContextCompat.getColor(this, R.color.white))

                binding.tvDetailEmptyMessage.setText(R.string.no_video_in_downloading)
                binding.layoutPlatformTabContent.visibility = View.GONE
                binding.layoutListTabContent.visibility = View.VISIBLE
                loadNativeDetailDownloadAd()
                viewModel.sendIntent(HistoryIntent.ChangeFilter(DownloadStatus.DOWNLOADING))
            }
            2 -> {
                adapter.currentTab = HistoryTab.DOWNLOADED
                binding.tabDownloaded.setBackgroundResource(R.drawable.bg_segmented_tab_selected)
                binding.tabDownloaded.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tabPlatform.setBackgroundResource(R.color.transparent)
                binding.tabPlatform.setTextColor(ContextCompat.getColor(this, R.color.white))
                binding.tabProgress.setBackgroundResource(R.color.transparent)
                binding.tabProgress.setTextColor(ContextCompat.getColor(this, R.color.white))

                binding.tvDetailEmptyMessage.setText(R.string.no_data_available)
                binding.layoutPlatformTabContent.visibility = View.GONE
                binding.layoutListTabContent.visibility = View.VISIBLE
                loadNativeDetailDownloadAd()
                viewModel.sendIntent(HistoryIntent.ChangeFilter(DownloadStatus.COMPLETED))
            }
        }
    }

    private fun showItemMenu(item: DownloadItem, anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menu.add(0, 1, 0, R.string.open_with).isEnabled = (item.status == DownloadStatus.COMPLETED)
        popup.menu.add(0, 2, 1, R.string.btn_share).isEnabled = (item.status == DownloadStatus.COMPLETED)
        popup.menu.add(0, 3, 2, R.string.rename).isEnabled = (item.status == DownloadStatus.COMPLETED)
        popup.menu.add(0, 4, 3, R.string.btn_delete)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> item.contentUri?.let { uri -> viewModel.sendIntent(HistoryIntent.OpenWithItem(uri)) }
                2 -> item.contentUri?.let { uri -> viewModel.sendIntent(HistoryIntent.ShareItem(uri)) }
                3 -> {
                    if (!isFinishing && !isDestroyed) {
                        val dialog = PopupRenameDialog(item.title) { newName ->
                            viewModel.sendIntent(HistoryIntent.RenameItem(item, newName))
                        }
                        dialog.showSafely(supportFragmentManager, "RenameDialog")
                    }
                }
                4 -> {
                    if (!isFinishing && !isDestroyed) {
                        val dialog = PopupDeleteDialog {
                            viewModel.sendIntent(HistoryIntent.DeleteItem(item.id))
                        }
                        dialog.showSafely(supportFragmentManager, "DeleteDialog")
                    }
                }
            }
            true
        }
        popup.show()
    }

    private fun fallbackToBrowser(url: String) {
        val intent = Intent(this, URLDownloaderActivity::class.java).apply {
            putExtra(URLDownloaderActivity.EXTRA_URL, url)
            putExtra(URLDownloaderActivity.EXTRA_TYPE, platform.lowercase())
        }
        startActivity(intent)
    }

    private fun openMyFile(contentUriString: String) {
        try {
            val uri = MediaStoreHelper.resolveShareableUri(this, contentUriString)

            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(openIntent)
        } catch (_: Exception) {
            try {
                val uri = MediaStoreHelper.resolveShareableUri(this, contentUriString)
                val chooserIntent = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    getString(R.string.open_with)
                )
                startActivity(chooserIntent)
            } catch (_: Exception) {
                Toast.makeText(this, getString(R.string.file_not_found), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNativeDetailDownloadAd() {
        AdsManager.loadAndShowNative(
            activity = this,
            container = binding.flNativeDetailDownload,
            placementKey = "native_download",
            adUnitId = getString(R.string.native_download),
            isEnabled = RemoteConfigs.native_download,
            layoutRes = R.layout.layout_native_small
        )
    }
}
