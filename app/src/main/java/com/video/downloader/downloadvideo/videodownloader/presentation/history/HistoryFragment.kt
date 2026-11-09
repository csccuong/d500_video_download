package com.video.downloader.downloadvideo.videodownloader.presentation.history

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseFragment
import com.video.downloader.downloadvideo.videodownloader.core.utils.MediaStoreHelper
import com.video.downloader.downloadvideo.videodownloader.core.utils.showSafely
import com.video.downloader.downloadvideo.videodownloader.databinding.FragmentHistoryBinding
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus
import com.video.downloader.downloadvideo.videodownloader.presentation.dialog.PopupDeleteDialog
import com.video.downloader.downloadvideo.videodownloader.presentation.dialog.PopupRenameDialog
import com.video.downloader.downloadvideo.videodownloader.core.ads.AdsManager
import com.video.downloader.downloadvideo.videodownloader.core.ads.RemoteConfigs
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class HistoryFragment : BaseFragment<FragmentHistoryBinding>() {

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentHistoryBinding {
        return FragmentHistoryBinding.inflate(inflater, container, false)
    }

    private fun switchToProgressTab() {
        val b = bindingOrNull ?: return
        if (!::adapter.isInitialized) return
        adapter.currentTab = HistoryTab.PROGRESS
        b.tabProgress.setBackgroundResource(R.drawable.bg_segmented_tab_selected)
        b.tabProgress.setTextColor(android.graphics.Color.WHITE)
        b.tabDownloaded.setBackgroundResource(android.R.color.transparent)
        b.tabDownloaded.setTextColor(android.graphics.Color.parseColor("#8E95A5"))
        b.tvEmptyMessage.text = getString(R.string.no_video_in_downloading)
        viewModel.sendIntent(HistoryIntent.ChangeFilter(DownloadStatus.DOWNLOADING))
    }

    private fun switchToDownloadedTab() {
        val b = bindingOrNull ?: return
        if (!::adapter.isInitialized) return
        adapter.currentTab = HistoryTab.DOWNLOADED
        b.tabDownloaded.setBackgroundResource(R.drawable.bg_segmented_tab_selected)
        b.tabDownloaded.setTextColor(android.graphics.Color.WHITE)
        b.tabProgress.setBackgroundResource(android.R.color.transparent)
        b.tabProgress.setTextColor(android.graphics.Color.parseColor("#8E95A5"))
        b.tvEmptyMessage.text = getString(R.string.no_data_available)
        viewModel.sendIntent(HistoryIntent.ChangeFilter(DownloadStatus.COMPLETED))
    }

    override fun initViews() {
        loadNativeDownloadAd()

        adapter = HistoryAdapter(
            onItemClick = { item ->
                if (item.status == DownloadStatus.COMPLETED) {
                    val act = activity ?: return@HistoryAdapter
                    AdsManager.showInterPlay(act) {
                        viewModel.sendIntent(HistoryIntent.PlayItem(item))
                    }
                } else if (item.status == DownloadStatus.DOWNLOADING) {
                    context?.let {
                        Toast.makeText(it, R.string.download_starting, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onMenuClick = { item, anchorView ->
                showItemMenu(item, anchorView)
            }
        )

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        binding.tabProgress.setOnClickListener {
            switchToProgressTab()
        }

        binding.tabDownloaded.setOnClickListener {
            switchToDownloadedTab()
        }

        viewModel.checkInitialTab { hasActive ->
            if (bindingOrNull == null || !isAdded) return@checkInitialTab
            if (hasActive) {
                switchToProgressTab()
            } else {
                switchToDownloadedTab()
            }
        }
    }

    override fun observeData() {
        collectLifecycleFlow(viewModel.uiState) { state ->
            val b = bindingOrNull ?: return@collectLifecycleFlow
            adapter.submitList(state.downloads)
            if (state.downloads.isEmpty()) {
                b.layoutEmptyState.visibility = View.VISIBLE
                b.rvHistory.visibility = View.GONE
            } else {
                b.layoutEmptyState.visibility = View.GONE
                b.rvHistory.visibility = View.VISIBLE
            }
        }

        collectLifecycleFlow(viewModel.uiEffect) { effect ->
            when (effect) {
                is HistoryEffect.OpenWith -> {
                    openMyFile(effect.contentUri)
                }
                is HistoryEffect.ShareItem -> {
                    val ctx = context ?: return@collectLifecycleFlow
                    try {
                        val shareUri = MediaStoreHelper.resolveShareableUri(ctx, effect.contentUri)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/*"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
                    } catch (_: Exception) {
                        Toast.makeText(ctx, R.string.file_not_found, Toast.LENGTH_SHORT).show()
                    }
                }
                is HistoryEffect.ShowToast -> {
                    context?.let {
                        Toast.makeText(it, effect.messageResId, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        collectLifecycleFlow(com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEventBus.events) { event ->
            when (event) {
                is com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEvent.Started -> {
                    switchToProgressTab()
                }
                is com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEvent.Completed -> {
                    context?.let { ctx ->
                        try {
                            com.video.downloader.downloadvideo.videodownloader.presentation.dialog.PopupDownloadSuccessDialog(ctx, event.title).show()
                        } catch (_: Exception) {}
                    }
                    switchToDownloadedTab()
                }
                is com.video.downloader.downloadvideo.videodownloader.core.utils.DownloadEvent.Failed -> {
                    context?.let { ctx ->
                        Toast.makeText(ctx, getString(R.string.download_failed, event.error), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showItemMenu(item: DownloadItem, anchorView: View) {
        val ctx = context ?: return
        val popup = PopupMenu(ctx, anchorView)
        popup.menu.add(0, 1, 0, R.string.open_with).isEnabled = (item.status == DownloadStatus.COMPLETED)
        popup.menu.add(0, 2, 1, R.string.btn_share).isEnabled = (item.status == DownloadStatus.COMPLETED)
        popup.menu.add(0, 3, 2, R.string.rename).isEnabled = (item.status == DownloadStatus.COMPLETED)
        popup.menu.add(0, 4, 3, R.string.btn_delete)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> item.contentUri?.let { uri -> viewModel.sendIntent(HistoryIntent.OpenWithItem(uri)) }
                2 -> item.contentUri?.let { uri -> viewModel.sendIntent(HistoryIntent.ShareItem(uri)) }
                3 -> {
                    if (isAdded) {
                        val dialog = PopupRenameDialog(item.title) { newName ->
                            viewModel.sendIntent(HistoryIntent.RenameItem(item, newName))
                        }
                        dialog.showSafely(childFragmentManager, "RenameDialog")
                    }
                }
                4 -> {
                    if (isAdded) {
                        val dialog = PopupDeleteDialog {
                            viewModel.sendIntent(HistoryIntent.DeleteItem(item.id))
                        }
                        dialog.showSafely(childFragmentManager, "DeleteDialog")
                    }
                }
            }
            true
        }
        popup.show()
    }

    private fun openMyFile(contentUriString: String) {
        val ctx = context ?: return
        try {
            val uri = MediaStoreHelper.resolveShareableUri(ctx, contentUriString)

            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(openIntent)
        } catch (_: Exception) {
            try {
                val uri = MediaStoreHelper.resolveShareableUri(ctx, contentUriString)
                val chooserIntent = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                    getString(R.string.open_with)
                )
                startActivity(chooserIntent)
            } catch (_: Exception) {
                Toast.makeText(
                    ctx,
                    getString(R.string.file_not_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadNativeDownloadAd() {
        val act = activity ?: return
        val b = bindingOrNull ?: return
        AdsManager.loadAndShowNative(
            activity = act,
            container = b.flNativeDownload,
            placementKey = "native_download",
            adUnitId = getString(R.string.native_download),
            isEnabled = RemoteConfigs.native_download,
            layoutRes = R.layout.layout_native_small
        )
    }
}
