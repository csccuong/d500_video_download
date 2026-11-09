package com.video.downloader.downloadvideo.videodownloader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val contentUri: String? = null,
    val title: String,
    val fileSize: Long = 0,
    val downloadedBytes: Long = 0,
    val progress: Int = 0,
    val speedText: String = "",
    val status: String = DownloadStatus.PENDING.name,
    val thumbnailPath: String? = null,
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): DownloadItem {
        return DownloadItem(
            id = id,
            url = url,
            contentUri = contentUri,
            title = title,
            fileSize = fileSize,
            downloadedBytes = downloadedBytes,
            progress = progress,
            speedText = speedText,
            status = try { DownloadStatus.valueOf(status) } catch (e: Exception) { DownloadStatus.PENDING },
            thumbnailPath = thumbnailPath,
            durationMs = durationMs,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(domain: DownloadItem): DownloadEntity {
            return DownloadEntity(
                id = domain.id,
                url = domain.url,
                contentUri = domain.contentUri,
                title = domain.title,
                fileSize = domain.fileSize,
                downloadedBytes = domain.downloadedBytes,
                progress = domain.progress,
                speedText = domain.speedText,
                status = domain.status.name,
                thumbnailPath = domain.thumbnailPath,
                durationMs = domain.durationMs,
                createdAt = domain.createdAt
            )
        }
    }
}
