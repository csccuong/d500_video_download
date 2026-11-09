package com.video.downloader.downloadvideo.videodownloader.presentation.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import com.video.downloader.downloadvideo.videodownloader.databinding.DialogLoadingBinding

class LoadingDialog(context: Context) : Dialog(context) {

    private val binding: DialogLoadingBinding =
        DialogLoadingBinding.inflate(LayoutInflater.from(context))

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    fun setMessage(message: String) {
        binding.tvLoadingMessage.text = message
    }
}
