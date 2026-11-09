package com.video.downloader.downloadvideo.videodownloader.presentation.browser

import androidx.lifecycle.viewModelScope
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseViewModel
import com.video.downloader.downloadvideo.videodownloader.core.base.UiEffect
import com.video.downloader.downloadvideo.videodownloader.core.base.UiIntent
import com.video.downloader.downloadvideo.videodownloader.core.base.UiState
import com.video.downloader.downloadvideo.videodownloader.domain.model.VideoQuality
import com.video.downloader.downloadvideo.videodownloader.domain.usecase.StartDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserState(
    val currentUrl: String = "https://www.google.com",
    val pageTitle: String = "",
    val detectedMediaList: List<VideoQuality> = emptyList(),
    val isLoading: Boolean = false,
    val progress: Int = 0
) : UiState

sealed class BrowserIntent : UiIntent {
    data class MediaDetected(val quality: VideoQuality, val title: String) : BrowserIntent()
    data class ClearDetectedMedia(val newUrl: String) : BrowserIntent()
    data class StartDownload(val quality: VideoQuality, val title: String) : BrowserIntent()
}

sealed class BrowserEffect : UiEffect {
    object ShowYoutubeBlockedDialog : BrowserEffect()
    data class OpenDownloadDialog(val qualities: List<VideoQuality>, val title: String) : BrowserEffect()
    data class DownloadStarted(val downloadId: Long, val url: String, val title: String) : BrowserEffect()
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val startDownloadUseCase: StartDownloadUseCase
) : BaseViewModel<BrowserState, BrowserIntent, BrowserEffect>() {

    override fun createInitialState(): BrowserState = BrowserState()

    override fun handleIntent(intent: BrowserIntent) {
        when (intent) {
            is BrowserIntent.MediaDetected -> {
                val exists = currentState.detectedMediaList.any { it.downloadUrl == intent.quality.downloadUrl }
                if (!exists) {
                    val updatedList = currentState.detectedMediaList + intent.quality
                    setState {
                        copy(
                            detectedMediaList = updatedList,
                            pageTitle = intent.title
                        )
                    }
                }
            }
            is BrowserIntent.ClearDetectedMedia -> {
                setState {
                    copy(
                        currentUrl = intent.newUrl,
                        detectedMediaList = emptyList()
                    )
                }
            }
            is BrowserIntent.StartDownload -> {
                viewModelScope.launch {
                    val downloadId = startDownloadUseCase(
                        url = intent.quality.downloadUrl,
                        title = intent.title,
                        fileSize = intent.quality.estimatedSizeBytes
                    )
                    setEffect {
                        BrowserEffect.DownloadStarted(
                            downloadId = downloadId,
                            url = intent.quality.downloadUrl,
                            title = intent.title
                        )
                    }
                }
            }
        }
    }

    fun onFabClicked() {
        if (currentState.detectedMediaList.isNotEmpty()) {
            setEffect {
                BrowserEffect.OpenDownloadDialog(
                    qualities = currentState.detectedMediaList,
                    title = currentState.pageTitle.ifEmpty { "Video_${System.currentTimeMillis()}" }
                )
            }
        }
    }
}
