package com.video.downloader.downloadvideo.videodownloader.domain.model

data class DownloadItem(
    val id: Long = 0,
    val url: String,
    val contentUri: String? = null,
    val title: String,
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val progress: Int = 0,
    val speedText: String = "",
    val status: DownloadStatus = DownloadStatus.PENDING,
    val thumbnailPath: String? = null,
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)
