package com.video.downloader.downloadvideo.videodownloader.core.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class DetectedVideo(
    val url: String,
    var title: String,
    var size: String? = null,
    var sizeBytes: Long = 0L,
    var thumbnail: Bitmap? = null,
    var thumbnailUrl: String? = null,
    var streamType: String = "Video Stream"
)

object VideoSniffer {

    private val VIDEO_EXTENSIONS = listOf(
        ".mp4", ".mkv", ".webm", ".mov", ".avi", ".wmv", ".flv", ".f4v",
        ".mpg", ".mpeg", ".m4v", ".3gp", ".3g2", ".ogv", ".ts", ".m2ts", ".m3u8"
    )

    fun isVideoUrl(url: String): Boolean {
        return try {
            val lower = url.lowercase()
            if (lower.contains("audio") || lower.contains(".m4a") || lower.contains(".mp3")) {
                return false
            }
            val uri = Uri.parse(url)
            val path = uri.path?.lowercase().orEmpty()

            if (VIDEO_EXTENSIONS.any { path.endsWith(it) }) {
                return true
            }
            lower.contains("video_mp4") ||
                    lower.contains(".mp4?") ||
                    lower.contains(".m3u8?") ||
                    lower.contains("/video/")
        } catch (_: Exception) {
            false
        }
    }

    fun cleanMediaUrl(rawUrl: String): String {
        return try {
            val uri = Uri.parse(rawUrl)
            if (uri.queryParameterNames.contains("bytestart") ||
                uri.queryParameterNames.contains("byteend") ||
                uri.queryParameterNames.contains("range")
            ) {
                val builder = uri.buildUpon().clearQuery()
                for (name in uri.queryParameterNames) {
                    if (name != "bytestart" && name != "byteend" && name != "range") {
                        builder.appendQueryParameter(name, uri.getQueryParameter(name))
                    }
                }
                builder.build().toString()
            } else {
                rawUrl
            }
        } catch (_: Exception) {
            rawUrl
        }
    }

    fun getStreamDescription(url: String, context: android.content.Context? = null): String {
        val lower = url.lowercase()
        val type = when {
            lower.contains(".m3u8") -> context?.getString(com.video.downloader.downloadvideo.videodownloader.R.string.hls_stream_m3u8) ?: "HLS Stream (.m3u8)"
            lower.contains(".m4s") || lower.contains("bytestart") || lower.contains("chunk") -> context?.getString(com.video.downloader.downloadvideo.videodownloader.R.string.dash_chunk_fragment) ?: "DASH Chunk (Fragment)"
            lower.contains("audio") || lower.contains("acodec") || lower.contains(".mp3") || lower.contains(".m4a") -> context?.getString(com.video.downloader.downloadvideo.videodownloader.R.string.audio_stream) ?: "Audio Stream"
            lower.contains("preview") -> context?.getString(com.video.downloader.downloadvideo.videodownloader.R.string.preview_video_thumbnail) ?: "Preview Video"
            lower.contains(".mp4") -> context?.getString(com.video.downloader.downloadvideo.videodownloader.R.string.mp4_stream) ?: "MP4 Stream"
            else -> context?.getString(com.video.downloader.downloadvideo.videodownloader.R.string.video_stream) ?: "Video Stream"
        }

        val resRegex = Regex("""(\d+x\d+)|_(\d+p)_""")
        val match = resRegex.find(url)
        val res = if (match != null) " | ${match.value.replace("_", "")}" else ""
        val host = try { Uri.parse(url).host ?: "" } catch (_: Exception) { "" }

        return "$type$res ($host)"
    }

    suspend fun resolveVideoSize(okHttpClient: OkHttpClient, url: String, referer: String? = null): Pair<Long, String?> =
        withContext(Dispatchers.IO) {
            try {
                // 1. Try decoding Facebook 'efg' base64 param
                val uri = Uri.parse(url)
                val efg = uri.getQueryParameter("efg")
                if (!efg.isNullOrEmpty()) {
                    try {
                        val decodedBytes = Base64.decode(efg, Base64.DEFAULT)
                        val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
                        if (json.has("duration_s") && json.has("bitrate")) {
                            val durationS = json.optDouble("duration_s", 0.0)
                            val bitrate = json.optDouble("bitrate", 0.0)
                            if (durationS > 0 && bitrate > 0) {
                                val bytes = ((durationS * bitrate) / 8).toLong()
                                if (bytes > 0) {
                                    return@withContext Pair(bytes, formatFileSize(bytes))
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 2. Try HTTP HEAD request
                val cleanUrl = cleanMediaUrl(url)
                val requestBuilder = Request.Builder()
                    .url(cleanUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")

                if (!referer.isNullOrEmpty()) {
                    requestBuilder.header("Referer", referer)
                }

                val headResponse = try {
                    okHttpClient.newCall(requestBuilder.head().build()).execute()
                } catch (_: Exception) {
                    null
                }

                if (headResponse != null && headResponse.isSuccessful) {
                    val lengthStr = headResponse.header("Content-Length")
                    val bytes = lengthStr?.toLongOrNull() ?: 0L
                    headResponse.close()
                    if (bytes > 0) {
                        return@withContext Pair(bytes, formatFileSize(bytes))
                    }
                }
                headResponse?.close()

                // 3. Fallback to Range request: bytes=0-0
                val rangeRequest = requestBuilder
                    .url(url)
                    .header("Range", "bytes=0-0")
                    .get()
                    .build()

                val rangeResponse = try {
                    okHttpClient.newCall(rangeRequest).execute()
                } catch (_: Exception) {
                    null
                }

                if (rangeResponse != null) {
                    val contentRange = rangeResponse.header("Content-Range")
                    val lengthStr = rangeResponse.header("Content-Length")
                    rangeResponse.close()

                    if (contentRange != null && contentRange.contains("/")) {
                        val totalStr = contentRange.substringAfterLast("/")
                        val totalBytes = totalStr.toLongOrNull() ?: 0L
                        if (totalBytes > 0) {
                            return@withContext Pair(totalBytes, formatFileSize(totalBytes))
                        }
                    }
                    val singleByte = lengthStr?.toLongOrNull() ?: 0L
                    if (singleByte > 0) {
                        return@withContext Pair(singleByte, formatFileSize(singleByte))
                    }
                }

                Pair(0L, null)
            } catch (_: Exception) {
                Pair(0L, null)
            }
        }

    private val thumbnailSemaphore = Semaphore(2)

    private fun shouldSkipRemoteThumbnail(url: String): Boolean {
        if (url.isBlank()) return true
        val lower = url.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return true
        return lower.contains(".m3u8") ||
            lower.contains(".mpd") ||
            lower.contains(".ts") ||
            lower.contains(".m2ts") ||
            lower.contains(".m4s") ||
            lower.contains(".mp3") ||
            lower.contains(".m4a") ||
            lower.contains("bytestart=") ||
            lower.contains("chunk")
    }

    suspend fun generateThumbnail(videoUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        if (shouldSkipRemoteThumbnail(videoUrl)) return@withContext null
        if (!thumbnailSemaphore.tryAcquire()) return@withContext null
        val retriever = MediaMetadataRetriever()
        try {
            val headers = HashMap<String, String>().apply {
                put("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36")
            }
            retriever.setDataSource(videoUrl, headers)
            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Throwable) {
            null
        } finally {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    retriever.close()
                } else {
                    retriever.release()
                }
            } catch (_: Throwable) {
            } finally {
                thumbnailSemaphore.release()
            }
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            kb >= 1.0 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
