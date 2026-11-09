package com.video.downloader.downloadvideo.videodownloader.presentation.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.databinding.DialogDownloadSuccessBinding

class PopupDownloadSuccessDialog(
    context: Context,
    private val videoTitle: String? = null
) : Dialog(context, R.style.Theme_Transparent_Dialog) {

    private val binding: DialogDownloadSuccessBinding =
        DialogDownloadSuccessBinding.inflate(LayoutInflater.from(context))
    private val handler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable {
        if (isShowing) {
            try {
                dismiss()
            } catch (_: Exception) {}
        }
    }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(binding.root)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // Do not dim background so user can see and interact with their screen
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // Allow touches outside the pill to pass through to the screen
            addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            val density = context.resources.displayMetrics.density
            val params = attributes
            params.y = (88 * density).toInt() // 88dp above bottom (above bottom nav)
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params

            setWindowAnimations(R.style.Animation_FloatingPill)
        }

        if (!videoTitle.isNullOrBlank()) {
            binding.tvSuccessDesc.text = videoTitle
        }

        binding.root.setOnClickListener {
            dismiss()
        }

        binding.btnSuccessClose.setOnClickListener {
            dismiss()
        }

        // Elastic bounce scale on checkmark icon
        val scaleAnim = ScaleAnimation(
            0f, 1f, 0f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 450
            interpolator = OvershootInterpolator(1.8f)
        }
        binding.ivSuccessCheck.startAnimation(scaleAnim)

        // Auto dismiss after 2.8 seconds
        handler.postDelayed(autoDismissRunnable, 2800)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(autoDismissRunnable)
    }
}
