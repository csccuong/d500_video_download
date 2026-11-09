package com.video.downloader.downloadvideo.videodownloader.presentation.browser

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseFragment
import com.video.downloader.downloadvideo.videodownloader.core.utils.SystemUtil
import com.video.downloader.downloadvideo.videodownloader.core.utils.showSafely
import com.video.downloader.downloadvideo.videodownloader.data.service.DownloadForegroundService
import com.video.downloader.downloadvideo.videodownloader.databinding.FragmentBrowserBinding
import com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog.DownloadFormatBottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrowserFragment : BaseFragment<FragmentBrowserBinding>() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentBrowserBinding {
        return FragmentBrowserBinding.inflate(inflater, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun initViews() {
        context?.let { SystemUtil.setLocale(it) }
        val targetUrl = arguments?.getString("target_url") ?: "https://www.google.com"

        binding.webProgressBar.max = 100
        binding.webProgressBar.progress = 0

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            webViewClient = CustomWebViewClient(
                onPageStarted = { url ->
                    val b = bindingOrNull
                    if (b != null) {
                        b.etBrowserUrl.setText(url)
                        b.webProgressBar.visibility = View.VISIBLE
                    }
                    viewModel.sendIntent(BrowserIntent.ClearDetectedMedia(url))
                },
                onPageFinished = {
                    bindingOrNull?.webProgressBar?.visibility = View.GONE
                },
                onYoutubeBlocked = {
                    showYoutubeBlockedDialog()
                },
                onMediaDetected = { quality, title ->
                    viewModel.sendIntent(BrowserIntent.MediaDetected(quality, title))
                }
            )

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    val b = bindingOrNull ?: return
                    b.webProgressBar.progress = newProgress
                    if (newProgress == 100) {
                        b.webProgressBar.visibility = View.GONE
                    }
                }
            }

            loadUrl(targetUrl)
        }

        binding.btnWebBack.setOnClickListener {
            val webView = bindingOrNull?.webView ?: return@setOnClickListener
            if (webView.canGoBack()) webView.goBack()
        }

        binding.btnWebRefresh.setOnClickListener {
            bindingOrNull?.webView?.reload()
        }

        binding.btnClearUrl.setOnClickListener {
            bindingOrNull?.etBrowserUrl?.setText("")
        }

        binding.etBrowserUrl.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                bindingOrNull?.btnClearUrl?.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.etBrowserUrl.setOnEditorActionListener { _, _, _ ->
            val b = bindingOrNull ?: return@setOnEditorActionListener true
            val query = b.etBrowserUrl.text.toString().trim()
            if (query.isNotEmpty()) {
                val target = if (query.startsWith("http://") || query.startsWith("https://")) query
                else if (query.contains(".") && !query.contains(" ")) "https://$query"
                else "https://www.google.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
                b.webView.loadUrl(target)
            }
            true
        }

        binding.layoutSnifferFab.setOnClickListener {
            viewModel.onFabClicked()
        }
    }

    override fun observeData() {
        collectLifecycleFlow(viewModel.uiState) { state ->
            val b = bindingOrNull ?: return@collectLifecycleFlow
            if (state.detectedMediaList.isNotEmpty()) {
                b.layoutSnifferFab.setBackgroundResource(R.drawable.bg_fab_download)
            } else {
                b.layoutSnifferFab.setBackgroundResource(R.drawable.bg_fab_download_disabled)
            }
        }

        collectLifecycleFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is BrowserEffect.ShowYoutubeBlockedDialog -> {
                    showYoutubeBlockedDialog()
                }
                is BrowserEffect.OpenDownloadDialog -> {
                    if (isAdded) {
                        val dialog = DownloadFormatBottomSheetDialog(
                            qualities = effect.qualities,
                            videoTitle = effect.title,
                            onConfirm = { selectedQuality ->
                                viewModel.sendIntent(BrowserIntent.StartDownload(selectedQuality, effect.title))
                            }
                        )
                        dialog.showSafely(childFragmentManager, "DownloadFormatBottomSheet")
                    }
                }
                is BrowserEffect.DownloadStarted -> {
                    val ctx = context ?: return@collectLifecycleFlow
                    DownloadForegroundService.startDownload(
                        context = ctx,
                        downloadId = effect.downloadId,
                        url = effect.url,
                        title = effect.title
                    )
                }
            }
        }
    }

    private fun showYoutubeBlockedDialog() {
        if (!isAdded) return
        val sheet = com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog.YoutubeNotSupportedBottomSheet()
        sheet.showSafely(childFragmentManager, "YoutubeNotSupported")
    }

    override fun onDestroyView() {
        // Zero-Memory-Leak Policy for WebView
        bindingOrNull?.webView?.apply {
            webChromeClient = null
            stopLoading()
            clearHistory()
            loadUrl("about:blank")
            onPause()
            removeAllViews()
            destroy()
        }
        super.onDestroyView()
    }
}
