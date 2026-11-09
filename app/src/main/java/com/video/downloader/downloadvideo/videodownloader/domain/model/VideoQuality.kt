package com.video.downloader.downloadvideo.videodownloader.domain.model

data class VideoQuality(
    val resolution: String,
    val downloadUrl: String,
    val estimatedSizeBytes: Long,
    val format: String = "mp4",
    val isSelected: Boolean = false
)
