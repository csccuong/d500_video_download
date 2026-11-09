package com.video.downloader.downloadvideo.videodownloader.presentation.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseActivity
import com.video.downloader.downloadvideo.videodownloader.core.utils.AppPreferences
import com.video.downloader.downloadvideo.videodownloader.core.utils.DetectedVideo
import com.video.downloader.downloadvideo.videodownloader.core.utils.OrganicGuard
import com.video.downloader.downloadvideo.videodownloader.core.utils.SystemUtil
import com.video.downloader.downloadvideo.videodownloader.core.utils.VideoSniffer
import com.video.downloader.downloadvideo.videodownloader.core.utils.YoutubeGuard
import com.video.downloader.downloadvideo.videodownloader.core.utils.showSafely
import com.video.downloader.downloadvideo.videodownloader.data.service.DownloadForegroundService
import com.video.downloader.downloadvideo.videodownloader.databinding.ActivityUrlDownloaderBinding
import com.video.downloader.downloadvideo.videodownloader.domain.usecase.ExtractVideoUseCase
import com.video.downloader.downloadvideo.videodownloader.domain.usecase.StartDownloadUseCase
import com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog.VideoFormatsBottomSheet
import com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog.YoutubeNotSupportedBottomSheet
import com.video.downloader.downloadvideo.videodownloader.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.URLEncoder
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class URLDownloaderActivity : BaseActivity<ActivityUrlDownloaderBinding>() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TYPE = "extra_type"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36"
    }

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var appPreferences: AppPreferences

    @Inject
    lateinit var extractVideoUseCase: ExtractVideoUseCase

    @Inject
    lateinit var startDownloadUseCase: StartDownloadUseCase

    private val detectedVideos = Collections.synchronizedList(mutableListOf<DetectedVideo>())
    private val detectedUrls = Collections.synchronizedSet(mutableSetOf<String>())
    private var currentTitle: String = ""
    private var currentPageUrl: String = ""
    private var downloadType: String = "webview"

    override fun applyOverrideConfiguration(overrideConfiguration: android.content.res.Configuration?) {
        // Avoid stripping Chromium SystemWebView dynamic APK resources on Android 5.0-10
        // (fixes org.chromium.content.browser.input.SelectPopup.show Resources$NotFoundException).
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            return
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityUrlDownloaderBinding {
        return ActivityUrlDownloaderBinding.inflate(inflater)
    }

    override fun initViews() {
        // Re-apply app locale after WebView inflation so WebView initialization on Android 7-10
        // never resets Activity strings back to the device default locale.
        SystemUtil.setLocale(this)
        binding.etBrowserSearch.hint = getString(R.string.enter_url_here)

        downloadType = intent.getStringExtra(EXTRA_TYPE) ?: "webview"
        val passedUrl = intent.getStringExtra(EXTRA_URL)?.trim().orEmpty()

        setupAddressBar()
        setupWebView()
        setupFab()
        setupBackNavigation()

        // Determine starting URL
        var initialUrl = passedUrl
        if (initialUrl.isEmpty()) {
            initialUrl = appPreferences.lastSearchedUrl.trim()
        }

        if (initialUrl.isNotEmpty()) {
            if (YoutubeGuard.isYoutubeUrl(initialUrl)) {
                showYoutubeBlockedDialog()
            } else if (OrganicGuard.isSensitiveContent(initialUrl, this)) {
                Toast.makeText(this, R.string.restricted_website_content, Toast.LENGTH_SHORT).show()
                binding.etBrowserSearch.setText("")
                appPreferences.clearLastSearchedUrl()
            } else {
                loadFormattedUrl(initialUrl)
            }
        } else {
            // Request focus if empty
            binding.etBrowserSearch.requestFocus()
            showKeyboard(binding.etBrowserSearch)
        }
    }

    private fun setupAddressBar() {
        binding.btnBrowserBack.setOnClickListener {
            handleBackAction()
        }

        binding.etBrowserSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnBrowserClear.visibility =
                    if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnBrowserClear.setOnClickListener {
            binding.etBrowserSearch.setText("")
            appPreferences.clearLastSearchedUrl()
            clearDetectedVideos()
            binding.etBrowserSearch.requestFocus()
            showKeyboard(binding.etBrowserSearch)
        }

        binding.etBrowserSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                val text = binding.etBrowserSearch.text?.toString().orEmpty()
                submitSearch(text)
                true
            } else {
                false
            }
        }

        binding.btnBrowserSubmit.setOnClickListener {
            val text = binding.etBrowserSearch.text?.toString().orEmpty()
            submitSearch(text)
        }
    }

    private fun submitSearch(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        if (YoutubeGuard.isYoutubeUrl(trimmed)) {
            showYoutubeBlockedDialog()
            return
        }

        if (OrganicGuard.isSensitiveContent(trimmed, this)) {
            Toast.makeText(this, R.string.restricted_website_content, Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        clearDetectedVideos()
        loadFormattedUrl(trimmed)
    }

    private fun loadFormattedUrl(input: String) {
        val trimmed = input.trim()
        val finalUrl = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> {
                trimmed
            }
            trimmed.contains(".") && !trimmed.contains(" ") -> {
                "https://$trimmed"
            }
            else -> {
                val encoded = try {
                    URLEncoder.encode(trimmed, "UTF-8")
                } catch (_: Exception) {
                    trimmed
                }
                "https://www.google.com/search?q=$encoded"
            }
        }

        currentPageUrl = finalUrl
        binding.etBrowserSearch.setText(finalUrl)
        appPreferences.lastSearchedUrl = finalUrl
        binding.webViewDownloader.loadUrl(finalUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webViewDownloader.apply {
            setBackgroundColor(Color.parseColor("#161625"))

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = USER_AGENT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = false
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    if (isFinishing || isDestroyed) return
                    if (newProgress < 100) {
                        binding.pbBrowserLoading.visibility = View.VISIBLE
                        binding.pbBrowserLoading.progress = newProgress
                    } else {
                        binding.pbBrowserLoading.visibility = View.GONE
                    }
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    if (!title.isNullOrEmpty() && !title.contains("about:blank", ignoreCase = true)) {
                        currentTitle = title
                    }
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString().orEmpty()
                    if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                        return true
                    }
                    if (YoutubeGuard.isYoutubeUrl(url)) {
                        showYoutubeBlockedDialog()
                        return true
                    }
                    if (OrganicGuard.isSensitiveContent(url, this@URLDownloaderActivity)) {
                        Toast.makeText(this@URLDownloaderActivity, R.string.restricted_website_content, Toast.LENGTH_SHORT).show()
                        return true
                    }
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (isFinishing || isDestroyed) return
                    if (url != null && !url.contains("about:blank", ignoreCase = true)) {
                        if (checkDomainChanged(currentPageUrl, url)) {
                            clearDetectedVideos()
                        }
                        currentPageUrl = url
                        if (!binding.etBrowserSearch.hasFocus()) {
                            binding.etBrowserSearch.setText(url)
                        }
                        appPreferences.lastSearchedUrl = url
                    }

                    if (url != null && YoutubeGuard.isYoutubeUrl(url)) {
                        view?.stopLoading()
                        showYoutubeBlockedDialog()
                        if (view?.canGoBack() == true) {
                            view.goBack()
                        } else {
                            view?.loadUrl("about:blank")
                        }
                        return
                    }

                    if (url != null && OrganicGuard.isSensitiveContent(url, this@URLDownloaderActivity)) {
                        view?.stopLoading()
                        Toast.makeText(this@URLDownloaderActivity, R.string.restricted_website_content, Toast.LENGTH_SHORT).show()
                        if (view?.canGoBack() == true) {
                            view.goBack()
                        } else {
                            view?.loadUrl("about:blank")
                        }
                        return
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (url != null && !url.contains("about:blank", ignoreCase = true)) {
                        currentPageUrl = url
                        val pageTitle = view?.title.orEmpty()
                        if (pageTitle.isNotEmpty() && !pageTitle.contains("about:blank", ignoreCase = true)) {
                            currentTitle = pageTitle
                        }

                        // Background extraction for social media video pages
                        val isTiktok = url.contains("tiktok.com", ignoreCase = true)
                        val isSupportedSocial = isTiktok ||
                                url.contains("facebook.com", ignoreCase = true) ||
                                url.contains("fb.watch", ignoreCase = true) ||
                                url.contains("instagram.com", ignoreCase = true)
                        if (isSupportedSocial && (url.contains("/video/") || url.contains("/reel/") || url.contains("/p/") || url.contains("/watch") || isTiktok)) {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val result = extractVideoUseCase(url, isTiktok = isTiktok)
                                result.onSuccess { model ->
                                    try {
                                        val defaultTitle = model.title.orEmpty().ifEmpty { currentTitle.ifEmpty { "${downloadType}_Video_${System.currentTimeMillis()}" } }
                                        val sortedFormats = com.video.downloader.downloadvideo.videodownloader.core.utils.FormatHelper.processAndSortFormats(model, defaultTitle)
                                        synchronized(detectedUrls) {
                                            for (detVideo in sortedFormats) {
                                                if (!detectedUrls.contains(detVideo.url)) {
                                                    detectedUrls.add(detVideo.url)
                                                    detectedVideos.add(detVideo)
                                                }
                                            }
                                        }
                                        withContext(Dispatchers.Main) {
                                            updateFabState()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("URLDownloader", "Error parsing formats", e)
                                    }
                                }
                            }
                        }
                    }
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val reqUrl = request?.url?.toString().orEmpty()
                    if (VideoSniffer.isVideoUrl(reqUrl) && !YoutubeGuard.isYoutubeUrl(reqUrl)) {
                        onPotentialVideoDetected(reqUrl)
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if (url != null && VideoSniffer.isVideoUrl(url) && !YoutubeGuard.isYoutubeUrl(url)) {
                        onPotentialVideoDetected(url)
                    }
                }
            }
        }
    }

    private fun checkDomainChanged(oldUrl: String, newUrl: String): Boolean {
        return try {
            val oldHost = Uri.parse(oldUrl).host.orEmpty()
            val newHost = Uri.parse(newUrl).host.orEmpty()
            oldHost.isNotEmpty() && newHost.isNotEmpty() && !oldHost.equals(newHost, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    private fun onPotentialVideoDetected(rawUrl: String) {
        val cleanUrl = VideoSniffer.cleanMediaUrl(rawUrl)
        if (cleanUrl.contains("about:blank")) return

        synchronized(detectedUrls) {
            if (detectedUrls.contains(cleanUrl)) return
            detectedUrls.add(cleanUrl)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val streamType = VideoSniffer.getStreamDescription(cleanUrl, this@URLDownloaderActivity)
            val fallbackTitle = currentTitle.ifEmpty { "${downloadType}_Video_${System.currentTimeMillis()}" }
            val item = DetectedVideo(
                url = cleanUrl,
                title = fallbackTitle,
                streamType = streamType
            )

            // Resolve size in background
            val (sizeBytes, sizeStr) = VideoSniffer.resolveVideoSize(okHttpClient, cleanUrl, currentPageUrl)
            item.sizeBytes = sizeBytes
            item.size = sizeStr

            // Generate thumbnail in background
            val thumb = VideoSniffer.generateThumbnail(cleanUrl)
            item.thumbnail = thumb

            withContext(Dispatchers.Main) {
                detectedVideos.add(item)
                updateFabState()
            }
        }
    }

    private fun setupFab() {
        updateFabState()
        binding.fabDownloadSniffer.setOnClickListener {
            if (isFinishing || isDestroyed) return@setOnClickListener
            if (detectedVideos.isNotEmpty()) {
                val videosSnapshot = synchronized(detectedVideos) { detectedVideos.toList() }
                val sheet = VideoFormatsBottomSheet(videosSnapshot) { selectedVideos ->
                    executeDownload(selectedVideos)
                }
                sheet.showSafely(supportFragmentManager, "VideoFormatsBottomSheet")
            }
        }
    }

    private fun updateFabState() {
        if (isFinishing || isDestroyed) return
        val count = detectedVideos.size
        if (count > 0) {
            binding.fabDownloadSniffer.setBackgroundResource(R.drawable.bg_fab_download)
            binding.ivFabDownloadIcon.setColorFilter(ContextCompat.getColor(this, R.color.white))
            binding.fabDownloadSniffer.isClickable = true
        } else {
            binding.fabDownloadSniffer.setBackgroundResource(R.drawable.bg_fab_download_disabled)
            binding.ivFabDownloadIcon.setColorFilter(ContextCompat.getColor(this, R.color.white))
            binding.fabDownloadSniffer.isClickable = false
        }
    }

    private fun clearDetectedVideos() {
        synchronized(detectedUrls) { detectedUrls.clear() }
        synchronized(detectedVideos) { detectedVideos.clear() }
        updateFabState()
    }

    private fun executeDownload(selectedVideos: List<DetectedVideo>) {
        if (selectedVideos.isEmpty()) return

        Toast.makeText(this, R.string.download_started, Toast.LENGTH_SHORT).show()

        val headers = hashMapOf(
            "User-Agent" to USER_AGENT
        )
        if (currentPageUrl.isNotEmpty()) {
            headers["Referer"] = currentPageUrl
            try {
                val cookie = android.webkit.CookieManager.getInstance().getCookie(currentPageUrl)
                if (!cookie.isNullOrEmpty()) {
                    headers["Cookie"] = cookie
                }
            } catch (_: Exception) {}
        }

        lifecycleScope.launch {
            for (video in selectedVideos) {
                val downloadId = startDownloadUseCase(
                    url = video.url,
                    title = video.title,
                    fileSize = video.sizeBytes,
                    thumbnailUrl = video.thumbnailUrl
                )
                DownloadForegroundService.startDownload(
                    context = this@URLDownloaderActivity,
                    downloadId = downloadId,
                    url = video.url,
                    title = video.title,
                    headers = headers
                )
            }
        }

        // Navigate to Download tab in MainActivity immediately matching Flutter
        val mainIntent = Intent(this@URLDownloaderActivity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_SELECT_TAB, 1)
        }
        startActivity(mainIntent)
        finish()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            handleBackAction()
        }
    }

    private fun handleBackAction() {
        if (binding.webViewDownloader.canGoBack()) {
            binding.webViewDownloader.goBack()
        } else {
            finish()
        }
    }

    private fun showYoutubeBlockedDialog() {
        if (isFinishing || isDestroyed) return
        val sheet = YoutubeNotSupportedBottomSheet()
        sheet.showSafely(supportFragmentManager, "YoutubeBlocked")
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etBrowserSearch.windowToken, 0)
        binding.etBrowserSearch.clearFocus()
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroy() {
        // Safe WebView cleanup to prevent memory leaks
        try {
            binding.webViewDownloader.apply {
                webChromeClient = null
                stopLoading()
                clearHistory()
                loadUrl("about:blank")
                onPause()
                removeAllViews()
                destroy()
            }
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
