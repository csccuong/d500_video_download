package com.video.downloader.downloadvideo.videodownloader.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('DOWNLOADING', 'PENDING', 'PAUSED') ORDER BY createdAt DESC")
    fun getProgressDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('DOWNLOADING', 'PENDING', 'PAUSED')")
    suspend fun getActiveDownloadsCount(): Int

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(entity: DownloadEntity): Long

    @Update
    suspend fun updateDownload(entity: DownloadEntity)

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, progress = :progress, speedText = :speed WHERE id = :id")
    suspend fun updateProgress(id: Long, downloadedBytes: Long, progress: Int, speed: String)

    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: Long)
}
