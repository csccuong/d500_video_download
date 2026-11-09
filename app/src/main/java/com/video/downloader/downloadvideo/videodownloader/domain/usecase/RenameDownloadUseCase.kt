package com.video.downloader.downloadvideo.videodownloader.domain.usecase

import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.repository.DownloadRepository
import javax.inject.Inject

class RenameDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(item: DownloadItem, newTitle: String) {
        val updated = item.copy(title = newTitle)
        repository.updateDownload(updated)
    }
}
