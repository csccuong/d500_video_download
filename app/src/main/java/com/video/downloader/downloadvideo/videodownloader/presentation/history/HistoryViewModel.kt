package com.video.downloader.downloadvideo.videodownloader.presentation.history

import androidx.lifecycle.viewModelScope
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseViewModel
import com.video.downloader.downloadvideo.videodownloader.core.base.UiEffect
import com.video.downloader.downloadvideo.videodownloader.core.base.UiIntent
import com.video.downloader.downloadvideo.videodownloader.core.base.UiState
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus
import com.video.downloader.downloadvideo.videodownloader.domain.usecase.DeleteDownloadUseCase
import com.video.downloader.downloadvideo.videodownloader.domain.usecase.GetDownloadsUseCase
import com.video.downloader.downloadvideo.videodownloader.domain.usecase.RenameDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryState(
    val downloads: List<DownloadItem> = emptyList(),
    val selectedFilter: DownloadStatus? = null,
    val isLoading: Boolean = false
) : UiState

sealed class HistoryIntent : UiIntent {
    data class ChangeFilter(val status: DownloadStatus?) : HistoryIntent()
    data class DeleteItem(val id: Long) : HistoryIntent()
    data class RenameItem(val item: DownloadItem, val newName: String) : HistoryIntent()
    data class PlayItem(val item: DownloadItem) : HistoryIntent()
    data class OpenWithItem(val contentUri: String) : HistoryIntent()
    data class ShareItem(val contentUri: String) : HistoryIntent()
}

sealed class HistoryEffect : UiEffect {
    data class OpenWith(val contentUri: String) : HistoryEffect()
    data class ShareItem(val contentUri: String) : HistoryEffect()
    data class ShowToast(val messageResId: Int) : HistoryEffect()
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val renameDownloadUseCase: RenameDownloadUseCase
) : BaseViewModel<HistoryState, HistoryIntent, HistoryEffect>() {

    override fun createInitialState(): HistoryState = HistoryState()

    private var loadJob: Job? = null

    init {
        loadDownloads(DownloadStatus.DOWNLOADING)
    }

    fun checkInitialTab(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val hasActive = getDownloadsUseCase.hasActiveDownloads()
            onResult(hasActive)
        }
    }

    override fun handleIntent(intent: HistoryIntent) {
        when (intent) {
            is HistoryIntent.ChangeFilter -> {
                setState { copy(selectedFilter = intent.status) }
                loadDownloads(intent.status)
            }
            is HistoryIntent.DeleteItem -> {
                viewModelScope.launch {
                    deleteDownloadUseCase(intent.id)
                }
            }
            is HistoryIntent.RenameItem -> {
                viewModelScope.launch {
                    renameDownloadUseCase(intent.item, intent.newName)
                }
            }
            is HistoryIntent.PlayItem -> {
                val uri = intent.item.contentUri
                if (!uri.isNullOrEmpty()) {
                    setEffect { HistoryEffect.OpenWith(uri) }
                } else {
                    setEffect { HistoryEffect.ShowToast(R.string.file_not_found) }
                }
            }
            is HistoryIntent.OpenWithItem -> {
                setEffect { HistoryEffect.OpenWith(intent.contentUri) }
            }
            is HistoryIntent.ShareItem -> {
                setEffect { HistoryEffect.ShareItem(intent.contentUri) }
            }
        }
    }

    private fun loadDownloads(status: DownloadStatus?) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getDownloadsUseCase(status).collectLatest { list ->
                val sorted = if (status == null) {
                    list.sortedWith(
                        compareByDescending<DownloadItem> { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
                            .thenByDescending { it.createdAt }
                    )
                } else {
                    list.sortedByDescending { it.createdAt }
                }
                setState { copy(downloads = sorted) }
            }
        }
    }
}
