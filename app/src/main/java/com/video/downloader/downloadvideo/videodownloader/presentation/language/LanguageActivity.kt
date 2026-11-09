package com.video.downloader.downloadvideo.videodownloader.presentation.language

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseActivity
import com.video.downloader.downloadvideo.videodownloader.core.utils.AppPreferences
import com.video.downloader.downloadvideo.videodownloader.core.utils.SystemUtil
import com.video.downloader.downloadvideo.videodownloader.databinding.ActivityLanguageBinding
import com.video.downloader.downloadvideo.videodownloader.domain.model.LanguageModel
import com.video.downloader.downloadvideo.videodownloader.presentation.main.MainActivity
import com.video.downloader.downloadvideo.videodownloader.presentation.onboard.OnboardActivity
import com.video.downloader.downloadvideo.videodownloader.presentation.settings.SettingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {

    companion object {
        const val EXTRA_IS_FIRST_LAUNCH = "extra_is_first_launch"
    }

    @Inject
    lateinit var appPreferences: AppPreferences

    private var isFirstLaunch = false
    private lateinit var adapter: LanguageAdapter

    override fun inflateBinding(inflater: LayoutInflater): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(inflater)
    }

    override fun initViews() {
        isFirstLaunch = intent.getBooleanExtra(EXTRA_IS_FIRST_LAUNCH, false)

        if (!isFirstLaunch) {
            binding.btnLanguageBack.visibility = View.VISIBLE
            binding.btnLanguageBack.setOnClickListener { finish() }
        }

        val languageList = listOf(
            LanguageModel("English", "en", R.drawable.flag_english),
            LanguageModel("Tiếng Việt", "vi", R.drawable.flag_vietnam),
            LanguageModel("Español", "es", R.drawable.flag_spain),
            LanguageModel("Português", "pt", R.drawable.flag_portuguese),
            LanguageModel("हिन्दी", "hi", R.drawable.flag_hindi),
            LanguageModel("Français", "fr", R.drawable.flag_france),
            LanguageModel("Deutsch", "de", R.drawable.flag_german),
            LanguageModel("Bahasa Indonesia", "id", R.drawable.flag_indonesia),
            LanguageModel("العربية", "ar", R.drawable.flag_arabic),
            LanguageModel("বাংলা", "bn", R.drawable.flag_bengali),
            LanguageModel("Русский", "ru", R.drawable.flag_russia),
            LanguageModel("日本語", "ja", R.drawable.flag_japan),
            LanguageModel("한국어", "ko", R.drawable.flag_korea),
            LanguageModel("简体中文", "zh", R.drawable.flag_china),
            LanguageModel("Filipino", "fil", R.drawable.flag_philippine),
            LanguageModel("ไทย", "th", R.drawable.flag_thailand),
            LanguageModel("Türkçe", "tr", R.drawable.flag_turkish),
            LanguageModel("Italiano", "it", R.drawable.flag_italy)
        )

        val savedCode = if (!appPreferences.userLanguageCode.isNullOrBlank()) {
            appPreferences.userLanguageCode
        } else {
            SystemUtil.getPreLanguage(this)
        }
        var defaultIndex = languageList.indexOfFirst { it.languageCode.equals(savedCode, ignoreCase = true) }
        if (defaultIndex < 0) defaultIndex = 0

        adapter = LanguageAdapter(
            languages = languageList,
            selectedIndex = defaultIndex,
            onSelect = { _, position ->
                adapter.updateSelectedIndex(position)
            }
        )

        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = adapter

        binding.btnLanguageDone.setOnClickListener {
            val selected = adapter.getSelectedLanguage()
            val previousCode = if (!appPreferences.userLanguageCode.isNullOrBlank()) {
                appPreferences.userLanguageCode
            } else {
                SystemUtil.getPreLanguage(this)
            }
            appPreferences.userLanguageCode = selected.languageCode
            SystemUtil.setPreLanguage(this, selected.languageCode)
            SystemUtil.changeLang(selected.languageCode, this)

            if (isFirstLaunch) {
                startActivity(Intent(this, OnboardActivity::class.java))
                finish()
            } else {
                if (!previousCode.equals(selected.languageCode, ignoreCase = true)) {
                    val mainIntent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(mainIntent)

                    val settingIntent = Intent(this, SettingActivity::class.java)
                    startActivity(settingIntent)
                }
                finish()
            }
        }
    }
}
