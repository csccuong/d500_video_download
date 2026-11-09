package com.video.downloader.downloadvideo.videodownloader.data.engine

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadStrategyFactory @Inject constructor(
    private val directHttpDownloadStrategy: DirectHttpDownloadStrategy,
    private val hlsStreamDownloadStrategy: HlsStreamDownloadStrategy
) {
    fun getStrategy(url: String): DownloadStrategy {
        return if (url.contains(".m3u8") || url.contains("application/x-mpegURL")) {
            hlsStreamDownloadStrategy
        } else {
            directHttpDownloadStrategy
        }
    }
}
