package com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.utils.DetectedVideo
import com.video.downloader.downloadvideo.videodownloader.core.utils.showSafely
import com.video.downloader.downloadvideo.videodownloader.databinding.DialogVideoFormatsBottomSheetBinding
import com.video.downloader.downloadvideo.videodownloader.databinding.ItemVideoFormatSheetBinding
import com.video.downloader.downloadvideo.videodownloader.presentation.dialog.PopupRenameDialog

class VideoFormatsBottomSheet @JvmOverloads constructor(
    private val videos: List<DetectedVideo> = emptyList(),
    private val onDownloadSelected: ((List<DetectedVideo>) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: DialogVideoFormatsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val selectedIndices = mutableSetOf<Int>().apply {
        val firstVideoIndex = videos.indexOfFirst { !it.streamType.startsWith("Audio", ignoreCase = true) }
        if (firstVideoIndex != -1) {
            add(firstVideoIndex)
        } else if (videos.isNotEmpty()) {
            add(0)
        }
    }
    private lateinit var adapter: FormatAdapter

    override fun getTheme(): Int = R.style.RoundedBottomSheetDialogTheme

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                sheet.layoutParams.height = (resources.displayMetrics.heightPixels * 0.75).toInt()
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogVideoFormatsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (onDownloadSelected == null || videos.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }

        adapter = FormatAdapter()
        binding.rvVideoFormats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVideoFormats.adapter = adapter

        binding.btnSheetClose.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnSheetDownload.setOnClickListener {
            val selected = selectedIndices.mapNotNull { idx ->
                if (idx in videos.indices) videos[idx] else null
            }
            if (selected.isNotEmpty()) {
                dismissAllowingStateLoss()
                onDownloadSelected.invoke(selected)
            }
        }
    }

    private inner class FormatAdapter : RecyclerView.Adapter<FormatAdapter.FormatViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormatViewHolder {
            val itemBinding = ItemVideoFormatSheetBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return FormatViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: FormatViewHolder, position: Int) {
            holder.bind(videos[position], position)
        }

        override fun getItemCount(): Int = videos.size

        inner class FormatViewHolder(private val itemBinding: ItemVideoFormatSheetBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(item: DetectedVideo, position: Int) {
                itemBinding.tvFormatTitle.text = item.title
                itemBinding.tvFormatDetail.text = item.streamType
                itemBinding.tvFormatSize.text = item.size ?: ""

                if (item.thumbnail != null) {
                    itemBinding.ivFormatThumb.setImageBitmap(item.thumbnail)
                } else if (!item.thumbnailUrl.isNullOrEmpty()) {
                    Glide.with(itemBinding.root)
                        .load(item.thumbnailUrl)
                        .placeholder(R.drawable.video_thumb)
                        .error(R.drawable.video_thumb)
                        .into(itemBinding.ivFormatThumb)
                } else {
                    itemBinding.ivFormatThumb.setImageResource(R.drawable.video_thumb)
                }

                val isSelected = selectedIndices.contains(position)
                if (isSelected) {
                    itemBinding.ivFormatCheck.setImageResource(R.drawable.ic_checkbox_checked)
                    itemBinding.ivFormatCheck.clearColorFilter()
                } else {
                    itemBinding.ivFormatCheck.setImageResource(R.drawable.ic_checkbox_uncheck)
                    itemBinding.ivFormatCheck.clearColorFilter()
                }

                itemBinding.root.setOnClickListener {
                    if (selectedIndices.contains(position)) {
                        selectedIndices.remove(position)
                    } else {
                        selectedIndices.add(position)
                    }
                    notifyItemChanged(position)
                }

                itemBinding.btnFormatEditName.setOnClickListener {
                    if (isAdded) {
                        val renameDialog = PopupRenameDialog(item.title) { newTitle ->
                            item.title = newTitle
                            notifyItemChanged(position)
                        }
                        renameDialog.showSafely(childFragmentManager, "RenameFormatDialog")
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
