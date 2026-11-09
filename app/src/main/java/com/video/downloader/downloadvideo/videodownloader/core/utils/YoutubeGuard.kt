package com.video.downloader.downloadvideo.videodownloader.core.utils

import java.util.regex.Pattern

object YoutubeGuard {
    private val YOUTUBE_PATTERN = Pattern.compile(
        "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be|m\\.youtube\\.com)/.*",
        Pattern.CASE_INSENSITIVE
    )

    fun isYoutubeUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        return YOUTUBE_PATTERN.matcher(trimmed).matches() ||
                trimmed.contains("youtube.com") ||
                trimmed.contains("youtu.be")
    }
}
