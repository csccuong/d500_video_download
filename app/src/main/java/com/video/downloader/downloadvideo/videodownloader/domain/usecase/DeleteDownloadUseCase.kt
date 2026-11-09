package com.video.downloader.downloadvideo.videodownloader.domain.usecase

import com.video.downloader.downloadvideo.videodownloader.domain.repository.DownloadRepository
import javax.inject.Inject

class DeleteDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteDownload(id)
    }
}
