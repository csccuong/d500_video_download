package com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.databinding.DialogDownloadFormatBinding
import com.video.downloader.downloadvideo.videodownloader.domain.model.VideoQuality

class DownloadFormatBottomSheetDialog @JvmOverloads constructor(
    private val qualities: List<VideoQuality> = emptyList(),
    private val videoTitle: String = "",
    private val onConfirm: ((VideoQuality) -> Unit)? = null
) : BottomSheetDialogFragment() {

    private var _binding: DialogDownloadFormatBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.RoundedBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDownloadFormatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (onConfirm == null || qualities.isEmpty()) {
            dismissAllowingStateLoss()
            return
        }
        binding.tvFormatTitle.text = videoTitle
        binding.tvFormatSource.text = qualities.firstOrNull()?.downloadUrl.orEmpty()

        binding.btnConfirmDownload.setOnClickListener {
            val selected = qualities.firstOrNull() ?: return@setOnClickListener
            onConfirm.invoke(selected)
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
