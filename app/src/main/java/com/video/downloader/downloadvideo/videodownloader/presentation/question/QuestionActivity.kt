package com.video.downloader.downloadvideo.videodownloader.presentation.question

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseActivity
import com.video.downloader.downloadvideo.videodownloader.core.utils.AppPreferences
import com.video.downloader.downloadvideo.videodownloader.databinding.ActivityQuestionBinding
import com.video.downloader.downloadvideo.videodownloader.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class QuestionActivity : BaseActivity<ActivityQuestionBinding>() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var adapter: QuestionAdapter

    override fun inflateBinding(inflater: LayoutInflater): ActivityQuestionBinding {
        return ActivityQuestionBinding.inflate(inflater)
    }

    override fun initViews() {
        // Block back button (canPop: false 1:1 like Flutter QuestionPage)
        onBackPressedDispatcher.addCallback(this) {
            // Do not allow back to onboard
        }

        val questionList = listOf(
            getString(R.string.watch_videos_offline),
            getString(R.string.save_videos_for_later),
            getString(R.string.download_music_from_videos),
            getString(R.string.share_with_friends),
            getString(R.string.edit_or_trim_videos),
            getString(R.string.fast_simple_downloads)
        )

        adapter = QuestionAdapter(questionList) { hasSelection ->
            updateNextButtonState(hasSelection)
        }

        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = adapter

        updateNextButtonState(false)

        binding.btnQuestionNext.setOnClickListener {
            if (adapter.hasSelection()) {
                appPreferences.isNoFirstOpenApp = true
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }

        loadNativeAd()
    }

    private fun loadNativeAd() {
        try {
            com.video.downloader.downloadvideo.videodownloader.core.ads.AdsManager.loadAndShowNative(
                activity = this,
                container = binding.flNativeAd,
                placementKey = "native_question",
                adUnitId = getString(R.string.native_question),
                isEnabled = com.video.downloader.downloadvideo.videodownloader.core.ads.RemoteConfigs.native_question,
                layoutRes = R.layout.layout_native_media
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateNextButtonState(enabled: Boolean) {
        if (enabled) {
            binding.btnQuestionNext.setBackgroundResource(R.drawable.bg_btn_next_enabled)
            binding.tvQuestionNext.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.ivQuestionNextArrow.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.white)
            )
        } else {
            binding.btnQuestionNext.setBackgroundResource(R.drawable.bg_btn_next_disabled)
            binding.tvQuestionNext.setTextColor(Color.parseColor("#8E8E93"))
            binding.ivQuestionNextArrow.imageTintList = ColorStateList.valueOf(
                Color.parseColor("#8E8E93")
            )
        }
    }
}
