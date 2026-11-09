package com.video.downloader.downloadvideo.videodownloader.presentation.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.utils.FileUtil
import com.video.downloader.downloadvideo.videodownloader.databinding.ItemDownloadHistoryBinding
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadItem
import com.video.downloader.downloadvideo.videodownloader.domain.model.DownloadStatus

enum class HistoryTab {
    PROGRESS,
    DOWNLOADED
}

data class ProgressPayload(
    val progress: Int,
    val speedText: String,
    val status: DownloadStatus
)

class HistoryAdapter(
    private val onItemClick: (DownloadItem) -> Unit,
    private val onMenuClick: (DownloadItem, View) -> Unit
) : ListAdapter<DownloadItem, HistoryAdapter.HistoryViewHolder>(DiffCallback) {

    var currentTab: HistoryTab = HistoryTab.PROGRESS
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemDownloadHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(
        holder: HistoryViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            val payload = payloads.first() as? ProgressPayload
            if (payload != null) {
                holder.bindPartial(payload, getItem(position))
                return
            }
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    inner class HistoryViewHolder(
        private val binding: ItemDownloadHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DownloadItem) {
            binding.tvHistoryTitle.text = item.title

            val density = binding.root.context.resources.displayMetrics.density
            val thumbParams = binding.layoutThumbnailCard.layoutParams
            thumbParams.width = (76 * density).toInt()
            thumbParams.height = (62 * density).toInt()
            binding.layoutThumbnailCard.layoutParams = thumbParams

            val placeholderRes = R.drawable.bg_thumb_placeholder
            if (!item.thumbnailPath.isNullOrEmpty()) {
                Glide.with(binding.ivHistoryThumbnail.context)
                    .load(item.thumbnailPath)
                    .placeholder(placeholderRes)
                    .error(placeholderRes)
                    .into(binding.ivHistoryThumbnail)
            } else {
                binding.ivHistoryThumbnail.setImageResource(placeholderRes)
            }

            val sizeText = FileUtil.formatFileSize(item.fileSize)

            val isDownloading = (currentTab == HistoryTab.PROGRESS) && (item.status != DownloadStatus.COMPLETED)
            if (isDownloading) {
                binding.ivPlayOverlay.visibility = View.GONE
                binding.layoutProgressRow.visibility = View.VISIBLE
                binding.tvHistorySubInfo.visibility = View.GONE
                if (item.progress > 0) {
                    binding.historyProgressBar.isIndeterminate = false
                    binding.historyProgressBar.progress = item.progress
                    binding.tvProgressPercent.text = "${item.progress}%"
                } else {
                    binding.historyProgressBar.isIndeterminate = true
                    binding.tvProgressPercent.text = item.speedText.ifEmpty { "0%" }
                }
                binding.ivCheckIcon.visibility = View.GONE
                binding.btnHistoryEdit.visibility = View.GONE
            } else {
                // Downloaded tab or completed item: hide progress bar & 100%, show file size & play overlay
                binding.ivPlayOverlay.visibility = View.VISIBLE
                binding.layoutProgressRow.visibility = View.GONE
                binding.tvHistorySubInfo.visibility = View.VISIBLE
                binding.tvHistorySubInfo.text = sizeText
                binding.ivCheckIcon.visibility = View.GONE
                binding.btnHistoryEdit.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener { onItemClick(item) }
            binding.btnHistoryEdit.setOnClickListener { onMenuClick(item, it) }
        }

        fun bindPartial(payload: ProgressPayload, item: DownloadItem) {
            if (currentTab == HistoryTab.PROGRESS && payload.status == DownloadStatus.DOWNLOADING) {
                binding.ivPlayOverlay.visibility = View.GONE
                binding.layoutProgressRow.visibility = View.VISIBLE
                binding.tvHistorySubInfo.visibility = View.GONE
                if (payload.progress > 0) {
                    binding.historyProgressBar.isIndeterminate = false
                    binding.historyProgressBar.progress = payload.progress
                    binding.tvProgressPercent.text = "${payload.progress}%"
                } else {
                    binding.historyProgressBar.isIndeterminate = true
                    binding.tvProgressPercent.text = payload.speedText.ifEmpty { "0%" }
                }
                binding.ivCheckIcon.visibility = View.GONE
                binding.btnHistoryEdit.visibility = View.GONE
            } else {
                bind(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DownloadItem>() {
        override fun areItemsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: DownloadItem, newItem: DownloadItem): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: DownloadItem, newItem: DownloadItem): Any? {
            return if (oldItem.progress != newItem.progress || oldItem.speedText != newItem.speedText || oldItem.status != newItem.status) {
                ProgressPayload(newItem.progress, newItem.speedText, newItem.status)
            } else {
                null
            }
        }
    }
}
