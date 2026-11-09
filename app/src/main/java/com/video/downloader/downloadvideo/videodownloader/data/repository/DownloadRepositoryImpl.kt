package com.video.downloader.downloadvideo.videodownloader.data.repository

import com.video.downloader.downloadvideo.videodownloader.data.local.DownloadDao
import com.video.downloader.downloadvideo.videodownloader.data.local.DownloadEntity
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus
import com.video.downloader.downloadvideo.videodownloader.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao
) : DownloadRepository {

    override fun getAllDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getAllDownloads().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadItem>> {
        return downloadDao.getDownloadsByStatus(status.name).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProgressDownloads(): Flow<List<DownloadItem>> {
        return downloadDao.getProgressDownloads().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getActiveDownloadsCount(): Int {
        return downloadDao.getActiveDownloadsCount()
    }

    override suspend fun getDownloadById(id: Long): DownloadItem? {
        return downloadDao.getDownloadById(id)?.toDomain()
    }

    override suspend fun insertDownload(item: DownloadItem): Long {
        return downloadDao.insertDownload(DownloadEntity.fromDomain(item))
    }

    override suspend fun updateDownload(item: DownloadItem) {
        downloadDao.updateDownload(DownloadEntity.fromDomain(item))
    }

    override suspend fun updateProgress(
        id: Long,
        downloadedBytes: Long,
        progress: Int,
        speed: String
    ) {
        downloadDao.updateProgress(id, downloadedBytes, progress, speed)
    }

    override suspend fun updateStatus(id: Long, status: DownloadStatus) {
        downloadDao.updateStatus(id, status.name)
    }

    override suspend fun deleteDownload(id: Long) {
        downloadDao.deleteDownload(id)
    }
}
