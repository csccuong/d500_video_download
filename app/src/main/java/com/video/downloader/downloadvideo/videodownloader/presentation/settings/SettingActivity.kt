package com.video.downloader.downloadvideo.videodownloader.presentation.settings

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.Toast
import com.video.downloader.downloadvideo.videodownloader.BuildConfig
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseActivity
import com.video.downloader.downloadvideo.videodownloader.databinding.ActivitySettingBinding
import com.video.downloader.downloadvideo.videodownloader.presentation.language.LanguageActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingActivity : BaseActivity<ActivitySettingBinding>() {

    override fun inflateBinding(inflater: LayoutInflater): ActivitySettingBinding {
        return ActivitySettingBinding.inflate(inflater)
    }

    override fun initViews() {
        binding.tvAppVersion.text = BuildConfig.VERSION_NAME

        binding.btnSettingBack.setOnClickListener {
            finish()
        }

        binding.btnSettingLanguage.setOnClickListener {
            val intent = Intent(this, com.cscmobi.libraryads.views.language.CSCLanguageActivity::class.java).apply {
                putExtra(com.cscmobi.libraryads.commons.utils.Constants.FROM_SETTING, true)
            }
            startActivity(intent)
        }

        binding.btnSettingRate.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
            }
        }

        binding.btnSettingShare.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_msg, packageName))
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.setting_share_app)))
        }

        binding.btnSettingPolicy.setOnClickListener {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/game-empire-studio"))
                startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.could_not_open_browser), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
