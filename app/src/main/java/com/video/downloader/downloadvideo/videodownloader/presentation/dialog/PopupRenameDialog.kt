package com.video.downloader.downloadvideo.videodownloader.presentation.dialog

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.video.downloader.downloadvideo.videodownloader.databinding.DialogPopupRenameBinding

class PopupRenameDialog @JvmOverloads constructor(
    private val currentName: String = "",
    private val onSave: ((String) -> Unit)? = null
) : DialogFragment() {

    private var _binding: DialogPopupRenameBinding? = null
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
        _binding = DialogPopupRenameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (onSave == null) {
            dismissAllowingStateLoss()
            return
        }

        binding.etRenameInput.setText(currentName)
        binding.etRenameInput.setSelection(currentName.length)

        binding.btnRenameCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }

        binding.btnRenameSave.setOnClickListener {
            val newName = binding.etRenameInput.text?.toString()?.trim().orEmpty()
            if (newName.isNotEmpty()) {
                dismissAllowingStateLoss()
                onSave.invoke(newName)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
