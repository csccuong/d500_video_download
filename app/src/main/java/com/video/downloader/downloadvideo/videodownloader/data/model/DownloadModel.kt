package com.video.downloader.downloadvideo.videodownloader.data.model

import com.google.gson.annotations.SerializedName

data class FormatModel(
    @SerializedName("format_id")
    val formatId: String? = null,
    val height: Double? = null,
    val width: Double? = null,
    val ext: String? = null,
    @SerializedName("filesize")
    val rawFileSize: Long? = null,
    @SerializedName("filesize_approx")
    val rawFileSizeApprox: Long? = null,
    val url: String? = null,
    var title: String? = null,
    var thumbnailUrl: String? = null,
    val vcodec: String? = null,
    val acodec: String? = null
) {
    val fileSize: Long
        get() = rawFileSize ?: rawFileSizeApprox ?: 0L
}

data class DownloadModel(
    val title: String? = null,
    val thumbnail: String? = null,
    val duration: Double? = null,
    val source: String? = null,
    val formats: List<FormatModel>? = null
)

data class ApiResponseModel(
    val code: Int = 0,
    val message: String = "",
    val detail: String? = null
)
