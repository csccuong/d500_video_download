package com.video.downloader.downloadvideo.videodownloader.presentation.settings

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.video.downloader.downloadvideo.videodownloader.BuildConfig
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseFragment
import com.video.downloader.downloadvideo.videodownloader.databinding.FragmentSettingBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingFragment : BaseFragment<FragmentSettingBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentSettingBinding {
        return FragmentSettingBinding.inflate(inflater, container, false)
    }

    override fun initViews() {
        binding.tvAppVersion.text = BuildConfig.VERSION_NAME

        binding.btnSettingLanguage.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val intent = Intent(ctx, com.cscmobi.libraryads.views.language.CSCLanguageActivity::class.java).apply {
                putExtra(com.cscmobi.libraryads.commons.utils.Constants.FROM_SETTING, true)
            }
            startActivity(intent)
        }

        binding.btnSettingRate.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val pkg = ctx.packageName
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
            }
        }

        binding.btnSettingShare.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val pkg = ctx.packageName
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_msg, pkg))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.setting_share_app)))
        }

        binding.btnSettingPolicy.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/game-empire-studio"))
                startActivity(browserIntent)
            } catch (_: Exception) {
                Toast.makeText(ctx, getString(R.string.could_not_open_browser), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
