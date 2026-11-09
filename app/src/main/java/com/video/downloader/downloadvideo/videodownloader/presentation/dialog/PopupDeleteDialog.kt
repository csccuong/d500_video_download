package com.video.downloader.downloadvideo.videodownloader.presentation.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.video.downloader.downloadvideo.videodownloader.databinding.DialogPopupDeleteBinding

class PopupDeleteDialog @JvmOverloads constructor(
    private val onConfirmDelete: (() -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogPopupDeleteBinding? = null
    private val binding get() = _binding!!

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
        _binding = DialogPopupDeleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (onConfirmDelete == null) {
            dismissAllowingStateLoss()
            return
        }

        binding.btnDeleteCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnDeleteConfirm.setOnClickListener {
            dismissAllowingStateLoss()
            onConfirmDelete.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
