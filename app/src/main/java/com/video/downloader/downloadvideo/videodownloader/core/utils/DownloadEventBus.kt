package com.video.downloader.downloadvideo.videodownloader.core.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class DownloadEvent {
    data class Started(val downloadId: Long, val title: String) : DownloadEvent()
    data class Completed(val downloadId: Long, val title: String, val contentUri: String) : DownloadEvent()
    data class Failed(val downloadId: Long, val title: String, val error: String) : DownloadEvent()
}

object DownloadEventBus {
    private val _events = MutableSharedFlow<DownloadEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<DownloadEvent> = _events.asSharedFlow()

    fun emitEvent(event: DownloadEvent) {
        _events.tryEmit(event)
    }
}
