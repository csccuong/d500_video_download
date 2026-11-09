package com.video.downloader.downloadvideo.videodownloader.core.utils

import com.video.downloader.downloadvideo.videodownloader.data.model.DownloadModel
import com.video.downloader.downloadvideo.videodownloader.data.model.FormatModel

object FormatHelper {

    /**
     * Ports Flutter's Utils.getVideoQualityLabel
     */
    fun getVideoQualityLabel(width: Double?, height: Double?): String {
        val w = width ?: 0.0
        val h = (height ?: 0.0).toInt()
        if (w <= 0 || h <= 0) {
            return "720p"
        }
        return when {
            h >= 4320 -> "8K"
            h >= 2160 -> "4K"
            h >= 1440 -> "1440p"
            h >= 1080 -> "1080p"
            h >= 720 -> "720p"
            h >= 480 -> "480p"
            h >= 360 -> "360p"
            h >= 240 -> "240p"
            else -> "Low"
        }
    }

    /**
     * Ports Flutter's exact format deduplication & resolution sorting algorithm from download_detail_controller.dart:
     * 1. Groups by quality label ("1080p", "720p", "480p", etc.).
     * 2. Prefers formats that have BOTH video AND audio (itemHasAudio && !existingHasAudio).
     * 3. Breaks ties with larger fileSize or 'mp4' extension.
     * 4. Sorts by (width * height) DESCENDING so highest video resolution with audio is FIRST (index 0).
     * 5. Audio-only formats (width=0, height=0) are sorted to the very bottom.
     */
    fun processAndSortFormats(
        model: DownloadModel,
        defaultTitle: String
    ): List<DetectedVideo> {
        val uniqueFormats = LinkedHashMap<String, FormatModel>()
        val formatsList = model.formats.orEmpty()

        for (item in formatsList) {
            val url = item.url.orEmpty()
            if (url.isEmpty()) continue

            val width = item.width ?: 0.0
            val height = item.height ?: 0.0
            val vcodec = item.vcodec.orEmpty()
            val acodec = item.acodec.orEmpty()
            val formatId = item.formatId.orEmpty()
            val ext = item.ext.orEmpty()

            val label = when {
                width > 0 && height > 0 -> getVideoQualityLabel(width, height)
                vcodec.isEmpty() || vcodec.equals("none", ignoreCase = true) -> "Audio Only"
                formatId.isNotEmpty() -> "Quality: $formatId"
                else -> "Default Quality"
            }

            val existing = uniqueFormats[label]
            if (existing == null) {
                uniqueFormats[label] = item
            } else {
                val existingAcodec = existing.acodec.orEmpty()
                val existingHasAudio = existingAcodec.isNotEmpty() && !existingAcodec.equals("none", ignoreCase = true)
                val itemHasAudio = acodec.isNotEmpty() && !acodec.equals("none", ignoreCase = true)
                if (itemHasAudio && !existingHasAudio) {
                    uniqueFormats[label] = item
                } else if (itemHasAudio == existingHasAudio) {
                    val existingExt = existing.ext.orEmpty()
                    if (item.fileSize > existing.fileSize) {
                        uniqueFormats[label] = item
                    } else if (item.fileSize == existing.fileSize && ext.equals("mp4", ignoreCase = true) && !existingExt.equals("mp4", ignoreCase = true)) {
                        uniqueFormats[label] = item
                    }
                }
            }
        }

        // Sort by resolution descending: Best video first, Audio Only last
        val sortedList = uniqueFormats.values.toMutableList()
        sortedList.sortWith { a, b ->
            val resA = ((a.width ?: 0.0) * (a.height ?: 0.0)).toLong()
            val resB = ((b.width ?: 0.0) * (b.height ?: 0.0)).toLong()
            resB.compareTo(resA)
        }

        return sortedList.map { format ->
            val width = format.width ?: 0.0
            val height = format.height ?: 0.0
            val vcodec = format.vcodec.orEmpty()
            val ext = format.ext.orEmpty()
            val formatId = format.formatId.orEmpty()

            val label = when {
                width > 0 && height > 0 -> "${getVideoQualityLabel(width, height)} ($ext)"
                vcodec.isEmpty() || vcodec.equals("none", ignoreCase = true) -> "Audio Only ($ext)"
                else -> formatId.ifEmpty { "Default Quality" }
            }

            val title = format.title.orEmpty().ifEmpty { model.title.orEmpty().ifEmpty { defaultTitle } }
            DetectedVideo(
                url = format.url.orEmpty(),
                title = title,
                size = if (format.fileSize > 0) VideoSniffer.formatFileSize(format.fileSize) else null,
                sizeBytes = format.fileSize,
                thumbnailUrl = model.thumbnail.orEmpty(),
                streamType = label
            )
        }
    }
}
