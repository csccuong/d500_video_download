package com.video.downloader.downloadvideo.videodownloader.data.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.video.downloader.downloadvideo.videodownloader.core.utils.FileUtil
import com.video.downloader.downloadvideo.videodownloader.core.utils.MediaStoreHelper
import com.video.downloader.downloadvideo.videodownloader.data.engine.DownloadProgressListener
import com.video.downloader.downloadvideo.videodownloader.data.engine.DownloadStrategy
import com.video.downloader.downloadvideo.videodownloader.data.engine.DownloadStrategyFactory
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus
import com.video.downloader.downloadvideo.videodownloader.domain.repository.DownloadRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class DownloadForegroundService : Service() {

    @Inject
    lateinit var strategyFactory: DownloadStrategyFactory

    @Inject
    lateinit var repository: DownloadRepository

    @Inject
    lateinit var mediaStoreHelper: MediaStoreHelper

    @Inject
    lateinit var notificationHelper: DownloadNotificationHelper

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeStrategies = ConcurrentHashMap<Long, DownloadStrategy>()
    private val activeJobs = ConcurrentHashMap<Long, Job>()

    companion object {
        const val ACTION_START = "action_start_download"
        const val ACTION_CANCEL = "action_cancel_download"
        const val EXTRA_DOWNLOAD_ID = "extra_download_id"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_HEADERS = "extra_headers"

        fun startDownload(
            context: Context,
            downloadId: Long,
            url: String,
            title: String,
            headers: HashMap<String, String>? = null
        ) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                if (headers != null) {
                    putExtra(EXTRA_HEADERS, headers)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "video.mp4"
                val headers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_HEADERS, HashMap::class.java) as? HashMap<String, String>
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_HEADERS) as? HashMap<String, String>
                }
                startForegroundDownload(downloadId, url, title, headers)
            }
            ACTION_CANCEL -> {
                val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1L)
                if (downloadId != -1L) {
                    activeStrategies[downloadId]?.cancel()
                    activeJobs[downloadId]?.cancel()
                    activeStrategies.remove(downloadId)
                    activeJobs.remove(downloadId)
                } else {
                    activeStrategies.values.forEach { it.cancel() }
                    activeJobs.values.forEach { it.cancel() }
                    activeStrategies.clear()
                    activeJobs.clear()
                }
                if (activeStrategies.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundDownload(
        downloadId: Long,
        url: String,
        title: String,
        headers: HashMap<String, String>?
    ) {
        val initialNotification = notificationHelper.buildProgressNotification(title, 0, "Starting...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                DownloadNotificationHelper.NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(DownloadNotificationHelper.NOTIFICATION_ID, initialNotification)
        }

        com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEventBus.emitEvent(
            com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEvent.Started(downloadId, title)
        )

        val job = serviceScope.launch {
            val cleanTitle = FileUtil.sanitizeFileName(title)
            val fileName = "$cleanTitle.mp4"
            val destinationUri = mediaStoreHelper.createPendingVideoUri(fileName) ?: run {
                repository.updateStatus(downloadId, DownloadStatus.FAILED)
                com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEventBus.emitEvent(
                    com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEvent.Failed(downloadId, title, "Cannot create media store entry")
                )
                checkAndStopIfIdle(downloadId)
                return@launch
            }

            val strategy = strategyFactory.getStrategy(url)
            activeStrategies[downloadId] = strategy

            strategy.executeDownload(
                sourceUrl = url,
                destinationUri = destinationUri,
                listener = object : DownloadProgressListener {
                    override fun onProgress(
                        bytesDownloaded: Long,
                        totalBytes: Long,
                        progressPercent: Int,
                        speedText: String
                    ) {
                        serviceScope.launch {
                            repository.updateProgress(downloadId, bytesDownloaded, progressPercent, speedText)
                            notificationHelper.notify(
                                DownloadNotificationHelper.NOTIFICATION_ID,
                                notificationHelper.buildProgressNotification(title, progressPercent, speedText)
                            )
                        }
                    }

                    override fun onComplete(destinationUri: Uri) {
                        serviceScope.launch {
                            val thumbPath = mediaStoreHelper.extractAndSaveThumbnail(destinationUri, cleanTitle)
                            val item = repository.getDownloadById(downloadId)
                            if (item != null) {
                                repository.updateDownload(
                                    item.copy(
                                        contentUri = destinationUri.toString(),
                                        status = DownloadStatus.COMPLETED,
                                        progress = 100,
                                        thumbnailPath = thumbPath
                                    )
                                )
                            }
                            notificationHelper.notify(
                                (DownloadNotificationHelper.NOTIFICATION_ID + downloadId).toInt(),
                                notificationHelper.buildCompletedNotification(title)
                            )
                            com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEventBus.emitEvent(
                                com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEvent.Completed(
                                    downloadId,
                                    title,
                                    destinationUri.toString()
                                )
                            )
                            checkAndStopIfIdle(downloadId)
                        }
                    }

                    override fun onError(exception: Throwable) {
                        serviceScope.launch {
                            repository.updateStatus(downloadId, DownloadStatus.FAILED)
                            com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEventBus.emitEvent(
                                com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEvent.Failed(
                                    downloadId,
                                    title,
                                    exception.message.orEmpty()
                                )
                            )
                            checkAndStopIfIdle(downloadId)
                        }
                    }
                },
                headers = headers
            )
        }

        activeJobs[downloadId] = job
    }

    private fun checkAndStopIfIdle(completedDownloadId: Long) {
        activeStrategies.remove(completedDownloadId)
        activeJobs.remove(completedDownloadId)
        if (activeStrategies.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeStrategies.values.forEach { it.cancel() }
        activeJobs.values.forEach { it.cancel() }
        activeStrategies.clear()
        activeJobs.clear()
        serviceScope.cancel()
    }
}
