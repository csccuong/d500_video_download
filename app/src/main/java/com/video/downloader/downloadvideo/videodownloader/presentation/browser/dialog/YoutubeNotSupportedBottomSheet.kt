package com.video.downloader.downloadvideo.videodownloader.presentation.browser.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.databinding.DialogYoutubeNotSupportedBinding

class YoutubeNotSupportedBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogYoutubeNotSupportedBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.RoundedBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogYoutubeNotSupportedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)

        binding.btnCloseTips.setOnClickListener {
            dismiss()
        }

        binding.btnGotIt.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            context?.let { ctx ->
                window.navigationBarColor = ContextCompat.getColor(ctx, R.color.app_background_item)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
