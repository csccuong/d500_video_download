package com.video.downloader.downloadvideo.videodownloader.domain.repository

import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadItem>>
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>>
    fun getProgressDownloads(): Flow<List<DownloadItem>>
    suspend fun getActiveDownloadsCount(): Int
    suspend fun getDownloadById(id: Long): DownloadItem?
    suspend fun insertDownload(item: DownloadItem): Long
    suspend fun updateDownload(item: DownloadItem)
    suspend fun updateProgress(id: Long, downloadedBytes: Long, progress: Int, speed: String)
    suspend fun updateStatus(id: Long, status: DownloadStatus)
    suspend fun deleteDownload(id: Long)
}
