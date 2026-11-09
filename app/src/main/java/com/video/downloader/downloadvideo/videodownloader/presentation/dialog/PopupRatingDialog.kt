package com.video.downloader.downloadvideo.videodownloader.presentation.dialog

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.video.downloader.downloadvideo.videodownloader.databinding.DialogPopupRatingBinding

class PopupRatingDialog : DialogFragment() {

    private var _binding: DialogPopupRatingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPopupRatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRatingClose.setOnClickListener { dismiss() }
        binding.btnRatingNoThanks.setOnClickListener { dismiss() }
        binding.btnThankYouClose.setOnClickListener { dismiss() }

        val starViews = listOf(
            binding.ivStar1,
            binding.ivStar2,
            binding.ivStar3,
            binding.ivStar4,
            binding.ivStar5
        )

        starViews.forEachIndexed { index, starView ->
            starView.setOnClickListener {
                val rating = index + 1
                handleRatingSelected(rating)
            }
        }
    }

    private fun handleRatingSelected(stars: Int) {
        if (stars <= 3) {
            binding.layoutRatingMain.visibility = View.GONE
            binding.layoutRatingThankYou.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded) dismiss()
            }, 1500)
        } else {
            dismiss()
            openPlayStoreRating()
        }
    }

    private fun openPlayStoreRating() {
        val packageName = requireContext().packageName
        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
