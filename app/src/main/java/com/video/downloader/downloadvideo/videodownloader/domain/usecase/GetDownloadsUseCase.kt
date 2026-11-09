package com.video.downloader.downloadvideo.videodownloader.domain.usecase

import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus
import com.video.downloader.downloadvideo.videodownloader.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(status: DownloadStatus? = null): Flow<List<DownloadItem>> {
        return when (status) {
            null -> repository.getAllDownloads()
            DownloadStatus.COMPLETED -> repository.getDownloadsByStatus(DownloadStatus.COMPLETED)
            DownloadStatus.DOWNLOADING, DownloadStatus.PENDING, DownloadStatus.PAUSED -> repository.getProgressDownloads()
            else -> repository.getDownloadsByStatus(status)
        }
    }

    suspend fun hasActiveDownloads(): Boolean {
        return repository.getActiveDownloadsCount() > 0
    }
}
