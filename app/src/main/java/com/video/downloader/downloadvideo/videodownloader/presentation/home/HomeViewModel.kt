package com.video.downloader.downloadvideo.videodownloader.presentation.home

import androidx.lifecycle.viewModelScope
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseViewModel
import com.video.downloader.downloadvideo.videodownloader.core.base.UiEffect
import com.video.downloader.downloadvideo.videodownloader.core.base.UiIntent
import com.video.downloader.downloadvideo.videodownloader.core.base.UiState
import com.video.downloader.downloadvideo.videodownloader.core.utils.YoutubeGuard
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.usecase.GetDownloadsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val recentDownloads: List<DownloadItem> = emptyList(),
    val isLoading: Boolean = false
) : UiState

sealed class HomeIntent : UiIntent {
    data class SubmitUrl(val url: String) : HomeIntent()
    object LoadRecentDownloads : HomeIntent()
}

sealed class HomeEffect : UiEffect {
    data class NavigateToBrowser(val url: String) : HomeEffect()
    object ShowYoutubeBlockedDialog : HomeEffect()
    data class ShowToast(val messageResId: Int) : HomeEffect()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase
) : BaseViewModel<HomeState, HomeIntent, HomeEffect>() {

    override fun createInitialState(): HomeState = HomeState()

    init {
        sendIntent(HomeIntent.LoadRecentDownloads)
    }

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SubmitUrl -> processUrl(intent.url)
            is HomeIntent.LoadRecentDownloads -> loadRecents()
        }
    }

    private fun processUrl(rawUrl: String) {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            setEffect { HomeEffect.ShowToast(R.string.please_enter_url) }
            return
        }

        if (YoutubeGuard.isYoutubeUrl(trimmed)) {
            setEffect { HomeEffect.ShowYoutubeBlockedDialog }
            return
        }

        val targetUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else if (trimmed.contains(".") && !trimmed.contains(" ")) {
            "https://$trimmed"
        } else {
            "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
        }

        setEffect { HomeEffect.NavigateToBrowser(targetUrl) }
    }

    private fun loadRecents() {
        viewModelScope.launch {
            getDownloadsUseCase().collectLatest { downloads ->
                setState { copy(recentDownloads = downloads.take(5)) }
            }
        }
    }
}
