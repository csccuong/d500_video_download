package com.video.downloader.downloadvideo.videodownloader.presentation.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.databinding.DialogPopupConfirmQuitBinding

class PopupConfirmQuitDialog @JvmOverloads constructor(
    private val onConfirmQuit: (() -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogPopupConfirmQuitBinding? = null
    private val binding get() = _binding!!

    private var countDownTimer: CountDownTimer? = null
    private var isConfirmedEnabled = false

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPopupConfirmQuitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (onConfirmQuit == null) {
            dismissAllowingStateLoss()
            return
        }

        binding.btnQuitClose.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnQuitCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnQuitConfirm.setOnClickListener {
            if (isConfirmedEnabled) {
                dismissAllowingStateLoss()
                onConfirmQuit.invoke()
            }
        }

        startCountDown()
    }

    private fun startCountDown() {
        val baseExitText = getString(R.string.exit_app)
        isConfirmedEnabled = false
        binding.btnQuitConfirm.isEnabled = false
        binding.btnQuitConfirm.alpha = 0.5f

        countDownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val b = _binding ?: return
                val seconds = (millisUntilFinished / 1000) + 1
                b.btnQuitConfirm.text = "$baseExitText ($seconds)"
            }

            override fun onFinish() {
                val b = _binding ?: return
                b.btnQuitConfirm.text = baseExitText
                isConfirmedEnabled = true
                b.btnQuitConfirm.isEnabled = true
                b.btnQuitConfirm.alpha = 1.0f
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
    }
}
