package com.video.downloader.downloadvideo.videodownloader.domain.usecase

import com.video.downloader.downloadvideo.videodownloader.data.model.DownloadModel
import com.video.downloader.downloadvideo.videodownloader.data.model.FormatModel
import com.video.downloader.downloadvideo.videodownloader.data.network.UrlRequestBody
import com.video.downloader.downloadvideo.videodownloader.data.network.VideoExtractApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExtractVideoUseCase @Inject constructor(
    private val apiService: VideoExtractApiService
) {
    suspend operator fun invoke(url: String, isTiktok: Boolean = false): Result<DownloadModel> =
        withContext(Dispatchers.IO) {
            try {
                val cleanUrl = url.trim()
                if (cleanUrl.endsWith(".mp4", ignoreCase = true) || cleanUrl.contains(".mp4?", ignoreCase = true) ||
                    cleanUrl.endsWith(".m3u8", ignoreCase = true) || cleanUrl.contains(".m3u8?", ignoreCase = true) ||
                    cleanUrl.endsWith(".mov", ignoreCase = true) || cleanUrl.contains(".mov?", ignoreCase = true)) {
                    val rawName = cleanUrl.substringBefore("?").substringAfterLast("/").ifEmpty { "video" }
                    val nameWithoutExt = rawName.substringBeforeLast(".")
                    val ext = rawName.substringAfterLast(".", "mp4")
                    return@withContext Result.success(
                        DownloadModel(
                            title = nameWithoutExt,
                            formats = listOf(
                                FormatModel(
                                    formatId = "direct",
                                    url = cleanUrl,
                                    ext = ext,
                                    title = nameWithoutExt
                                )
                            )
                        )
                    )
                }

                val body = UrlRequestBody(cleanUrl)
                val response = if (isTiktok) {
                    apiService.extractTiktok(body)
                } else {
                    apiService.extractVideo(body)
                }

                if (response.isSuccessful && response.body() != null) {
                    val downloadModel = response.body()!!
                    if (!downloadModel.formats.isNullOrEmpty()) {
                        Result.success(downloadModel)
                    } else {
                        Result.failure(Exception("No video formats available"))
                    }
                } else {
                    Result.failure(Exception("API returned code ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
