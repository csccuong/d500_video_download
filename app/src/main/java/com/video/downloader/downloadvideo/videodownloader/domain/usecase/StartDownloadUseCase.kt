package com.video.downloader.downloadvideo.videodownloader.domain.usecase

import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus
import com.video.downloader.downloadvideo.videodownloader.domain.repository.DownloadRepository
import javax.inject.Inject

class StartDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        url: String,
        title: String,
        fileSize: Long,
        thumbnailUrl: String? = null
    ): Long {
        val downloadItem = DownloadItem(
            url = url,
            title = title,
            fileSize = fileSize,
            thumbnailPath = thumbnailUrl,
            status = DownloadStatus.DOWNLOADING
        )
        return repository.insertDownload(downloadItem)
    }
}
