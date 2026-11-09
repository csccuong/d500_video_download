package com.video.downloader.downloadvideo.videodownloader.presentation.onboard

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.video.downloader.downloadvideo.videodownloader.R
import com.video.downloader.downloadvideo.videodownloader.core.base.BaseActivity
import com.video.downloader.downloadvideo.videodownloader.databinding.ActivityOnboardBinding
import com.video.downloader.downloadvideo.videodownloader.databinding.ItemOnboardSlideBinding
import com.video.downloader.downloadvideo.videodownloader.presentation.question.QuestionActivity
import dagger.hilt.android.AndroidEntryPoint

data class OnboardStepItem(
    val imageRes: Int,
    val title: String,
    val desc: String
)

@AndroidEntryPoint
class OnboardActivity : BaseActivity<ActivityOnboardBinding>() {

    private val steps by lazy {
        listOf(
            OnboardStepItem(
                R.drawable.onboard1,
                getString(R.string.intro_slide_1_title),
                getString(R.string.intro_slide_1_des)
            ),
            OnboardStepItem(
                R.drawable.onboard2,
                getString(R.string.intro_slide_2_title),
                getString(R.string.intro_slide_2_des)
            ),
            OnboardStepItem(
                R.drawable.onboard3,
                getString(R.string.intro_slide_3_title),
                getString(R.string.intro_slide_3_des)
            ),
            OnboardStepItem(
                R.drawable.onboard4,
                getString(R.string.intro_slide_4_title),
                getString(R.string.intro_slide_4_des)
            )
        )
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivityOnboardBinding {
        return ActivityOnboardBinding.inflate(inflater)
    }

    override fun initViews() {
        val adapter = OnboardSlideAdapter(steps)
        binding.vpOnboard.adapter = adapter

        setupDots(0)
        binding.tvBtnOnboard.text = getString(R.string.intro_next)

        binding.vpOnboard.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(position)
                if (position == steps.size - 1) {
                    binding.tvBtnOnboard.text = getString(R.string.get_started)
                } else {
                    binding.tvBtnOnboard.text = getString(R.string.intro_next)
                }
            }
        })

        binding.btnOnboardNext.setOnClickListener {
            val current = binding.vpOnboard.currentItem
            if (current < steps.size - 1) {
                binding.vpOnboard.currentItem = current + 1
            } else {
                startActivity(Intent(this, QuestionActivity::class.java))
                finish()
            }
        }
    }

    private fun setupDots(currentPosition: Int) {
        binding.layoutDots.removeAllViews()
        for (i in steps.indices) {
            val dot = ImageView(this).apply {
                val size = if (i == currentPosition) 20 else 8
                val layoutParams = LinearLayout.LayoutParams(size, 8).apply {
                    setMargins(6, 0, 6, 0)
                }
                this.layoutParams = layoutParams
                setBackgroundResource(if (i == currentPosition) R.drawable.bg_lemon_button else R.drawable.bg_fab_download_disabled)
            }
            binding.layoutDots.addView(dot)
        }
    }

    inner class OnboardSlideAdapter(private val items: List<OnboardStepItem>) :
        RecyclerView.Adapter<OnboardSlideAdapter.SlideViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
            val itemBinding = ItemOnboardSlideBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return SlideViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class SlideViewHolder(private val itemBinding: ItemOnboardSlideBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(item: OnboardStepItem) {
                itemBinding.ivOnboardImage.setImageResource(item.imageRes)
                itemBinding.tvOnboardTitle.text = item.title
                itemBinding.tvOnboardDesc.text = item.desc
            }
        }
    }
}
