package com.video.downloader.downloadvideo.videodownloader.presentation.browser

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.video.downloader.downloadvideo.videodownloader.core.utils.YoutubeGuard
import com.video.downloader.downloadvideo.videodownloader.domain.model.VideoQuality

class CustomWebViewClient(
    private val onPageStarted: (String) -> Unit,
    private val onPageFinished: (String) -> Unit,
    private val onYoutubeBlocked: () -> Unit,
    private val onMediaDetected: (VideoQuality, String) -> Unit
) : WebViewClient() {

    private val mediaExtensions = listOf(
        ".mp4", ".mkv", ".webm", ".mov", ".avi", ".m3u8", ".ts", ".mpd"
    )

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString().orEmpty()
        if (YoutubeGuard.isYoutubeUrl(url)) {
            onYoutubeBlocked()
            return true
        }
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onPageStarted(it) }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinished(it) }
    }

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

        val isVideoUrl = mediaExtensions.any { url.contains(it, ignoreCase = true) } &&
                !url.contains("audio", ignoreCase = true) &&
                !url.contains("acodec", ignoreCase = true)
        if (isVideoUrl && !YoutubeGuard.isYoutubeUrl(url)) {
            val quality = VideoQuality(
                resolution = "Auto / Original",
                downloadUrl = url,
                estimatedSizeBytes = 0L,
                format = if (url.contains(".m3u8")) "m3u8" else "mp4"
            )
            val pageTitle = view?.title.orEmpty().ifEmpty { "Downloaded Video" }
            onMediaDetected(quality, pageTitle)
        }

        return super.shouldInterceptRequest(view, request)
    }
}
