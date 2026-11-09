package com.video.downloader.downloadvideo.videodownloader.data.engine

import android.net.Uri

interface DownloadProgressListener {
    fun onProgress(bytesDownloaded: Long, totalBytes: Long, progressPercent: Int, speedText: String)
    fun onComplete(destinationUri: Uri)
    fun onError(exception: Throwable)
}

interface DownloadStrategy {
    suspend fun executeDownload(
        sourceUrl: String,
        destinationUri: Uri,
        listener: DownloadProgressListener,
        headers: Map<String, String>? = null
    )
    fun cancel()
}
